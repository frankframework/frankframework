/*
 * $Log: RecordHandlingFlow.java,v $
 * Revision 1.4  2006-05-19 09:28:36  europe\m00i745
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

/**
 * The flow contains the handlers to handle records of a specific type.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.RecordHandlingFlow</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordHandlerManagerRef(String) recordHandlerManagerRef}</td><td>Name of the manager to which this RecordHandlingFlow must be added</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordKey(String) recordKey}</td><td>Key under which this RecordHandlingFlow must be registered in the manager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordHandlerRef(String) recordHandlerRef}</td><td>Name of the recordhandler to be used to transform records of the type specified in the key (optional)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNextRecordHandlerManagerRef(String) nextRecordHandlerManagerRef}</td><td>Name of the manager to be used after handling this record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResultHandlerRef(String) resultHandlerRef}</td><td>Name of the resulthandler to be used to handle the transformed result</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author John Dekker
 */
public final class RecordHandlingFlow {
	public static final String version = "$RCSfile: RecordHandlingFlow.java,v $  $Revision: 1.4 $ $Date: 2006-05-19 09:28:36 $";

	private String recordKey;
	private String recordHandlerRef;
	private String recordHandlerManagerRef;
	private String nextRecordHandlerManagerRef;
	private String resultHandlerRef;
	
	private IRecordHandler recordHandler;
	private IRecordHandlerManager nextRecordHandlerManager;
	private IResultHandler resultHandler;
	
	public RecordHandlingFlow() {
	}
	
	public IRecordHandlerManager getNextRecordHandlerManager() {
		return nextRecordHandlerManager;
	}

	public IRecordHandler getRecordHandler() {
		return recordHandler;
	}

	public void setNextRecordHandlerManager(IRecordHandlerManager manager) {
		nextRecordHandlerManager = manager;
	}

	public void setRecordHandler(IRecordHandler handler) {
		recordHandler = handler;
	}

	public String getRecordKey() {
		return recordKey;
	}

	public void setRecordKey(String recordKey) {
		this.recordKey = recordKey;
	}

	public String getNextRecordHandlerManagerRef() {
		return nextRecordHandlerManagerRef;
	}

	public String getRecordHandlerRef() {
		return recordHandlerRef;
	}

	public void setNextRecordHandlerManagerRef(String nextRecordHandlerManagerName) {
		nextRecordHandlerManagerRef = nextRecordHandlerManagerName;
	}

	public void setRecordHandlerRef(String recordHandlerName) {
		recordHandlerRef = recordHandlerName;
	}

	public String getRecordHandlerManagerRef() {
		return recordHandlerManagerRef;
	}

	public void setRecordHandlerManagerRef(String recordHandlerManagerName) {
		recordHandlerManagerRef = recordHandlerManagerName;
	}

	public String getResultHandlerRef() {
		return resultHandlerRef;
	}

	public void setResultHandlerRef(String resultHandlerName) {
		resultHandlerRef = resultHandlerName;
	}

	public IResultHandler getResultHandler() {
		return resultHandler;
	}

	public void setResultHandler(IResultHandler handler) {
		resultHandler = handler;
	}

}
