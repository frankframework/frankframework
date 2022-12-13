/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
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
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;
	private @Getter String inputSeparator;
	private @Getter boolean trim=false;

	private List<InputField> inputFields=new LinkedList<>();
	private List<Integer> recordIdentifyingFields=new LinkedList<>();

	protected @Getter ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
		if (inputFields.size()>0 && StringUtils.isNotEmpty(getInputSeparator())) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" inputFields and inputSeparator cannot be specified both");
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

	@Deprecated
	public void registerChild(InputfieldsPart part) {
		registerInputFields(part);
	}

	public void registerInputFields(InputfieldsPart part) {
		setInputFields(part.getValue());
	}

	protected int getNumberOfInputFields() {
		return inputFields.size();
	}

	@Override
	public List<String> parse(PipeLineSession session, String record) {
		if (inputFields.size() > 0) {
			return parseUsingInputFields(record);
		}
		else if (inputSeparator != null) {
			return parseUsingSeparator(record);
		}
		else {
			List<String> result = new ArrayList<>();
			result.add(record);
			return result;
		}
	}

	private List<String> parseUsingInputFields(String record) {
		List<String> result = new ArrayList<>();

		int recordLength = record.length();
		int curPos = 0;
		for (Iterator<InputField> fieldIt = inputFields.iterator(); fieldIt.hasNext();) {
			InputField field = fieldIt.next();
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

	private List<String> parseUsingSeparator(String record) {
		List<String> result = new ArrayList<>();

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
	public String getRecordType(List<String> record) {
		String result=null;

		for (Iterator<Integer> it = recordIdentifyingFields.iterator(); it.hasNext();) {
			int i = (it.next()).intValue();
			String field=record.get(i-1);
			String fieldValue=field==null?"":field;
			if (result==null) {
				result=fieldValue;
			} else {
				result+="_"+fieldValue;
			}
		}
		return result;
	}

	@Override
	public boolean isNewRecordType(PipeLineSession session, boolean equalRecordHandlers, List<String> prevRecord, List<String> curRecord) {
		if (getRecordIdentifyingFieldList().size() == 0) {
			if (log.isTraceEnabled()) log.trace("isNewRecordType(): no RecordIdentifyingFields specified, so returning false");
			return false;
		}
		if (! equalRecordHandlers) {
			if (log.isTraceEnabled()) log.trace("isNewRecordType(): equalRecordTypes ["+equalRecordHandlers+"], so returning true");
			return true;
		}

		if (prevRecord == null) {
			if (log.isTraceEnabled()) log.trace("isNewRecordType(): no previous record, so returning true");
			return true;
		}
		for (Iterator<Integer> it = recordIdentifyingFields.iterator(); it.hasNext();) {
			int i = (it.next()).intValue();
			String prevField=prevRecord.get(i-1);
			String curField=curRecord.get(i-1);
			if (! prevField.equals(curField)) {
				if (log.isTraceEnabled()) log.trace("isNewRecordType(): fields ["+i+"] different previous value ["+prevField+"] current value ["+curField+"], so returning true");
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
	public List<Integer> getRecordIdentifyingFieldList() {
		return recordIdentifyingFields;
	}

	/** comma separated list of numbers of those fields that are compared with the previous record to determine if a prefix must be written. if any of these fields is not equal in both records, the record types are assumed to be different */
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

	@Deprecated
	@ConfigurationWarning("The attribute 'fieldsDifferConditionForPrefix' has been renamed 'recordIdentifyingFields'")
	public void setFieldsDifferConditionForPrefix(String fieldNrs) {
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

	/** Name of the recordhandler */
	@Override
	public void setName(String string) {
		name = string;
	}

	/** Comma separated specification of field lengths. if neither this attribute nor <code>inputSeparator</code> is specified then the entire record is parsed */
	public void setInputFields(String fieldLengths) {
		StringTokenizer st = new StringTokenizer(fieldLengths, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			addInputField(Integer.parseInt(token));
		}
	}

	/** Separator that separates the fields in the input record. If neither this attribute nor <code>inputFields</code> is specified then the entire record is parsed */
	public void setInputSeparator(String string) {
		inputSeparator = string;
	}

	/**
	 * If set <code>true</code>, trailing spaces are removed from each field
	 * @ff.default false
	 */
	public void setTrim(boolean b) {
		trim = b;
	}

}
