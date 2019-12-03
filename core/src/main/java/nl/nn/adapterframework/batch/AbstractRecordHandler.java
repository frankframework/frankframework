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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Abstract class that contains functionality for parsing the field values from a 
 * record (line). Fields in the record are either separated with a separator or have
 * a fixed position in the line.
 * 
 * @author  John Dekker
 */
public abstract class AbstractRecordHandler implements IRecordHandler, IWithParameters {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputSeparator;
	private boolean trim=false;
	
	private List inputFields=new LinkedList(); 
	private List recordIdentifyingFields=new LinkedList();
	
	protected ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
		if (inputFields.size()>0 && StringUtils.isNotEmpty(getInputSeparator())) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" ["+getName()+"] inputFields and inputSeparator cannot be specified both");
		}
	}
	
	@Override
	public void open() throws SenderException {
		//nothing to do		
	}
	@Override
	public void close() throws SenderException {
		//nothing to do		
	}

	public void addInputField(int length) {
		inputFields.add(new InputField(length));
	}
	
	public void registerChild(InputfieldsPart part) {
		setInputFields(part.getValue());
	}


	protected int getNumberOfInputFields() {
		return inputFields.size();
	}
	
	@Override
	public List parse(IPipeLineSession session, String record) {
		if (inputFields.size() > 0) {
			return parseUsingInputFields(record);
		}
		else if (inputSeparator != null) {
			return parseUsingSeparator(record);
		}
		else {
			List result = new ArrayList();
			result.add(record);
			return result;
		}
	}
	
	private List parseUsingInputFields(String record) {
		List result = new ArrayList();

		int recordLength = record.length(); 
		int curPos = 0;
		for (Iterator fieldIt = inputFields.iterator(); fieldIt.hasNext();) {
			InputField field = (InputField) fieldIt.next();
			int endPos = curPos + field.length; 
			
			String item;
			if (curPos >= recordLength) {
				item="";
			}
			else if (endPos >= recordLength) {
				item=record.substring(curPos);
			}
			else {
				item=record.substring(curPos, endPos);
			}
			if (isTrim()) {
				result.add(item.trim());
			} else {
				result.add(item);
			}
			
			curPos = endPos;
		}
		
		return result;
	}

	private List parseUsingSeparator(String record) {
		List result = new ArrayList();
		
		int endNdx = -1;
		do {
			int startNdx = endNdx + 1;
			endNdx = record.indexOf(inputSeparator, startNdx);
			String item;
			if (endNdx == -1) {
				item=record.substring(startNdx);
			}
			else {
				item=record.substring(startNdx, endNdx);
			}
			if (isTrim()) {
				result.add(item.trim());
			} else {
				result.add(item);
			}
		}
		while(endNdx != -1);
		
		return result;
	}
	
	@Override
	public String getRecordType(List record) {
		String result=null;
		
		for (Iterator it = recordIdentifyingFields.iterator(); it.hasNext();) {
			int i = ((Integer)it.next()).intValue();
			Object field=record.get(i-1);
			String fieldValue=field==null?"":field.toString();
			if (result==null) {
				result=fieldValue;
			} else {
				result+="_"+fieldValue;
			}
		}
		return result;
	}
	
	@Override
	public boolean isNewRecordType(IPipeLineSession session, boolean equalRecordHandlers, List prevRecord, List curRecord) {
		if (getRecordIdentifyingFieldList().size() == 0) {
			log.debug("isNewRecordType(): no RecordIdentifyingFields specified, so returning false");
			return false;
		}
		if (! equalRecordHandlers) {
			log.debug("isNewRecordType(): equalRecordTypes ["+equalRecordHandlers+"], so returning true");
			return true;
		}
			
		if (prevRecord == null) {
			log.debug("isNewRecordType(): no previous record, so returning true");
			return true;
		}
		for (Iterator it = recordIdentifyingFields.iterator(); it.hasNext();) {
			int i = ((Integer)it.next()).intValue();
			Object prevField=prevRecord.get(i-1);
			Object curField=curRecord.get(i-1);
			if (! prevField.equals(curField)) {
				log.debug("isNewRecordType(): fields ["+i+"] different previous value ["+prevField+"] current value ["+curField+"], so returning true");
				return true;
			}
		}
		return false;
	}
	

	protected class InputField {
		private int length;
		
		InputField(int length) {
			this.length = length;
		}
	}

	/*
	 * Returns a List, and therefore cannot be called 'getRecordIdentifyingFields', 
	 * because then setRecordIdentifyingFields is not found as a setter.
	 */  
	public List getRecordIdentifyingFieldList() {
		return recordIdentifyingFields;
	}

	@IbisDoc({"comma separated list of numbers of those fields that are compared with the previous record to determine if a prefix must be written. if any of these fields is not equal in both records, the record types are assumed to be different", ""})
	public void setRecordIdentifyingFields(String fieldNrs) {
		StringTokenizer st = new StringTokenizer(fieldNrs, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			// log.debug("setRecordIdentifyingFields() found identifiying field ["+token+"]");
			recordIdentifyingFields.add(new Integer(token));
		}
		if (recordIdentifyingFields.size()==0) {
			log.warn("setRecordIdentifyingFields(): value ["+fieldNrs+"] did result in an empty list of tokens");
		}
	}
	public void setFieldsDifferConditionForPrefix(String fieldNrs) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the attribute 'fieldsDifferConditionForPrefix' has been renamed 'recordIdentifyingFields' since version 4.7";
		configWarnings.add(log, msg);
		setRecordIdentifyingFields(fieldNrs);
	}


	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@IbisDoc({"name of the recordhandler", ""})
	@Override
	public void setName(String string) {
		name = string;
	}
	@Override
	public String getName() {
		return name;
	}


	@IbisDoc({"comma separated specification of fieldlengths. if neither this attribute nor <code>inputseparator</code> is specified then the entire record is parsed", ""})
	public void setInputFields(String fieldLengths) {
		StringTokenizer st = new StringTokenizer(fieldLengths, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			addInputField(Integer.parseInt(token));
		}
	}


	/**
	 * @deprecated typo has been fixed: please use 'inputSeparator' instead of 'inputSeperator'
	 */
	@Deprecated
	public void setInputSeperator(String string) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: typo has been fixed: please use 'inputSeparator' instead of 'inputSeperator'";
		configWarnings.add(log, msg);
		setInputSeparator(string);
	}

	@IbisDoc({"separator that separated the fields in the input record. if neither this attribute nor <code>inputfields</code> is specified then the entire record is parsed", ""})
	public void setInputSeparator(String string) {
		inputSeparator = string;
	}
	public String getInputSeparator() {
		return inputSeparator;
	}

	@IbisDoc({"when set <code>true</code>, trailing spaces are removed from each field", "false"})
	public void setTrim(boolean b) {
		trim = b;
	}
	public boolean isTrim() {
		return trim;
	}

}
