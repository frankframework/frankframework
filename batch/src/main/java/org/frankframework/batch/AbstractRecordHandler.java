/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2021, 2023 WeAreFrank!

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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

/**
 * Abstract class that contains functionality for parsing the field values from a
 * record (line). Fields in the record are either separated with a separator or have
 * a fixed position in the line.
 *
 * @author  John Dekker
 * @deprecated Warning: non-maintained functionality.
 */
public abstract class AbstractRecordHandler implements IRecordHandler, IWithParameters {
	protected Logger log = LogUtil.getLogger(this);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;
	private @Getter String inputSeparator;
	private @Getter boolean trim=false;

	private final List<InputField> inputFields = new ArrayList<>();
	private final List<Integer> recordIdentifyingFields = new ArrayList<>();

	protected @Getter ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
		if (!inputFields.isEmpty() && StringUtils.isNotEmpty(getInputSeparator())) {
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
	public void addChild(InputfieldsPart part) {
		addInputFields(part);
	}

	public void addInputFields(InputfieldsPart part) {
		setInputFields(part.getValue());
	}

	protected int getNumberOfInputFields() {
		return inputFields.size();
	}

	@Override
	public List<String> parse(PipeLineSession session, String record) {
		if (!inputFields.isEmpty()) {
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
		for (InputField field : inputFields) {
			int endPos = curPos + field.length;

			String item;
			if (curPos >= recordLength) {
				item = "";
			} else if (endPos >= recordLength) {
				item = record.substring(curPos);
			} else {
				item = record.substring(curPos, endPos);
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
		StringBuilder result = new StringBuilder();

		for (final Integer recordIdentifyingField : recordIdentifyingFields) {
			int i = recordIdentifyingField;
			String field = record.get(i - 1);
			String fieldValue = field == null ? "" : field;
			if (result.length() > 0) {
				result.append("_");
			}
			result.append(fieldValue);
		}
		return result.toString();
	}

	@Override
	public boolean isNewRecordType(PipeLineSession session, boolean equalRecordHandlers, List<String> prevRecord, List<String> curRecord) {
		if (getRecordIdentifyingFieldList().isEmpty()) {
			if (log.isTraceEnabled()) log.trace("isNewRecordType(): no RecordIdentifyingFields specified, so returning false");
			return false;
		}
		if (! equalRecordHandlers) {
			if (log.isTraceEnabled()) log.trace("isNewRecordType(): equalRecordTypes [{}], so returning true", equalRecordHandlers);
			return true;
		}

		if (prevRecord == null) {
			if (log.isTraceEnabled()) log.trace("isNewRecordType(): no previous record, so returning true");
			return true;
		}
		for (final Integer recordIdentifyingField : recordIdentifyingFields) {
			int i = recordIdentifyingField;
			String prevField = prevRecord.get(i - 1);
			String curField = curRecord.get(i - 1);
			if (!prevField.equals(curField)) {
				if (log.isTraceEnabled())
					log.trace("isNewRecordType(): fields [{}] different previous value [{}] current value [{}], so returning true", i, prevField, curField);
				return true;
			}
		}
		return false;
	}


	protected static class InputField {
		private final int length;

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
		for (String token : StringUtil.split(fieldNrs)) {
			// log.debug("setRecordIdentifyingFields() found identifying field ["+token+"]");
			recordIdentifyingFields.add(Integer.parseInt(token));
		}
		if (recordIdentifyingFields.isEmpty()) {
			log.warn("setRecordIdentifyingFields(): value [{}] did result in an empty list of tokens", fieldNrs);
		}
	}

	@Deprecated
	@ConfigurationWarning("The attribute 'fieldsDifferConditionForPrefix' has been renamed 'recordIdentifyingFields'")
	public void setFieldsDifferConditionForPrefix(String fieldNrs) {
		setRecordIdentifyingFields(fieldNrs);
	}


	@Override
	public void addParameter(IParameter p) {
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
		for (String token : StringUtil.split(fieldLengths)) {
			addInputField(Integer.parseInt(token));
		}
	}

	/** Separator that separates the fields in the input record. If neither this attribute nor <code>inputFields</code> is specified then the entire record is parsed */
	public void setInputSeparator(String string) {
		inputSeparator = string;
	}

	/**
	 * If set to <code>true</code>, trailing spaces are removed from each field
	 * @ff.default false
	 */
	public void setTrim(boolean b) {
		trim = b;
	}

}
