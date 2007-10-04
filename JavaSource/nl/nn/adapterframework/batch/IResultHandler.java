/*
 * $Log: IResultHandler.java,v $
 * Revision 1.6.2.1  2007-10-04 13:07:13  europe\L190409
 * synchronize with HEAD (4.7.0)
 *
 * Revision 1.10  2007/09/24 14:55:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameters
 *
 * Revision 1.9  2007/09/19 11:15:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added openDocument() and closeDocument()
 * added openBlock() and closeBlock()
 *
 * Revision 1.8  2007/09/17 07:43:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added hasPrefix()
 *
 * Revision 1.7  2007/09/10 11:07:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed logic processing from writePrefix to calling class
 * renamed writePrefix() and writeSuffix() into open/closeRecordType()
 *
 * Revision 1.6  2007/08/03 08:26:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added configure(), open() and close()
 *
 * Revision 1.5  2007/07/24 07:59:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * change inputFilename to streamId
 *
 * Revision 1.4  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:03  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:20  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Interface for handling a transformed record.
 * 
 * @author  John Dekker
 * @version Id
 */
public interface IResultHandler extends INamedObject {

	public void configure() throws ConfigurationException;
	public void open() throws SenderException;
	public void close() throws SenderException;
	
	/**
	 * Called once, before the first record of a stream is presented to handleResult.
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message
	 */
	void openDocument(PipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception;
	void closeDocument(PipeLineSession session, String streamId, ParameterResolutionContext prc);

	/**
	 * write a result ta record. 
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @param recordKey key of the record (describes the record type)
	 * @param result transformed record
	 */
	void handleResult(PipeLineSession session, String streamId, String recordKey, Object result, ParameterResolutionContext prc) throws Exception;
	
	/**
	 * Called when all records in the original file are handled.
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @return the name or names of the output files
	 */
	Object finalizeResult(PipeLineSession session, String streamId, boolean error, ParameterResolutionContext prc) throws Exception;

	/**
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @param mustPrefix boolean indicates if the prefix must be written
	 * @param hasPreviousRecord boolean indicates if a previous record has been written, in case a suffix has to be written first
	 * @throws Exception
	 */
	void openRecordType(PipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception;
	
	/**
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 */
	void closeRecordType(PipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception;
	
	void openBlock(PipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception;
	void closeBlock(PipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception;

	/**
	 * @return true if this resulthandler should be used for all flows if no resulthandler is specified for that flow 
	 */
	boolean isDefault();
	void setDefault(boolean isDefault);
	
	boolean hasPrefix();
}
