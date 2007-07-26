/*
 * $Log: BlobLineIteratingPipe.java,v $
 * Revision 1.2  2007-07-26 16:22:00  europe\L190409
 * now based on LobLineIteratingPipe
 *
 * Revision 1.1  2007/07/19 15:06:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 */
package nl.nn.adapterframework.jdbc;

import java.io.Reader;
import java.sql.ResultSet;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Pipe that iterates over the lines in a blob.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.jdbc.BlobLineIteratingPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit</td><td>"receiver timed out"</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply</td><td>CORRELATIONID</td></tr>
 * <tr><td>{@link #setStopConditionXPathExpression(String) stopConditionXPathExpression}</td><td>expression evaluated on each result if set. 
 * 		Iteration stops if condition returns anything other than <code>false</code> or an empty result.
 * For example, to stop after the second child element has been processed, one of the following expressions could be used:
 * <table> 
 * <tr><td><li><code>result[@item='2']</code></td><td>returns result element after second child element has been processed</td></tr>
 * <tr><td><li><code>result/@item='2'</code></td><td>returns <code>false</code> after second child element has been processed, <code>true</code> for others</td></tr>
 * </table> 
 * </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveXmlDeclarationInResults(boolean) removeXmlDeclarationInResults}</td><td>postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document</td><td>false</td></tr>
 * <tr><td>{@link #setCollectResults(boolean) collectResults}</td><td>controls whether all the results of each iteration will be collected in one result message. If set <code>false</code>, only a small summary is returned</td><td>true</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read and write BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class BlobLineIteratingPipe extends LobLineIteratingPipeBase {

	protected Reader getReader(ResultSet rs) throws SenderException {
		try {
			return JdbcUtil.getBlobReader(rs,1,querySender.getBlobCharset(),querySender.isBlobsCompressed());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	public void setBlobCharset(String charset) {
		querySender.setBlobCharset(charset);
	}

	public void setBlobsCompressed(boolean compressed) {
		querySender.setBlobsCompressed(compressed);
	}
	
}
