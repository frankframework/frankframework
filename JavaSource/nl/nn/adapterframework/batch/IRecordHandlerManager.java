/*
 * $Log: IRecordHandlerManager.java,v $
 * Revision 1.1  2005-10-11 13:00:22  europe\m00f531
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.Collection;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Interface for handling a transformed record 
 * 
 * @author John Dekker
 */
public interface IRecordHandlerManager extends INamedObject {
	/**
	 * @return List of RecordHandlingFlow elements that this manager manages
	 */
	Collection getRecordHandlers();

	/**
	 * @param flow New flow to be added to the managed flow elements
	 */
	void addHandler(RecordHandlingFlow flow);
	
	/**
	 * @param record 
	 * @return the RecordHandlingFlow element to be used to handle the record
	 * @throws Exception
	 */
	RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception;
	
	/**
	 * @param filename
	 * @return the IRecordHandlingManager to be used initially based on the name of the input file 
	 */
	IRecordHandlerManager getRecordFactoryUsingFilename(PipeLineSession session, String filename);
	
	/**
	 * @param initialFactory inidicates if this manager is the initial manager
	 */
	void setInitial(boolean initialFactory);
	boolean isInitial();
}
