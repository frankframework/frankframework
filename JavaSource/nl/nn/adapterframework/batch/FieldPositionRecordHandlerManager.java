/*
 * $Log: FieldPositionRecordHandlerManager.java,v $
 * Revision 1.1  2005-10-11 13:00:20  europe\m00f531
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Manager that decides the handlers based on the content of a field in the specified 
 * position in a record. The fields in the ercord are seperated by a seperator
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.FieldPositionRecordHandlerManager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFieldNr(int) fieldNr}</td><td>Position of field with recordtype (position of first field is 1)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSeperator(String) seperator}</td><td>Seperator that seperates the fields in the record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author John Dekker
 */
public class FieldPositionRecordHandlerManager extends RecordHandlerManager {
	public static final String version = "$RCSfile: FieldPositionRecordHandlerManager.java,v $  $Revision: 1.1 $ $Date: 2005-10-11 13:00:20 $";

	private int fieldNr;
	private String seperator;
	
	public FieldPositionRecordHandlerManager() {
	}
	
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
		int startNdx = -1, endNdx = -1;
		int curField = 0;
		while (curField++ != fieldNr) {
			if (startNdx != -1 && endNdx == -1) {
				throw new Exception("Record contains less fields then the specified fieldnr indicating its type");
			}
			startNdx = endNdx + 1;
			endNdx = record.indexOf(seperator, startNdx);
		}
		if (endNdx == -1) {
			return getRecordHandlerByKey(record.substring(startNdx));
		}
		else {
			return getRecordHandlerByKey(record.substring(startNdx, endNdx));
		}
	}

	public int getFieldNr() {
		return fieldNr;
	}

	public String getSeperator() {
		return seperator;
	}

	public void setFieldNr(int i) {
		fieldNr = i;
	}

	public void setSeperator(String string) {
		seperator = string;
	}

}
