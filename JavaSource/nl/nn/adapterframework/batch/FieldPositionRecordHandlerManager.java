/*
 * $Log: FieldPositionRecordHandlerManager.java,v $
 * Revision 1.6.2.1  2007-10-04 13:07:12  europe\L190409
 * synchronize with HEAD (4.7.0)
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

import nl.nn.adapterframework.core.PipeLineSession;
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
 * <tr><td>{@link #setFieldNr(int) fieldNr}</td><td>Position of field with recordtype (position of first field is 1)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSeparator(String) separator}</td><td>Separator that separates the fields in the record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author John Dekker
 */
public class FieldPositionRecordHandlerManager extends RecordHandlerManager {
	public static final String version = "$RCSfile: FieldPositionRecordHandlerManager.java,v $  $Revision: 1.6.2.1 $ $Date: 2007-10-04 13:07:12 $";

	private int fieldNr;
	private String separator;
	
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
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
		log.warn(ClassUtils.nameOf(this) +"["+getName()+"]: typo has been fixed: please use 'separator' instead of 'seperator'");
		separator = string;
	}
	public void setSeparator(String string) {
		separator = string;
	}
	public String getSeparator() {
		return separator;
	}

}
