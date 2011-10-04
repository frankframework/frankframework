/*
 * $Log: FixedPositionRecordHandlerManager.java,v $
 * Revision 1.13  2011-10-04 09:51:51  l190409
 * fixed typo in javadoc
 *
 * Revision 1.12  2011/08/25 12:39:38  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * avoid StringIndexOutOfBoundsException
 *
 * Revision 1.11  2008/06/30 08:54:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow for variable endPosition
 *
 * Revision 1.10  2008/02/19 09:23:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.9  2008/02/15 16:06:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2007/09/24 14:54:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.7  2007/09/12 09:15:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.6  2007/07/26 16:07:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.5  2007/07/24 08:02:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.4  2006/05/19 09:28:37  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:03  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.core.PipeLineSession;

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
	public static final String version = "$RCSfile: FixedPositionRecordHandlerManager.java,v $  $Revision: 1.13 $ $Date: 2011-10-04 09:51:51 $";

	private int startPosition;
	private int endPosition=-1;
	
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
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
