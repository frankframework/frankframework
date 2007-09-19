/*
 * $Log: Result2LobWriterBase.java,v $
 * Revision 1.2  2007-09-19 13:06:50  europe\L190409
 * modify exception type thrown
 *
 * Revision 1.1  2007/08/03 08:43:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first versions of Jdbc result writers
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.batch.ResultWriter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;


/**
 * Baseclass for batch {@link nl.nn.adapterframework.batch.IResulthandler Resulthandler} that writes the transformed record to a LOB.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.Result2LobWriterBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultResultHandler(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public abstract class Result2LobWriterBase extends ResultWriter {
	public static final String version = "$RCSfile: Result2LobWriterBase.java,v $  $Revision: 1.2 $ $Date: 2007-09-19 13:06:50 $";
	
	protected Map openStreams = Collections.synchronizedMap(new HashMap());
	protected Map openResultSets = Collections.synchronizedMap(new HashMap());

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

	protected abstract Writer getWriter(ResultSet rs) throws SenderException;
	
	protected Writer createWriter(PipeLineSession session, String streamId) throws Exception {
		querySender.sendMessage(streamId, streamId);
		Connection conn=querySender.getConnection();
		
		PreparedStatement stmt = querySender.getStatement(conn,session.getMessageId(),streamId);
		//TODO should apply parameters in some way
		ResultSet rs =stmt.executeQuery();
		openResultSets.put(streamId,rs);
		return getWriter(rs);
	}
	
	public Object finalizeResult(PipeLineSession session, String streamId, boolean error) throws Exception {
		try {
			return super.finalizeResult(session,streamId, error);
		} finally {
			ResultSet rs = (ResultSet)openResultSets.get(streamId);
			if (rs!=null) {
				JdbcUtil.fullClose(rs);
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
