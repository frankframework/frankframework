/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.batch;

import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Translate a record using an outputFields description. 
 * 
 * 
 * The {@link #setOutputFields(String) outputFields} description can contain the following functions:
 * 
 * <table border="1">
 * <tr><td>string(value)</td><td>inserts the value between the braces</td><td>string( Dit wordt geinsert inclusief spaties ervoor en erna. )</td></tr>
 * <tr><td>align(value,size,align,fillchar)</td><td>inserts the value aligned</td><td>align(test~10~left~ )</td></tr>
 * <tr><td>fill(size,fillchar)</td><td>insert size fillchars</td><td>fill(2,0)</td></tr>
 * <tr><td>now(outformat)</td><td>inserts the current date</td><td>now(dd MMM yyyy)</td></tr>
 * <tr><td>incopy(fieldnr)</td><td>simply inserts the value of the field</td><td>incopy(2)</td></tr>
 * <tr><td>substr(fieldnr,startindex,endindex)</td><td>insert part of the value of the field</td><td>substr(2,0,8)</td></tr>
 * <tr><td>lookup(fieldnr,orgvval=newval,...)</td><td>replace original value using lookup table</td><td>lookup(3,Debit=+,Credit=-)</td></tr>
 * <tr><td>indate(fieldnr,informat,outformat)</td><td>inserts an input datefield using a different format</td><td>indate(2~MMddYY~dd MMM yyyy)</td></tr>
 * <tr><td>inalign(fieldnr,size,align,fillchar)</td><td>inserts an input field</td><td>inalign(3~5~left~0)</td></tr>
 * <tr><td>if(fieldnr,comparator,compareval)</td><td>only output the next fields if condition is true. Comparator is EQ (is equal to), NE (is not equal to), SW (starts with) or NS (not starts with). Use "{..|..|..}" for multiple compareValues</td><td>if(1,eq,3)</td></tr>
 * <tr><td>elseif(fieldnr,comparator,compareval)</td><td>only output the next fields if condition is true. Comparator is EQ, NE, SW or NS</td><td>elseif(1,ne,4)</td></tr>
 * <tr><td>endif()</td><td>endmarker for if</td><td>endif()</td></tr>
 * </table>
 * 
 * @author  John Dekker
 */
public class RecordTransformer extends AbstractRecordHandler {

	private String outputSeparator;

	private List<IOutputField> outputFields=new LinkedList<>();

	
	@Override
	public Object handleRecord(IPipeLineSession session, List<String> parsedRecord) throws Exception {
		StringBuffer output = new StringBuffer();
		Stack<IOutputField> conditions = new Stack<>();
		
		for (Iterator<IOutputField> outputFieldIt = outputFields.iterator(); outputFieldIt.hasNext();) {
			IOutputField outputField = outputFieldIt.next();
			
			// if outputfields are to be seperator with delimiter
			if (outputSeparator != null && output.length() > 0) {
				output.append(outputSeparator); 
			}
			
			// if not in a condition
			if (conditions.isEmpty()) {
				IOutputField condition = outputField.appendValue(outputField, output, parsedRecord);
				if (condition != null) {
					conditions.push(condition);
				}
			}
			// in condition
			else {
				IOutputField condition = conditions.pop();
				IOutputField newCondition = condition.appendValue(outputField, output, parsedRecord);
				if (newCondition != null) {
					conditions.push(condition);
					if (newCondition != condition) {
						conditions.push(newCondition);
					}
				}
			}
		}
		if (output.length() > 0) {
			return output.toString();
		}
		return null;
	}

	/*
	 * the following methods adds and additional output field
	 */
	private void addOutputField(IOutputField field) {
		outputFields.add(field);
	}
	
	public void clearOutputFields() {
		outputFields.clear();
	}
	
	public void addOutputInput(int inputFieldIndex) throws ConfigurationException {
		addOutputField(new OutputInput(inputFieldIndex-1));
	}

	public void addAlignedInput(int inputFieldIndex, int lenght, boolean leftAlign, char fillCharacter) throws ConfigurationException {
		addOutputField(new OutputAlignedInput(inputFieldIndex-1, lenght, leftAlign, fillCharacter));
	}

	public void addFixedOutput(String fixedValue) {
		addOutputField(new FixedOutput(fixedValue));
	}
	
	public void addFillOutput(int length, char fillchar) {
		addOutputField(new FixedFillOutput(length, fillchar));
	}
	
	public void addAlignedOutput(String fixedValue, int lenght, boolean leftAlign, char fillCharacter) {
		addOutputField(new FixedAlignedOutput(fixedValue, lenght, leftAlign, fillCharacter));
	} 
	
	public void addDateOutput(String outformat) throws ConfigurationException {
		addOutputField(new FixedDateOutput(outformat, null, -1));
	}

	public void addDateOutput(int inputFieldIndex, String informat, String outformat) throws ConfigurationException {
		addOutputField(new FixedDateOutput(outformat, informat, inputFieldIndex-1));
	}
	
	public void addLookup(int inputFieldIndex, Map<String,String> lookupValues) throws ConfigurationException {
		addOutputField(new Lookup(inputFieldIndex-1, lookupValues));
	}
	
	public void addSubstring(int inputFieldIndex, int startIndex, int endIndex) throws ConfigurationException {
		addOutputField(new Substring(inputFieldIndex-1, startIndex, endIndex));
	}

	public void addExternal(int inputFieldIndex, String delegateName, String params) throws ConfigurationException {
		addOutputField(new DelegateOutput(inputFieldIndex-1, delegateName, params));
	}

	public void addIf(int inputFieldIndex, String comparator, String compareValue) throws ConfigurationException {
		addOutputField(new IfCondition(inputFieldIndex-1, comparator, compareValue));
	}

	public void addElseIf(int inputFieldIndex, String comparator, String compareValue) throws ConfigurationException {
		addEndIf();
		addIf(inputFieldIndex, comparator, compareValue);
	}

	public void addEndIf() throws ConfigurationException {
		addOutputField(new EndIfCondition());
	}
	
	/**
	 * translates a functiondeclaration to an function instance
	 */
	public void addOutputField(String fieldDef) throws ConfigurationException {
		StringTokenizer st = new StringTokenizer(fieldDef, "(),");
		String def = nextToken(st, "Function in outputFields must be parameterized [" + fieldDef +"]").trim().toUpperCase();
		if ("STRING".equals(def)) {
			 addFixedOutput(nextToken(st, "Fixed function expects a value"));
		}
		else if ("NOW".equals(def)) {
			addDateOutput(nextToken(st, "Now function expects an outformat"));
		}
		else if ("INCOPY".equals(def)) {
			addOutputInput(Integer.parseInt(nextToken(st, "In function expects a numeric value")));
		}
		else if ("INDATE".equals(def)) {
			int field = Integer.parseInt(nextToken(st, "Indate function expects a fieldnummer"));
			addDateOutput(field, nextToken(st, "Indate function expects an in and outformat, seperated with ~"), nextToken(st, "Indate function expects an in and outformat, seperated with ~"));
		}
		else if ("FILL".equals(def)) {
			int length = Integer.parseInt(nextToken(st, "Fill function expects a fieldlength"));
			char fillChar = nextToken(st, "Fill function expects a fillcharacter").charAt(0);
			addFillOutput(length, fillChar);			
		}
		else if ("LOOKUP".equals(def)) {
			int field = Integer.parseInt(nextToken(st, "Lookup function expects a fieldnummer"));
			Map<String,String> keyValues = convertToKeyValueMap(st, '=');
			addLookup(field, keyValues);
		}
		else if ("SUBSTR".equals(def)) {
			int field = Integer.parseInt(nextToken(st, "Substr function expects a fieldnummer"));
			int startIndex = Integer.parseInt(nextToken(st, "Substr function expects a startindex"));
			int endIndex = Integer.parseInt(nextToken(st, "Substr function expects an endindex"));
			addSubstring(field, startIndex, endIndex);
		}
		else if ("ALIGN".equals(def)) {
			String fixedValue = nextToken(st, "Align function expects a fixed value");
			int length = Integer.parseInt(nextToken(st, "Align function expects a fieldlength"));
			boolean leftAlign= "LEFT".equals(nextToken(st, "Align function expects alignment left").toUpperCase());
			char fillChar = nextToken(st, "Align function expects a fillcharacter").charAt(0);
			addAlignedOutput(fixedValue, length, leftAlign, fillChar);
		}
		else if ("INALIGN".equals(def)) {
			int field = Integer.parseInt(nextToken(st, "Inalign function expects a fieldnumber"));
			int length = Integer.parseInt(nextToken(st, "Inalign function expects a fieldlength"));
			boolean leftAlign= "LEFT".equals(nextToken(st, "Inalign function expects alignment left").toUpperCase());
			char fillChar = nextToken(st, "Inalign function expects a fillcharacter").charAt(0);
			addAlignedInput(field, length, leftAlign, fillChar);
		}
		else if ("EXTERNAL".equals(def)) {
			int field = Integer.parseInt(nextToken(st, "External function expects a fieldnumber"));
			String delegateName = nextToken(st, "External function expects a type name for the delegate");
			String params = nextToken(st, "External function expects a parameter string");
			addExternal(field, delegateName, params);
		}
		else if ("IF".equals(def)) {
			int field = Integer.parseInt(nextToken(st, "If function expects a fieldnummer"));
			String comparator = nextToken(st, "If function expects a comparator (EQ | NE | SW | NS)");
			String compareValue = nextToken(st, "If function expects a compareValue");
			addIf(field, comparator, compareValue);
		}
		else if ("ELSEIF".equals(def)) {
			int field = Integer.parseInt(nextToken(st, "If function expects a fieldnummer"));
			String comparator = nextToken(st, "If function expects a comparator (EQ | NE | SW | NS)");
			String compareValue = nextToken(st, "If function expects a compareValue");
			addElseIf(field, comparator, compareValue);
		}
		else if ("ENDIF".equals(def)) {
			addEndIf();
		}
		else {
			throw new ConfigurationException("Unexpected function [" + def + "] defined in outputFields");
		}
	}
	
	private String nextToken(StringTokenizer st, String error) throws ConfigurationException {
		if (st.hasMoreTokens()) {
			return st.nextToken();
		}
		throw new ConfigurationException(error);
	}

	/*
	 * Converts a string to a map 
	 */
	private Map<String,String> convertToKeyValueMap(StringTokenizer st, char kvSep) {
		Map<String,String> result = new HashMap<String,String>();
		while (st.hasMoreTokens()) {
			String kv = st.nextToken();
			int ndx = kv.indexOf(kvSep);
			if (ndx > 0 && ndx < kv.length()) { 
				result.put(kv.substring(0, ndx), kv.substring(ndx+1));
			}
		}
		return result;
	}
	
	/**
	 * Added to allow usage from Configuration file without the need to modify the 
	 * digester-rules 
	 */
	public void registerChild(OutputfieldsPart part) throws ConfigurationException {
		setOutputFields(part.getValue());
	}

	/**
	 * Translate a declaration string with functions to a list of function instances 
	 * @param outputfieldsDef
	 * @throws ConfigurationException
	 */
	@IbisDoc({"semicolon separated list of output record field specifications (see table below)", ""})
	public void setOutputFields(String outputfieldsDef) throws ConfigurationException {
		StringTokenizer st = new StringTokenizer(outputfieldsDef, ";");
		while (st.hasMoreTokens()) {
			addOutputField(st.nextToken().trim());
		}
	}
	

	/**
	 * Each function must implement this interface 
	 * @author John Dekker
	 */
	public interface IOutputField {
		IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) throws Exception;
	}
	
	/**
	 * Copies the value of an input field to the output  
	 * @author John Dekker
	 */
	class OutputInput implements IOutputField {
		private int inputFieldIndex;
		
		OutputInput(int inputFieldIndex) {
			this.inputFieldIndex = inputFieldIndex;
		}

		protected String toValue(List<String> inputFields) throws ConfigurationException {
			if (inputFieldIndex < 0 || inputFieldIndex >= inputFields.size()) {
				throw new ConfigurationException("Function refers to a non-existing inputfield [" + inputFieldIndex + "]");				
			}
			String val = (String)inputFields.get(inputFieldIndex);
			if ((! StringUtils.isEmpty(getOutputSeparator())) && (val != null)) {
				return val.trim();
			}
			return val;
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) throws ConfigurationException {
			result.append(toValue(inputFields));
			return null;
		}

		public int getInputFieldIndex() {
			return inputFieldIndex;
		}

	}
	
	/**
	 * Copies a part of the value of an input field to the output  
	 * @author John Dekker
	 */
	class Substring extends OutputInput {
		private int startIndex;
		private int endIndex;
		
		Substring(int inputFieldIndex, int startIndex, int endIndex) throws ConfigurationException {
			super(inputFieldIndex);
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			
			if (startIndex < 0 || endIndex <= startIndex) {
				throw new ConfigurationException("Incorrect indexes");
			}
		}
		
		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) throws ConfigurationException {
			String val = ((String)super.toValue(inputFields)).trim();
			
			if (startIndex >= val.length()) {
				if (StringUtils.isEmpty(getOutputSeparator())) {
					result.append(FileUtils.getFilledArray(endIndex - startIndex, ' '));
				}
			}
			else if (endIndex >= val.length()) {
				result.append(val.substring(startIndex));
				if (StringUtils.isEmpty(getOutputSeparator())) {
					int fillSize = endIndex - startIndex - val.length();
					if (fillSize > 0) {
						result.append(FileUtils.getFilledArray(fillSize, ' '));
					}
				}
			}
			else {
				result.append(val.substring(startIndex, endIndex));
			}
			return null;
		}
	}
	
	/**
	 * Align the value of an input field and wite it to the output   
	 * @author John Dekker
	 */
	class OutputAlignedInput extends OutputInput {
		private int length;
		private char fillchar;
		private boolean leftAlign;
		
		OutputAlignedInput(int inputFieldIndex, int length, boolean leftAlign, char fill) throws ConfigurationException {
			super(inputFieldIndex);
			this.fillchar = fill;
			this.length = length;
			this.leftAlign = leftAlign;
		}
		
		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) throws ConfigurationException {
			String val = ((String)super.toValue(inputFields)).trim();
			FileUtils.align(result, val, length, leftAlign, fillchar);
			return null;
		}
	}
	
	/**
	 * Sends a fixed value to the output  
	 * @author John Dekker
	 */
	class FixedOutput implements IOutputField {
		private String fixedOutput;
		
		FixedOutput(String fixedOutput) {
			this.fixedOutput = fixedOutput;
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) {
			result.append(fixedOutput);
			return null;
		}
	}
	
	/**
	 * Send x number of characters to the output  
	 * @author John Dekker
	 */
	class FixedFillOutput extends FixedOutput {
		FixedFillOutput(int length, char fillchar) {
			super(new String(FileUtils.getFilledArray(length, fillchar)));
		}
	}
	
	/**
	 * Align a fixed value and send it to the output  
	 * @author John Dekker
	 */
	class FixedAlignedOutput extends FixedOutput {
		FixedAlignedOutput(String fixedOutput, int length, boolean leftAlign, char fillchar) {
			super(FileUtils.align(fixedOutput, length, leftAlign, fillchar));
		}
	}
	
	/**
	 * Use the input value as the key of a lookup map and send the lookup value to the output  
	 * @author John Dekker
	 */
	class Lookup extends OutputInput {
		private Map<String,String> lookupValues;
		
		Lookup(int fieldNr, Map<String,String> lookupValues) {
			super(fieldNr);
			this.lookupValues = lookupValues;
		}
		
		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) throws ConfigurationException {
			String inVal = super.toValue(inputFields);
			String outVal = null;
			if (inVal != null) {
				outVal = lookupValues.get(inVal.trim());
			}
			if (outVal == null) {
				outVal = lookupValues.get("*");
				if (outVal == null) {
					throw new ConfigurationException("Loopupvalue for ["+inVal+"] not found");
				}
			}
			result.append(outVal);
			return null;
		}
	}
	
	/**
	 * Send either a fixed date or a transformed input datevalue to the output  
	 * @author John Dekker
	 */
	class FixedDateOutput implements IOutputField {
		private int inputFieldIndex = -1;
		private SimpleDateFormat outFormatter;
		private SimpleDateFormat inFormatter;
		
		FixedDateOutput(String outFormatPattern, String inFormatPattern, int inputFieldIndex) {
			this.inputFieldIndex = inputFieldIndex;
			if (StringUtils.isEmpty(outFormatPattern)) {
				this.outFormatter = new SimpleDateFormat();
			}
			else {
				this.outFormatter = new SimpleDateFormat(outFormatPattern);
			}
			if (StringUtils.isEmpty(inFormatPattern)) {
				this.inFormatter = new SimpleDateFormat();
			}
			else {
				this.inFormatter = new SimpleDateFormat(inFormatPattern);
			}
		}
		
		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) throws ParseException, ConfigurationException {
			Date date = null;
			
			if (inputFieldIndex < 0) {
				date = new Date();
			}
			else {
				if (inputFieldIndex >= inputFields.size()) {
					throw new ConfigurationException("Function refers to a non-existing inputfield [" + inputFieldIndex + "]");				
				}
				date = inFormatter.parse((String)inputFields.get(inputFieldIndex));
			}
			result.append(outFormatter.format(date));
			return null;
		}		
	}
	
	/**
	 * Abstract class for condition. Only if the condition is met, output is written  
	 * @author John Dekker
	 */
	abstract class Condition implements IOutputField {
		private boolean output;
		 
		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) throws Exception {
			// first call, check wether the condition is true or false 
			if (this == curFunction) {
				output = conditionIsTrue(inputFields);
				return this;	
			}
			
			// check if the condition has to be left
			if (isEndMarker(curFunction)) {
				return null;
			}
			
			if (output) {
				// write the result of the funtion to the output
				IOutputField condition = curFunction.appendValue(curFunction, result, inputFields);
				if (condition != null)
					return condition;
			}
			else {
				// function is a subcondition within this condition 
				if (curFunction instanceof Condition) {
					((Condition)curFunction).output = false;
					return curFunction; 
				}
			}
			return this;
		}
		
		protected abstract boolean conditionIsTrue(List<String> inputFields) throws ConfigurationException;
		protected abstract boolean isEndMarker(IOutputField function);
	}

	/**
	 * If condition  
	 * @author John Dekker
	 */
	class IfCondition extends Condition {
		private int inputFieldIndex;
		private int comparator;
		private String compareValue;
		 
		IfCondition(int inputFieldIndex, String comparator, String compareValue) throws ConfigurationException {
			this.inputFieldIndex = inputFieldIndex;
			
			String comp = comparator.trim().toUpperCase();
			if ("EQ".equals(comp))
				this.comparator = 1;
			else if ("NE".equals(comp))
				this.comparator = 2;
			else if ("SW".equals(comp))
				this.comparator = 3;
			else if ("NS".equals(comp))
				this.comparator = 4;
			else 
				throw new ConfigurationException("If function does not support [" + comparator + "]");				

			this.compareValue = compareValue;			
		}
		 
		@Override
		protected boolean conditionIsTrue(List<String> inputFields) throws ConfigurationException {
			if (inputFieldIndex < 0 && inputFieldIndex >= inputFields.size()) {
				throw new ConfigurationException("Function refers to a non-existing inputfield [" + inputFieldIndex + "]");				
			}
			String val = (String)inputFields.get(inputFieldIndex);

			if (compareValue.startsWith("{") && compareValue.endsWith("}")) { 
				Vector<String> v = new Vector<String>();
				StringTokenizer st = new StringTokenizer(compareValue.substring(1, compareValue.length() - 1),"|");
				while (st.hasMoreTokens()) {
					v.add(st.nextToken());
				}
				switch(comparator) {
					case 1: // eq
						return v.contains(val);
					case 3: // sw
						for (int i = 0; i < v.size(); i++) {
							String  vs = (String)v.elementAt(i);
							if (val.startsWith(vs)) {
								return true;
							}
						}
						return false;
					case 4: // ns
						for (int i = 0; i < v.size(); i++) {
							String  vs = (String)v.elementAt(i);
							if (val.startsWith(vs)) {
								return false;
							}
						}
						return true;
					default: // ne
						return ! v.contains(val);
				}
			} else {
				switch(comparator) {
					case 1: // eq
						return val.equals(compareValue);
					case 3: // sw
						return val.startsWith(compareValue);
					case 4: // ns
						return ! val.startsWith(compareValue);
					default: // ne
						return ! val.equals(compareValue);
				}
			}
		}
		
		@Override
		protected boolean isEndMarker(IOutputField function) {
			return (function instanceof EndIfCondition);
		}
	}
	
	/**
	 * End if marker  
	 * @author John Dekker
	 */
	class EndIfCondition implements IOutputField {
		@Override
		public IOutputField appendValue(IOutputField curFunction,StringBuffer result,List<String> inputFields) throws Exception {
			throw new Exception("Endif function has no corresponding if");
		}
	}
	
	
	/**
	 * Sends a fixed value to the output  
	 * @author John Dekker
	 */
	public interface IOutputDelegate {
		String transform(int fieldNr, List<String> inputFields, String params); 
	}
	

	class DelegateOutput extends OutputInput {
		private IOutputDelegate delegate;
		private String params;
		
		DelegateOutput(int inputFieldIndex, String delegateName, String params) throws ConfigurationException {
			super(inputFieldIndex);
			
			this.params = params;
			try {
				Class delegateClass = Class.forName(delegateName);
				Constructor constructor = delegateClass.getConstructor(new Class[0]);
				delegate = (IOutputDelegate)constructor.newInstance(new Object[0]);
			}
			catch(Exception e) {
				throw new ConfigurationException(e);
			}
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuffer result, List<String> inputFields) {
			String transform = delegate.transform(getInputFieldIndex(), inputFields, params);
			result.append(transform);
			return null;
		}
	}

	@IbisDoc({"optional separator to add between the fields", ""})
	public void setOutputSeparator(String string) {
		outputSeparator = string;
	}
	public String getOutputSeparator() {
		return outputSeparator;
	}

}
