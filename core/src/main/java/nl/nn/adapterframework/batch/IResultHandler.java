/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.FrankDocGroup;
import nl.nn.adapterframework.pipes.AbstractPipe;

/**
 * Interface for handling a transformed record.
 *
 * @author  John Dekker
 * @deprecated Old and non-maintained functionality. Deprecated since v7.8
 */
@Deprecated
@FrankDocGroup(name = "Batch")
@ConfigurationWarning("Old and non-maintained functionality. Deprecated since v7.8")
public interface IResultHandler extends IConfigurable {

	public void setPipe(AbstractPipe pipe);
	public void open() throws SenderException;
	public void close() throws SenderException;

	/**
	 * Called once, before the first record of a stream is presented to handleResult.
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message
	 */
	void openDocument(PipeLineSession session, String streamId) throws Exception;
	void closeDocument(PipeLineSession session, String streamId);

	/**
	 * write a result record.
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @param recordKey key of the record (describes the record type)
	 * @param result transformed record
	 */
	void handleResult(PipeLineSession session, String streamId, String recordKey, String result) throws Exception;

	/**
	 * Called when all records in the original file are handled.
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 * @return the name or names of the output files
	 */
	String finalizeResult(PipeLineSession session, String streamId, boolean error) throws Exception;

	/**
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 */
	void openRecordType(PipeLineSession session, String streamId) throws Exception;

	/**
	 * @param session  current PipeLineSession
	 * @param streamId identification of the original file/stream/message containing the untransformed records
	 */
	void closeRecordType(PipeLineSession session, String streamId) throws Exception;

	void openBlock(PipeLineSession session, String streamId, String blockName, Map<String, Object> blocks) throws Exception;
	void closeBlock(PipeLineSession session, String streamId, String blockName, Map<String, Object> blocks) throws Exception;

	/**
	 * @return true if this resulthandler should be used for all flows if no resulthandler is specified for that flow
	 */
	boolean isDefault();
	void setDefault(boolean isDefault);

	boolean hasPrefix();

	/**
	 * @return true causes groups of identical records, indicated by {@link IRecordHandler#isNewRecordType(PipeLineSession, boolean, List, List) newRecordType} to appear in a block.
	 */
	boolean isBlockByRecordType();
}
