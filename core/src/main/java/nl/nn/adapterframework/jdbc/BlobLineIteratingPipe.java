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

import java.io.Reader;
import java.sql.ResultSet;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Pipe that iterates over the lines in a blob.
 *
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when a good message was retrieved (synchronous sender), or the message was successfully sent and no listener was specified and the sender was not synchronous</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), if the sender was synchronous or a listener was specified.</td></tr>
 * <tr><td>"exception"</td><td>an exception was thrown by the Sender or its reply-Listener. The result passed to the next pipe is the exception that was caught.</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class BlobLineIteratingPipe extends LobLineIteratingPipeBase {

	@Override
	protected Reader getReader(ResultSet rs) throws SenderException {
		try {
			return JdbcUtil.getBlobReader(querySender.getDbmsSupport(), rs,1,querySender.getBlobCharset(),querySender.isBlobsCompressed());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@IbisDoc({"charset used to read and write blobs", "utf-8"})
	public void setBlobCharset(String charset) {
		querySender.setBlobCharset(charset);
	}

	@IbisDoc({"controls whether blobdata is stored compressed in the database", "true"})
	public void setBlobsCompressed(boolean compressed) {
		querySender.setBlobsCompressed(compressed);
	}
	
}
