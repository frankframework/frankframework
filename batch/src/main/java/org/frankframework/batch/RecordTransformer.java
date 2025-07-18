/*
   Copyright 2013 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package org.frankframework.batch;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TimeProvider;

/**
 * Translate a record using an outputFields description.
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
 * @deprecated Warning: non-maintained functionality.
 */
public class RecordTransformer extends AbstractRecordHandler {

	private String outputSeparator;

	private final List<IOutputField> outputFields = new ArrayList<>();

	@Override
	public String handleRecord(PipeLineSession session, List<String> parsedRecord) throws Exception {
		StringBuilder output = new StringBuilder();
		Stack<IOutputField> conditions = new Stack<>();

		for (IOutputField outputField : outputFields) {
			// if outputfields are to be seperator with delimiter
			if (outputSeparator != null && !output.isEmpty()) {
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
		if (!output.isEmpty()) {
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

	public void addOutputInput(int inputFieldIndex) {
		addOutputField(new OutputInput(inputFieldIndex-1));
	}

	public void addAlignedInput(int inputFieldIndex, int lenght, boolean leftAlign, char fillCharacter) {
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

	public void addDateOutput(String outformat) {
		addOutputField(new FixedDateOutput(outformat, null, -1));
	}

	public void addDateOutput(int inputFieldIndex, String informat, String outformat) {
		addOutputField(new FixedDateOutput(outformat, informat, inputFieldIndex - 1));
	}

	public void addLookup(int inputFieldIndex, Map<String,String> lookupValues) {
		addOutputField(new Lookup(inputFieldIndex-1, lookupValues));
	}

	public void addSubstring(int inputFieldIndex, int startIndex, int endIndex) throws ConfigurationException {
		addOutputField(new Substring(inputFieldIndex-1, startIndex, endIndex));
	}

	public void addExternal(int inputFieldIndex, String delegateName, String params) throws ConfigurationException {
		addOutputField(new DelegateOutput(inputFieldIndex-1, delegateName, params));
	}

	public void addIf(int inputFieldIndex, String comparator, String compareValue) throws ConfigurationException {
		addOutputField(new IfCondition(inputFieldIndex - 1, comparator, compareValue));
	}

	public void addElseIf(int inputFieldIndex, String comparator, String compareValue) throws ConfigurationException {
		addEndIf();
		addIf(inputFieldIndex, comparator, compareValue);
	}

	public void addEndIf() {
		addOutputField(new EndIfCondition());
	}

	/**
	 * translates a function declaration to a function instance
	 */
	public void addOutputField(String fieldDef) throws ConfigurationException {
		StringTokenizer st = new StringTokenizer(fieldDef, "(),");
		String def = nextToken(st, "Function in outputFields must be parameterized [" + fieldDef +"]").trim().toUpperCase();
		switch (def) {
			case "STRING":
				addFixedOutput(nextToken(st, "Fixed function expects a value"));
				break;
			case "NOW":
				addDateOutput(nextToken(st, "Now function expects an outformat"));
				break;
			case "INCOPY":
				addOutputInput(Integer.parseInt(nextToken(st, "In function expects a numeric value")));
				break;
			case "INDATE": {
				int field = Integer.parseInt(nextToken(st, "Indate function expects a field number"));
				addDateOutput(field, nextToken(st, "Indate function expects an in and outformat, separated with ~"), nextToken(st, "Indate function expects an in and outformat, separated with ~"));
				break;
			}
			case "FILL": {
				int length = Integer.parseInt(nextToken(st, "Fill function expects a field length"));
				char fillChar = nextToken(st, "Fill function expects a fillcharacter").charAt(0);
				addFillOutput(length, fillChar);
				break;
			}
			case "LOOKUP": {
				int field = Integer.parseInt(nextToken(st, "Lookup function expects a field number"));
				Map<String, String> keyValues = convertToKeyValueMap(st, '=');
				addLookup(field, keyValues);
				break;
			}
			case "SUBSTR": {
				int field = Integer.parseInt(nextToken(st, "Substr function expects a field number"));
				int startIndex = Integer.parseInt(nextToken(st, "Substr function expects a startindex"));
				int endIndex = Integer.parseInt(nextToken(st, "Substr function expects an endindex"));
				addSubstring(field, startIndex, endIndex);
				break;
			}
			case "ALIGN": {
				String fixedValue = nextToken(st, "Align function expects a fixed value");
				int length = Integer.parseInt(nextToken(st, "Align function expects a field length"));
				boolean leftAlign = "LEFT".equalsIgnoreCase(nextToken(st, "Align function expects alignment left"));
				char fillChar = nextToken(st, "Align function expects a fillcharacter").charAt(0);
				addAlignedOutput(fixedValue, length, leftAlign, fillChar);
				break;
			}
			case "INALIGN": {
				int field = Integer.parseInt(nextToken(st, "Inalign function expects a field number"));
				int length = Integer.parseInt(nextToken(st, "Inalign function expects a fieldlength"));
				boolean leftAlign = "LEFT".equalsIgnoreCase(nextToken(st, "Inalign function expects alignment left"));
				char fillChar = nextToken(st, "Inalign function expects a fillcharacter").charAt(0);
				addAlignedInput(field, length, leftAlign, fillChar);
				break;
			}
			case "EXTERNAL": {
				int field = Integer.parseInt(nextToken(st, "External function expects a field number"));
				String delegateName = nextToken(st, "External function expects a type name for the delegate");
				String params = nextToken(st, "External function expects a parameter string");
				addExternal(field, delegateName, params);
				break;
			}
			case "IF": {
				int field = Integer.parseInt(nextToken(st, "If function expects a field number"));
				String comparator = nextToken(st, "If function expects a comparator (EQ | NE | SW | NS)");
				String compareValue = nextToken(st, "If function expects a compareValue");
				addIf(field, comparator, compareValue);
				break;
			}
			case "ELSEIF": {
				int field = Integer.parseInt(nextToken(st, "If function expects a field number"));
				String comparator = nextToken(st, "If function expects a comparator (EQ | NE | SW | NS)");
				String compareValue = nextToken(st, "If function expects a compareValue");
				addElseIf(field, comparator, compareValue);
				break;
			}
			case "ENDIF":
				addEndIf();
				break;
			default:
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
		Map<String,String> result = new HashMap<>();
		while (st.hasMoreTokens()) {
			String kv = st.nextToken();
			int ndx = kv.indexOf(kvSep);
			if (ndx > 0) {
				result.put(kv.substring(0, ndx), kv.substring(ndx+1));
			}
		}
		return result;
	}

	/**
	 * Added to allow usage from Configuration file without the need to modify the
	 * digester-rules
	 */
	@Deprecated
	public void addChild(OutputfieldsPart part) throws ConfigurationException {
		addOutputFields(part);
	}

	public void addOutputFields(OutputfieldsPart part) throws ConfigurationException {
		setOutputFields(part.getValue());
	}

	/** semicolon separated list of output record field specifications (see table below) */
	public void setOutputFields(String outputFieldsDef) throws ConfigurationException {
		for (String token : StringUtil.split(outputFieldsDef, ";")) {
			addOutputField(token);
		}
	}

	/**
	 * Each function must implement this interface
	 * @author John Dekker
	 */
	public interface IOutputField {
		IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) throws Exception;
	}

	/**
	 * Copies the value of an input field to the output
	 * @author John Dekker
	 */
	class OutputInput implements IOutputField {
		private final int inputFieldIndex;

		OutputInput(int inputFieldIndex) {
			this.inputFieldIndex = inputFieldIndex;
		}

		protected String toValue(List<String> inputFields) throws ConfigurationException {
			if (inputFieldIndex < 0 || inputFieldIndex >= inputFields.size()) {
				throw new ConfigurationException("Function refers to a non-existing inputfield [" + inputFieldIndex + "]");
			}
			String val = inputFields.get(inputFieldIndex);
			if ((! StringUtils.isEmpty(getOutputSeparator())) && (val != null)) {
				return val.trim();
			}
			return val;
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) throws ConfigurationException {
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
		private final int startIndex;
		private final int endIndex;

		Substring(int inputFieldIndex, int startIndex, int endIndex) throws ConfigurationException {
			super(inputFieldIndex);
			this.startIndex = startIndex;
			this.endIndex = endIndex;

			if (startIndex < 0 || endIndex <= startIndex) {
				throw new ConfigurationException("Incorrect indexes");
			}
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) throws ConfigurationException {
			String val = super.toValue(inputFields).trim();

			if (startIndex >= val.length()) {
				if (StringUtils.isEmpty(getOutputSeparator())) {
					result.append(getFilledArray(endIndex - startIndex, ' '));
				}
			} else if (endIndex >= val.length()) {
				result.append(val.substring(startIndex));
				if (StringUtils.isEmpty(getOutputSeparator())) {
					int fillSize = endIndex - startIndex - val.length();
					if (fillSize > 0) {
						result.append(getFilledArray(fillSize, ' '));
					}
				}
			}
			else {
				result.append(val, startIndex, endIndex);
			}
			return null;
		}
	}

	/**
	 * Align the value of an input field and wite it to the output
	 * @author John Dekker
	 */
	class OutputAlignedInput extends OutputInput {
		private final int length;
		private final char fillChar;
		private final boolean leftAlign;

		OutputAlignedInput(int inputFieldIndex, int length, boolean leftAlign, char fill) {
			super(inputFieldIndex);
			this.fillChar = fill;
			this.length = length;
			this.leftAlign = leftAlign;
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) throws ConfigurationException {
			String val = super.toValue(inputFields).trim();
			align(result, val, length, leftAlign, fillChar);
			return null;
		}
	}

	/**
	 * Sends a fixed value to the output
	 * @author John Dekker
	 */
	static class FixedOutput implements IOutputField {
		private final String fixedOutput;

		FixedOutput(String fixedOutput) {
			this.fixedOutput = fixedOutput;
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) {
			result.append(fixedOutput);
			return null;
		}
	}

	/**
	 * Send x number of characters to the output
	 * @author John Dekker
	 */
	static class FixedFillOutput extends FixedOutput {
		FixedFillOutput(int length, char fillchar) {
			super(new String(getFilledArray(length, fillchar)));
		}
	}

	/**
	 * Align a fixed value and send it to the output
	 * @author John Dekker
	 */
	static class FixedAlignedOutput extends FixedOutput {
		FixedAlignedOutput(String fixedOutput, int length, boolean leftAlign, char fillchar) {
			super(align(fixedOutput, length, leftAlign, fillchar));
		}
	}

	/**
	 * Use the input value as the key of a lookup map and send the lookup value to the output
	 * @author John Dekker
	 */
	class Lookup extends OutputInput {
		private final Map<String,String> lookupValues;

		Lookup(int fieldNr, Map<String,String> lookupValues) {
			super(fieldNr);
			this.lookupValues = lookupValues;
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) throws ConfigurationException {
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
	static class FixedDateOutput implements IOutputField {
		private final int inputFieldIndex;
		private final DateTimeFormatter inputFormatter;
		private final DateTimeFormatter outputFormatter;

		FixedDateOutput(String outFormatPattern, String inFormatPattern, int inputFieldIndex) {
			this.inputFieldIndex = inputFieldIndex;
			this.inputFormatter = getDateTimeFormatter(inFormatPattern);
			this.outputFormatter = getDateTimeFormatter(outFormatPattern);
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) throws ConfigurationException {
			result.append(outputFormatter.format(getInstant(inputFields)));

			return null;
		}

		private Instant getInstant(List<String> inputFields) throws ConfigurationException {
			if (inputFieldIndex < 0) {
				return TimeProvider.now();

			} else {
				if (inputFieldIndex >= inputFields.size()) {
					throw new ConfigurationException("Function refers to a non-existing inputfield [" + inputFieldIndex + "]");
				}
				// check if we have parsed a time
				TemporalAccessor parsed = inputFormatter.parse(inputFields.get(inputFieldIndex));

				return Instant.from(parsed);
			}
		}

		private DateTimeFormatter getDateTimeFormatter(String format) {
			if (StringUtils.isEmpty(format)) {
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
			}

			return DateFormatUtils.getDateTimeFormatterWithOptionalComponents(format);
		}
	}

	/**
	 * Abstract class for condition. Only if the condition is met, output is written
	 * @author John Dekker
	 */
	abstract static class Condition implements IOutputField {
		private boolean output;

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) throws Exception {
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
			} else {
				// function is a subcondition within this condition
				if (curFunction instanceof Condition condition) {
					condition.output = false;
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
	static class IfCondition extends Condition {
		private final int inputFieldIndex;
		private final int comparator;
		private final String compareValue;

		IfCondition(int inputFieldIndex, String comparator, String compareValue) throws ConfigurationException {
			this.inputFieldIndex = inputFieldIndex;

			String comp = comparator.trim().toUpperCase();
			switch (comp) {
				case "EQ":
					this.comparator = 1;
					break;
				case "NE":
					this.comparator = 2;
					break;
				case "SW":
					this.comparator = 3;
					break;
				case "NS":
					this.comparator = 4;
					break;
				default:
					throw new ConfigurationException("If function does not support [" + comparator + "]");
			}

			this.compareValue = compareValue;
		}

		@Override
		protected boolean conditionIsTrue(List<String> inputFields) throws ConfigurationException {
			if (inputFieldIndex < 0 || inputFieldIndex >= inputFields.size()) {
				throw new ConfigurationException("Function refers to a non-existing inputfield [" + inputFieldIndex + "]");
			}
			String val = inputFields.get(inputFieldIndex);

			if (compareValue.startsWith("{") && compareValue.endsWith("}")) {
				String value = compareValue.substring(1, compareValue.length() - 1);
				Stream<String> v = StringUtil.splitToStream(value, "|");
				switch(comparator) {
					case 1: // eq
						return v.anyMatch(val::equals);
					case 3: // sw
						return v.anyMatch(val::startsWith);
					case 4: // ns
						return v.noneMatch(val::startsWith);
					default: // ne
						return v.noneMatch(val::equals);
				}
			}
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

		@Override
		protected boolean isEndMarker(IOutputField function) {
			return function instanceof EndIfCondition;
		}
	}

	/**
	 * End if marker
	 * @author John Dekker
	 */
	static class EndIfCondition implements IOutputField {
		@Override
		public IOutputField appendValue(IOutputField curFunction,StringBuilder result,List<String> inputFields) throws Exception {
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
		private final IOutputDelegate delegate;
		private final String params;

		DelegateOutput(int inputFieldIndex, String delegateName, String params) throws ConfigurationException {
			super(inputFieldIndex);

			this.params = params;
			try {
				Class<?> delegateClass = Class.forName(delegateName);
				Constructor<?> constructor = delegateClass.getConstructor();
				delegate = (IOutputDelegate)constructor.newInstance(new Object[0]);
			}
			catch(Exception e) {
				throw new ConfigurationException(e);
			}
		}

		@Override
		public IOutputField appendValue(IOutputField curFunction, StringBuilder result, List<String> inputFields) {
			String transform = delegate.transform(getInputFieldIndex(), inputFields, params);
			result.append(transform);
			return null;
		}
	}

	/*
	 * methods to create a fixed length string from a value
	 */
	static String align(String val, int length, boolean leftAlign, char padChar) {
		String output = leftAlign ? StringUtils.rightPad(val, length, padChar) : StringUtils.leftPad(val, length, padChar);
		return output.substring(0, length);
	}

	static void align(StringBuilder result, String val, int length, boolean leftAlign, char fillchar) {
		if (val.length() > length) {
			result.append(val, 0, length);
		} else if (val.length() == length) {
			result.append(val);
		} else {
			char[] fill = getFilledArray(length - val.length(), fillchar);
			if (leftAlign) {
				result.append(val).append(fill);
			} else {
				result.append(fill).append(val);
			}
		}
	}

	/**
	 * create a filled array
	 */
	static char[] getFilledArray(int length, char fillChar) {
		char[] fill = new char[length];
		Arrays.fill(fill, fillChar);
		return fill;
	}

	/** optional separator to add between the fields */
	public void setOutputSeparator(String string) {
		outputSeparator = string;
	}
	public String getOutputSeparator() {
		return outputSeparator;
	}

}
