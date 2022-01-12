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
package nl.nn.adapterframework.jdbc;

import java.io.Reader;
import java.sql.ResultSet;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;

/**
 * Pipe that batch-transforms the lines in a CLOB.
 *
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandlerManager manager}</td><td>Manager determines which handlers are to be used for the current line</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandlerManager manager}</td><td>Manager determines which handlers are to be used for the current line. 
 * 			If no manager is specified, a default manager and flow are created. The default manager 
 * 			always uses the default flow. The default flow always uses the first registered recordHandler 
 * 			(if available) and the first registered resultHandler (if available).</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandler recordHandler}</td><td>Handler for transforming records of a specific type</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IResultHandler resultHandler}</td><td>Handler for processing transformed records</td></tr>
 * </table>
 * </p>
 * 
 * N.B. the readerFactory is not used by this class.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class BatchClobTransformerPipe extends BatchTransformerPipeBase {


	@Override
	protected Reader getReader(ResultSet rs, String charset, String streamId, PipeLineSession session) throws SenderException {
		try {
			return rs.getCharacterStream(1);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
	
}
