/*
 * $Log: BatchBlobTransformerPipe.java,v $
 * Revision 1.5  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2010/05/03 17:04:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked stream handling, to allow for binary records.
 *
 * Revision 1.2  2010/02/25 13:41:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted javadoc for resultOnTimeOut attribute
 *
 * Revision 1.1  2007/08/03 08:44:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed TransformingPipes to TransformerPipes
 *
 * Revision 1.1  2007/07/26 16:19:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Pipe that batch-transforms the lines in a CLOB.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.jdbc.BatchBlobTransformingPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit (e.g. "receiver timed out").</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply</td><td>CORRELATIONID</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read and write BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
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
 * @version Id
 */
public class BatchBlobTransformerPipe extends BatchTransformerPipeBase {

	protected Reader getReader(ResultSet rs, String charset, String streamId, PipeLineSession session) throws SenderException {
		try {
			InputStream blobStream=JdbcUtil.getBlobInputStream(rs,1,querySender.isBlobsCompressed());
			return getReaderFactory().getReader(blobStream, charset, streamId, session);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	public void setBlobCharset(String charset) {
		setCharset(charset);
		ConfigurationWarnings.getInstance().add(log,"attribute blobCharset is replaced by charset for class ["+this.getClass().getName()+"]");
	}

	public void setBlobsCompressed(boolean compressed) {
		querySender.setBlobsCompressed(compressed);
	}
	
}
