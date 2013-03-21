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
/*
 * $Log: FieldPositionRecordHandlerManager.java,v $
 * Revision 1.13  2012-06-01 10:52:48  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.12  2011/11/30 13:51:56  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.10  2008/12/30 17:01:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.9  2008/02/19 09:23:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2008/02/15 16:06:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.7  2007/09/24 14:54:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.6  2007/07/26 16:07:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed seperator into separator
 *
 * Revision 1.5  2007/07/24 08:01:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * change seperator to separator
 *
 * Revision 1.4  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:20  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
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
