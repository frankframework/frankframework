/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: IResultHandler.java,v $
 * Revision 1.14  2012-06-01 10:52:48  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.13  2011/11/30 13:51:56  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.11  2010/01/27 13:31:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added isBlockByRecordType()
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
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Interface for handling a transformed record.
 * 
 * @author  John Dekker
 * @version $Id$
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
	void openDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception;
	void closeDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc);

	/**
	 * write a result ta record. 
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @param recordKey key of the record (describes the record type)
	 * @param result transformed record
	 */
	void handleResult(IPipeLineSession session, String streamId, String recordKey, Object result, ParameterResolutionContext prc) throws Exception;
	
	/**
	 * Called when all records in the original file are handled.
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @return the name or names of the output files
	 */
	Object finalizeResult(IPipeLineSession session, String streamId, boolean error, ParameterResolutionContext prc) throws Exception;

	/**
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @param mustPrefix boolean indicates if the prefix must be written
	 * @param hasPreviousRecord boolean indicates if a previous record has been written, in case a suffix has to be written first
	 * @throws Exception
	 */
	void openRecordType(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception;
	
	/**
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 */
	void closeRecordType(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception;
	
	void openBlock(IPipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception;
	void closeBlock(IPipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception;

	/**
	 * @return true if this resulthandler should be used for all flows if no resulthandler is specified for that flow 
	 */
	boolean isDefault();
	void setDefault(boolean isDefault);
	
	boolean hasPrefix();

	/**
	 * @return true causes groups of identical records, indicated by {@link IRecordHandler.isNewRecordType newRecordType} to appear in a block. 
	 */
	boolean isBlockByRecordType();
	
}
