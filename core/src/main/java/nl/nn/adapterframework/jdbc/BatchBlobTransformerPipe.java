/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Pipe that batch-transforms the lines in a CLOB.
 *
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IInputStreamReaderFactory readerFactory}</td><td>Factory for reader of inputstream. Default implementation {@link nl.nn.adapterframework.batch.InputStreamReaderFactory} just converts using the specified characterset</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandlerManager manager}</td><td>Manager determines which handlers are to be used for the current line. 
 * 			If no manager is specified, a default manager and flow are created. The default manager 
 * 			always uses the default flow. The default flow always uses the first registered recordHandler 
 * 			(if available) and the first registered resultHandler (if available).</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.RecordHandlingFlow manager/flow}</td><td>Element that contains the handlers for a specific record type, to be assigned to the manager</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandler recordHandler}</td><td>Handler for transforming records of a specific type</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IResultHandler resultHandler}</td><td>Handler for processing transformed records</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class BatchBlobTransformerPipe extends BatchTransformerPipeBase {

	@Override
	protected Reader getReader(ResultSet rs, String charset, String streamId, IPipeLineSession session) throws SenderException {
		try {
			InputStream blobStream=JdbcUtil.getBlobInputStream(querySender.getDbmsSupport(), rs ,1 , querySender.isBlobsCompressed());
			return getReaderFactory().getReader(blobStream, charset, streamId, session);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Deprecated
	@ConfigurationWarning("please use attribute 'charset' instead")
	public void setBlobCharset(String charset) {
		setCharset(charset);
	}

	@IbisDoc({"controls whether blobdata is stored compressed in the database", "true"})
	public void setBlobsCompressed(boolean compressed) {
		querySender.setBlobsCompressed(compressed);
	}
	
}
