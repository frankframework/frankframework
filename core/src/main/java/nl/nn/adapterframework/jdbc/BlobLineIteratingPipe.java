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

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.JdbcUtil;


/** 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
@IbisDescription(
	"Pipe that iterates over the lines in a blob. \n" + 
	"Iteration stops if condition returns anything other than <code>false</code> or an empty result. \n" + 
	"For example, to stop after the second child element has been processed, one of the following expressions could be used: \n" + 
	"<table>  \n" + 
	"<tr><td><li><code>result[@item='2']</code></td><td>returns result element after second child element has been processed</td></tr> \n" + 
	"<tr><td><li><code>result/@item='2'</code></td><td>returns <code>false</code> after second child element has been processed, <code>true</code> for others</td></tr> \n" + 
	"</table> \n" + 
	"<table> \n" + 
	"</td><td>&nbsp;</td></tr> \n" + 
	"<tr><td>{@link #setRemoveXmlDeclarationInResults(boolean) removeXmlDeclarationInResults}</td><td>postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document</td><td>false</td></tr> \n" + 
	"<tr><td>{@link #setCollectResults(boolean) collectResults}</td><td>controls whether all the results of each iteration will be collected in one result message. If set <code>false</code>, only a small summary is returned</td><td>true</td></tr> \n" + 
	"<tr><td>{@link #setQuery(String) query}</td><td>the SQL query text</td><td>&nbsp;</td></tr> \n" + 
	"<tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr> \n" + 
	"<tr><td>{@link #setBlockSize(int) blockSize}</td><td>controls multiline behaviour. when set to a value greater than 0, it specifies the number of rows send in a block to the sender.</td><td>0 (one line at a time, no prefix of suffix)</td></tr> \n" + 
	"<tr><td>{@link #setBlockPrefix(String) blockPrefix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the start of the set of lines.</td><td>&lt;block&gt;</td></tr> \n" + 
	"<tr><td>{@link #setBlockSuffix(String) blockSuffix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the end of the set of lines.</td><td>&lt;/block&gt;</td></tr> \n" + 
	"</table> \n" + 
	"<table border=\"1\"> \n" + 
	"<tr><th>nested elements</th><th>description</th></tr> \n" + 
	"<tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send messages with</td></tr> \n" + 
	"<tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr> \n" + 
	"<tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender, if this is a {@link nl.nn.adapterframework.core.ISenderWithParameters ISenderWithParameters}</td></tr> \n" + 
	"<tr><td><code>inputValidator</code></td><td>specification of Pipe to validate input messages</td></tr> \n" + 
	"<tr><td><code>outputValidator</code></td><td>specification of Pipe to validate output messages</td></tr> \n" + 
	"<tr><td>{@link nl.nn.adapterframework.core.ITransactionalStorage messageLog}</td><td>log of all messages sent</td></tr> \n" + 
	"</table> \n" + 
	"</p> \n" + 
	"<p><b>Exits:</b> \n" + 
	"<table border=\"1\"> \n" + 
	"<tr><th>state</th><th>condition</th></tr> \n" + 
	"<tr><td>\"success\"</td><td>default when a good message was retrieved (synchronous sender), or the message was successfully sent and no listener was specified and the sender was not synchronous</td></tr> \n" + 
	"<tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as \"success\"</td></tr> \n" + 
	"<tr><td>\"timeout\"</td><td>no data was received (timeout on listening), if the sender was synchronous or a listener was specified.</td></tr> \n" + 
	"<tr><td>\"exception\"</td><td>an exception was thrown by the Sender or its reply-Listener. The result passed to the next pipe is the exception that was caught.</td></tr> \n" + 
	"</table> \n" + 
	"</p> \n" 
)
public class BlobLineIteratingPipe extends LobLineIteratingPipeBase {

	protected Reader getReader(ResultSet rs) throws SenderException {
		try {
			return JdbcUtil.getBlobReader(rs,1,querySender.getBlobCharset(),querySender.isBlobsCompressed());
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
