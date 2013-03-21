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

import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.core.IPipeLineSession;

/**
 * Manager that decides the handlers based on the content of a field in the specified 
 * position in a record. The fields in the record are of a fixed length.
 * The data beween the start position and end position is taken as key in the flow-table.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.batch.FixedPositionRecordHandlerManager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the manager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInitial(boolean) initial}</td><td>This manager is the initial manager, i.e. to be used for the first record</td><td>false</td></tr>
 * <tr><td>{@link #setStartPosition(int) startPosition}</td><td>Startposition of the field in the record that identifies the recordtype (first character is 0)</td><td>0</td></tr>
 * <tr><td>{@link #setEndPosition(int) endPosition}</td><td>if endPosition >= 0 then this field contains the endposition of the recordtype field in the record; All characters beyond this position are ignored. Else, if endposition < 0 then it depends on the length of the recordKey in the flow</td><td>-1</td></tr>
 * </table>
 * </p>
 * 
 * @author John Dekker
 */
public class FixedPositionRecordHandlerManager extends RecordHandlerManager {
	public static final String version = "$RCSfile: FixedPositionRecordHandlerManager.java,v $  $Revision: 1.16 $ $Date: 2012-06-01 10:52:49 $";

	private int startPosition;
	private int endPosition=-1;
	
	public RecordHandlingFlow getRecordHandler(IPipeLineSession session, String record) throws Exception {
		String value = null;
		if (startPosition >= record.length()) {
			throw new Exception("Record size is smaller then the specified position of the recordtype within the record");
		}
		if (endPosition < 0) {
			Map valueHandlersMap = getValueHandlersMap();
			RecordHandlingFlow rhf = null;
			for(Iterator it=valueHandlersMap.keySet().iterator();it.hasNext() && rhf == null;) {
				String name=(String)it.next();
				log.debug("determining value for record ["+record+"] with key ["+name+"] and startPosition ["+startPosition+"]");
				if (name.length()<=record.length()) {
					value = record.substring(startPosition, name.length());
					if (value.equals(name)) {
						rhf = (RecordHandlingFlow)valueHandlersMap.get(name);
					}
				}
			}
			if  (rhf == null) {
				rhf =(RecordHandlingFlow)getValueHandlersMap().get("*");
				if  (rhf == null) {
					throw new Exception("No handlers (flow) found for recordKey [" + value + "]");
				}
			}
			return rhf;
		} else {
			if (endPosition >= record.length()) {
				value = record.substring(startPosition); 
			}
			else {
				value = record.substring(startPosition, endPosition);
			}
			
			return super.getRecordHandlerByKey(value);
		}
	}


	public void setStartPosition(int i) {
		startPosition = i;
	}
	public int getStartPosition() {
		return startPosition;
	}

	public void setEndPosition(int i) {
		endPosition = i;
	}
	public int getEndPosition() {
		return endPosition;
	}
}
