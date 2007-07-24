/*
 * $Log: FixedPositionRecordHandlerManager.java,v $
 * Revision 1.5  2007-07-24 08:02:02  europe\L190409
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

import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Manager that decides the handlers based on the content of a field in the specified 
 * position in a record. The fields in the record are of a fixed length.
 * The data beween the start position and end position is taken as key in the flow-table.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.FieldPositionRecordHandlerManager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStartPosition(int) startPosition}</td><td>Startposition of the recordtype field in the record (first character is 0)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setEndPosition(int) endPosition}</td><td>Endposition of the recordtype field in the record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author John Dekker
 */
public class FixedPositionRecordHandlerManager extends RecordHandlerManager {
	public static final String version = "$RCSfile: FixedPositionRecordHandlerManager.java,v $  $Revision: 1.5 $ $Date: 2007-07-24 08:02:02 $";

	private int startPosition;
	private int endPosition;
	
	public FixedPositionRecordHandlerManager() {
	}
	
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
		String value = null;
		if (startPosition >= record.length()) {
			throw new Exception("Record size is smaller then the specified position of the recordtype within the record");
		}
		if (endPosition >= record.length()) {
			value = record.substring(startPosition); 
		}
		else {
			value = record.substring(startPosition, endPosition);
		}
		
		return super.getRecordHandlerByKey(value);
	}

	public int getEndPosition() {
		return endPosition;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public void setEndPosition(int i) {
		endPosition = i;
	}

	public void setStartPosition(int i) {
		startPosition = i;
	}

}
