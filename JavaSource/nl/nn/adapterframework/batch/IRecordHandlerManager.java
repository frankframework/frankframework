/*
 * $Log: IRecordHandlerManager.java,v $
 * Revision 1.6.2.1  2007-10-10 14:30:46  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.7  2007/10/08 12:14:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.6  2007/07/24 16:14:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added configure()
 *
 * Revision 1.5  2007/07/24 07:59:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update javadoc
 *
 * Revision 1.4  2006/05/19 09:28:38  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:03  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:22  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Interface for handling a transformed record.
 * 
 * A RecordHandlerManager decides, based on some implementation dependent algorithm, which record handler
 * is to be used to process a record. 
 * A record manager keeps a table of flow-elements, that each define a recordhandler, resulthandler and, 
 * optionally, a next-manager.
 * 
 * @author John Dekker
 */
public interface IRecordHandlerManager extends INamedObject {

	public void configure(Map registeredManagers, Map registeredRecordHandlers, Map registeredResultHandlers, IResultHandler defaultHandler) throws ConfigurationException;

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
