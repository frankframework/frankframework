/*
 * $Log: Result2LobWriterBase.java,v $
 * Revision 1.8  2012-06-01 10:52:52  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.7  2012/02/17 18:04:02  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Use proxiedDataSources for JdbcIteratingPipeBase too
 * Call close on original/proxied connection instead of connection from statement that might be the unproxied connection
 *
 * Revision 1.6  2011/11/30 13:51:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/04/13 08:39:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Blob and Clob support using DbmsSupport
 *
 * Revision 1.3  2007/09/24 14:58:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameters
 *
 * Revision 1.2  2007/09/19 13:06:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modify exception type thrown
 *
 * Revision 1.1  2007/08/03 08:43:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first versions of Jdbc result writers
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.batch.ResultWriter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.JdbcUtil;


/**
 * Baseclass for batch {@link nl.nn.adapterframework.batch.IResulthandler Resulthandler} that writes the transformed record to a LOB.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.Result2LobWriterBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the resulthandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td><i>Deprecated</i> Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td><i>Deprecated</i> Suffix that has to be written after the record, if the record is in another block than the next record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnOpenDocument(String) onOpenDocument}</td><td>String that is written before any data of results is written</td><td>&lt;document name=&quot;#name#&quot;&gt;</td></tr>
 * <tr><td>{@link #setOnCloseDocument(String) onCloseDocument}</td><td>String that is written after all data of results is written</td><td>&lt;/document&gt;</td></tr>
 * <tr><td>{@link #setOnOpenBlock(String) onOpenBlock}</td><td>String that is written before the start of each logical block, as defined in the flow</td><td>&lt;#name#&gt;</td></tr>
 * <tr><td>{@link #setOnCloseBlock(String) onCloseBlock}</td><td>String that is written after the end of each logical block, as defined in the flow</td><td>&lt;/#name#&gt;</td></tr>
 * <tr><td>{@link #setBlockNamePattern(String) blockNamePattern}</td><td>String that is replaced by name of block or name of stream in above strings</td><td>#name#</td></tr>
 * </table>
 * <p/>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the resultHandler will be applied to the SQL statement</td></tr>
 * </table>
 * <p/>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public abstract class Result2LobWriterBase extends ResultWriter {
	public static final String version = "$RCSfile: Result2LobWriterBase.java,v $  $Revision: 1.8 $ $Date: 2012-06-01 10:52:52 $";
	
	protected Map openStreams = Collections.synchronizedMap(new HashMap());
	protected Map openConnections = Collections.synchronizedMap(new HashMap());
	protected Map openResultSets = Collections.synchronizedMap(new HashMap());
	protected Map openLobHandles = Collections.synchronizedMap(new HashMap());

	protected FixedQuerySender querySender = new FixedQuerySender();

	public void configure() throws ConfigurationException {
		super.configure();
		querySender.setName("querySender of "+getName());
		querySender.configure();
	}
	
	public void open() throws SenderException {
		super.open();
		querySender.open();
	}

	public void close() throws SenderException {
		try {
			super.close();
		} finally {
			querySender.close();
		}
	}

	protected abstract Object getLobHandle(IDbmsSupport dbmsSupport, ResultSet rs)                   throws SenderException;
	protected abstract Writer getWriter   (IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException;
	protected abstract void   updateLob   (IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException;
	
	protected Writer createWriter(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		querySender.sendMessage(streamId, streamId);
		Connection conn=querySender.getConnection();
		openConnections.put(streamId, conn);
		PreparedStatement stmt = querySender.getStatement(conn,session.getMessageId(),streamId, true);
		ResultSet rs =stmt.executeQuery();
		openResultSets.put(streamId,rs);
		IDbmsSupport dbmsSupport=querySender.getDbmsSupport();
		Object lobHandle=getLobHandle(dbmsSupport, rs);
		openLobHandles.put(streamId, lobHandle);
		return getWriter(dbmsSupport, lobHandle, rs);
	}
	
	public Object finalizeResult(IPipeLineSession session, String streamId, boolean error, ParameterResolutionContext prc) throws Exception {
		try {
			return super.finalizeResult(session,streamId, error, prc);
		} finally {
			Object lobHandle = openLobHandles.get(streamId);
			Connection conn = (Connection)openResultSets.get(streamId);
			ResultSet rs = (ResultSet)openResultSets.get(streamId);
			if (rs!=null) {
				updateLob(querySender.getDbmsSupport(), lobHandle, rs);
				JdbcUtil.fullClose(conn, rs);
			}
		}
	}

	
	public void setQuery(String query) {
		querySender.setQuery(query);
	}

	public void setDatasourceName(String datasourceName) {
		querySender.setDatasourceName(datasourceName);
	}

	public String getPhysicalDestinationName() {
		return querySender.getPhysicalDestinationName(); 
	}

	public void setJmsRealm(String jmsRealmName) {
		querySender.setJmsRealm(jmsRealmName);
	}

}
