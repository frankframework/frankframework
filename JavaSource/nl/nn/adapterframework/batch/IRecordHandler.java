/*
 * $Log: IRecordHandler.java,v $
 * Revision 1.8  2007-09-24 14:55:33  europe\L190409
 * support for parameters
 *
 * Revision 1.7  2007/09/13 12:35:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.6  2007/09/10 11:05:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed mustPrefix() to isNewRecordType()
 *
 * Revision 1.5  2007/05/03 11:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add methods configure(), open() and close()
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

import java.util.ArrayList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Interface for transforming a record (= structured ASCII line). 
 * 
 * @author John Dekker
 */
public interface IRecordHandler extends INamedObject {

	public void configure() throws ConfigurationException;

	/**
	 * This method will be called to start the sender. After this
	 * method is called the sendMessage method may be called
	 */ 
	public void open() throws SenderException;

	/**
	 * Stop/close the sender and deallocate resources.
	 */ 
	public void close() throws SenderException;

	/**
	 * Parse the line into an array of fields.
	 * 
	 * @return ArrayList with String values for each inputfield
	 * @throws Exception
	 */
	ArrayList parse(PipeLineSession session, String record) throws Exception;

	/**
	 * Perform an action on the array of fields.
	 * 
	 * @return transformed result
	 * @throws Exception
	 */	
	Object handleRecord(PipeLineSession session, ArrayList parsedRecord, ParameterResolutionContext prc) throws Exception;
	
	/**
	 * @param equalRecordTypes flag indicates if the previous record was of same type as the current
	 * @param prevRecord values of the input fields of the previous record
	 * @param curRecord values of the input fields of the current record
	 * @return boolean that indicates whether a prefix must be added to the transformed result
	 * @throws Exception
	 */
	boolean isNewRecordType(PipeLineSession session, boolean equalRecordTypes, ArrayList prevRecord, ArrayList curRecord) throws Exception;
	
}
