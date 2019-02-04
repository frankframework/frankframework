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

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Manager that decides the handlers based on the content of a field in the specified 
 * position in a record. The fields in the record are separated by a separator.
 * The value of the specified field is taken as key in the flow-table.
 * 
 * 
 * @author John Dekker
 */
public class FieldPositionRecordHandlerManager extends RecordHandlerManager {

	private int fieldNr;
	private String separator;
	
	public RecordHandlingFlow getRecordHandler(IPipeLineSession session, String record) throws Exception {
		int startNdx = -1, endNdx = -1;
		int curField = 0;
		while (curField++ != fieldNr) {
			if (startNdx != -1 && endNdx == -1) {
				throw new Exception("Record contains less fields then the specified fieldnr indicating its type");
			}
			startNdx = endNdx + 1;
			endNdx = record.indexOf(separator, startNdx);
		}
		if (endNdx == -1) {
			return getRecordHandlerByKey(record.substring(startNdx));
		}
		else {
			return getRecordHandlerByKey(record.substring(startNdx, endNdx));
		}
	}



	@IbisDoc({"position of field that identifies the recordtype (position of first field is 1)", ""})
	public void setFieldNr(int i) {
		fieldNr = i;
	}
	public int getFieldNr() {
		return fieldNr;
	}

	/** 
	 * @deprecated typo has been fixed: please use 'separator' instead of 'seperator'
	 * @param string the string that will be separated on
	 */
	public void setSeperator(String string) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: typo has been fixed: please use 'separator' instead of 'seperator'";
		configWarnings.add(log, msg);

		separator = string;
	}

	@IbisDoc({"separator that separates the fields in the record", ""})
	public void setSeparator(String string) {
		separator = string;
	}
	public String getSeparator() {
		return separator;
	}

}
