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
package nl.nn.adapterframework.batch;

import java.util.List;

import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.pipes.AbstractPipe;

/**
 * Interface for handling a transformed record.
 * 
 * @author  John Dekker
 */
public interface IResultHandler extends IConfigurable {

	public void setPipe(AbstractPipe pipe);
	public void open() throws SenderException;
	public void close() throws SenderException;
	
	/**
	 * Called once, before the first record of a stream is presented to handleResult.
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message
	 */
	void openDocument(IPipeLineSession session, String streamId) throws Exception;
	void closeDocument(IPipeLineSession session, String streamId);

	/**
	 * write a result record. 
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @param recordKey key of the record (describes the record type)
	 * @param result transformed record
	 */
	void handleResult(IPipeLineSession session, String streamId, String recordKey, String result) throws Exception;
	
	/**
	 * Called when all records in the original file are handled.
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @return the name or names of the output files
	 */
	String finalizeResult(IPipeLineSession session, String streamId, boolean error) throws Exception;

	/**
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 */
	void openRecordType(IPipeLineSession session, String streamId) throws Exception;
	
	/**
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 */
	void closeRecordType(IPipeLineSession session, String streamId) throws Exception;
	
	void openBlock(IPipeLineSession session, String streamId, String blockName) throws Exception;
	void closeBlock(IPipeLineSession session, String streamId, String blockName) throws Exception;

	/**
	 * @return true if this resulthandler should be used for all flows if no resulthandler is specified for that flow 
	 */
	boolean isDefault();
	void setDefault(boolean isDefault);
	
	boolean hasPrefix();

	/**
	 * @return true causes groups of identical records, indicated by {@link IRecordHandler#isNewRecordType(IPipeLineSession, boolean, List, List) newRecordType} to appear in a block. 
	 */
	boolean isBlockByRecordType();
	
}
