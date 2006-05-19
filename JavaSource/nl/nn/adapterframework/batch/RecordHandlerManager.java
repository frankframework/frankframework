/*
 * $Log: RecordHandlerManager.java,v $
 * Revision 1.4  2006-05-19 09:28:36  europe\m00i745
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

import java.util.Collection;
import java.util.HashMap;

import nl.nn.adapterframework.core.PipeLineSession;

/**
 * The manager decides which handlers to be used for a specific record.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.FieldPositionRecordHandlerManager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the manager</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInitial(boolean) initial}</td><td>This manager is the initial manager, i.e. to be used for the first record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author John Dekker
 */
public class RecordHandlerManager implements IRecordHandlerManager {
	public static final String version = "$RCSfile: RecordHandlerManager.java,v $  $Revision: 1.4 $ $Date: 2006-05-19 09:28:36 $";

	private HashMap valueHandlersMap;
	private String name;
	private boolean initial;

	RecordHandlerManager() {
		this.valueHandlersMap = new HashMap();
	}
	
	/* (non-Javadoc)
	 * @see nl.nn.ibis4fundation.transformation.IRecordHandlerManager#getRecordFactoryUsingFilename(java.lang.String)
	 */
	public IRecordHandlerManager getRecordFactoryUsingFilename(PipeLineSession session, String inputFilename) {
		return this;
	}
	
	/* (non-Javadoc)
	 * @see nl.nn.ibis4fundation.transformation.IRecordHandlerManager#addHandler(nl.nn.ibis4fundation.transformation.RecordHandlingFlow)
	 */
	public void addHandler(RecordHandlingFlow handlers) {
		valueHandlersMap.put(handlers.getRecordKey(), handlers);
		if (handlers.getNextRecordHandlerManager() == null) {
			handlers.setNextRecordHandlerManager(this);
		}
	}

	/* (non-Javadoc)
	 * @see nl.nn.ibis4fundation.transformation.IRecordHandlerManager#getRecordHandlers()
	 */
	public Collection getRecordHandlers() {
		return valueHandlersMap.values();	
	}
	
	/* (non-Javadoc)
	 * @see nl.nn.ibis4fundation.transformation.IRecordHandlerManager#getRecordHandler(java.lang.String)
	 */
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
		return (RecordHandlingFlow)valueHandlersMap.get("*");
	}

	/**
	 * @param recordKey
	 * @return RecordHandlingFlow element to be used for handling records of type recordkey
	 * @throws Exception
	 */
	public RecordHandlingFlow getRecordHandlerByKey(String recordKey) throws Exception {
		RecordHandlingFlow rhf =(RecordHandlingFlow)valueHandlersMap.get(recordKey);
		if  (rhf == null) {
			rhf =(RecordHandlingFlow)valueHandlersMap.get("*");
			if  (rhf == null) {
				throw new Exception("No handlers (flow) found for recordKey [" + recordKey + "]");
			}
		}
		return rhf;
		
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.INamedObject#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.INamedObject#setName(java.lang.String)
	 */
	public void setName(String string) {
		name = string;
	}

	/* (non-Javadoc)
	 * @see nl.nn.ibis4fundation.transformation.IRecordHandlerManager#isInitial()
	 */
	public boolean isInitial() {
		return initial;
	}

	/* (non-Javadoc)
	 * @see nl.nn.ibis4fundation.transformation.IRecordHandlerManager#setInitial(boolean)
	 */
	public void setInitial(boolean b) {
		initial = b;
	}

}
