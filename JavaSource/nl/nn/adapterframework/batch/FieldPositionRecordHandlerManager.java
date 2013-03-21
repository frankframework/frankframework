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
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Manager that decides the handlers based on the content of a field in the specified 
 * position in a record. The fields in the record are separated by a separator.
 * The value of the specified field is taken as key in the flow-table.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.batch.FieldPositionRecordHandlerManager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the manager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInitial(boolean) initial}</td><td>This manager is the initial manager, i.e. to be used for the first record</td><td>false</td></tr>
 * <tr><td>{@link #setSeparator(String) separator}</td><td>Separator that separates the fields in the record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFieldNr(int) fieldNr}</td><td>Position of field that identifies the recordtype (position of first field is 1)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author John Dekker
 */
public class FieldPositionRecordHandlerManager extends RecordHandlerManager {
	public static final String version = "$RCSfile: FieldPositionRecordHandlerManager.java,v $  $Revision: 1.13 $ $Date: 2012-06-01 10:52:48 $";

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



	public void setFieldNr(int i) {
		fieldNr = i;
	}
	public int getFieldNr() {
		return fieldNr;
	}

	/** 
	 * @deprecated typo has been fixed: please use 'separator' instead of 'seperator'
	 */
	public void setSeperator(String string) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: typo has been fixed: please use 'separator' instead of 'seperator'";
		configWarnings.add(log, msg);

		separator = string;
	}
	public void setSeparator(String string) {
		separator = string;
	}
	public String getSeparator() {
		return separator;
	}

}
