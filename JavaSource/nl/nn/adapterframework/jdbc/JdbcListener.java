/*
 * $Log: JdbcListener.java,v $
 * Revision 1.12  2011-03-16 16:42:40  L190409
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 * Revision 1.11  2009/08/04 11:24:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for messages in CLOBs and BLOBs
 *
 * Revision 1.10  2009/03/26 14:47:36  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added LOCKROWS_SUFFIX
 *
 * Revision 1.9  2008/12/10 08:35:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved locking and selection mechanism: now works in multiple threads. 
 * improved disaster recovery: no more specific 'in process' status, rolls back to original state (where apropriate)
 *
 * Revision 1.8  2008/02/28 16:22:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use PipeLineSession.setListenerParameters()
 *
 * Revision 1.7  2007/12/10 10:05:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * put id and cid in session
 *
 * Revision 1.6  2007/11/15 12:38:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed message wrapping
 *
 * Revision 1.5  2007/10/18 15:55:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed returning of message text
 *
 * Revision 1.4  2007/10/03 08:48:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.3  2007/09/13 09:08:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allowed use outside transaction
 *
 * Revision 1.2  2007/09/12 09:17:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first working version
 *
 * Revision 1.1  2007/09/11 11:53:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added JdbcListeners
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;

/**
 * JdbcListener base class.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcQueryListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyField(String) keyField}</td>  <td>primary key field of the table, used to identify messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td>  <td>(optional) field containing the message data</td><td><i>same as keyField</i></td></tr>
 * <tr><td>{@link #setMessageFieldType(String) messageFieldType}</td>  <td>type of the field containing the message data: either String, clob or blob</td><td><i>String</i></td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is considered stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setBlobSmartGet(boolean) blobSmartGet}</td><td>controls automatically whether blobdata is stored compressed and/or serialized in the database</td><td>false</td></tr>

 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>username used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>password used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConnectionsArePooled(boolean) connectionsArePooled}</td><td>when true, it is assumed that an connectionpooling mechanism is present. Before a message is sent, a new connection is obtained, that is closed after the message is sent. When transacted is true, connectionsArePooled is true, too</td><td>true</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class JdbcListener extends JdbcFacade implements IPullingListener {

	private String startLocalTransactionQuery;
	private String commitLocalTransactionQuery;
	private String selectQuery;
	private String updateStatusToProcessedQuery;
	private String updateStatusToErrorQuery;

	private String keyField;
	private String messageField;
	private String messageFieldType="String";

	private String blobCharset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private boolean blobsCompressed=true;
	private boolean blobSmartGet=false;
	
	protected Connection connection=null;

	private String preparedSelectQuery;

	private static final boolean trace=false;

	public void configure() throws ConfigurationException {
		try {
			if (getDatasource()==null) {
				throw new ConfigurationException(getLogPrefix()+"has no datasource");
			}
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
		try {
			preparedSelectQuery = getDbmsSupport().prepareQueryTextForWorkQueueReading(1, getSelectQuery());
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
	}

	public void open() throws ListenerException {
		if (!isConnectionsArePooled()) {
			try {
				connection = getConnection();
			} catch (JdbcException e) {
				throw new ListenerException(e);
			}
		}
	}

	public void close() throws ListenerException {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			throw new ListenerException(getLogPrefix() + "caught exception stopping listener", e);
		} finally {
			connection = null;
		}
	}
	
	public Map openThread() throws ListenerException {
		return new HashMap();
	}

	public void closeThread(Map threadContext) throws ListenerException {
	}

	public Object getRawMessage(Map threadContext) throws ListenerException {
		if (isConnectionsArePooled()) {
			Connection c = null;
			try {
				c = getConnection();
				return getRawMessage(c,threadContext);
			} catch (JdbcException e) {
				throw new ListenerException(e);
			} finally {
				if (c!=null) {
					try {
						c.close();
					} catch (SQLException e) {
						log.warn(new ListenerException(getLogPrefix() + "caught exception closing listener after retrieving message", e));
					}
				}
			}
			
		} else {
			synchronized (connection) {
				return getRawMessage(connection,threadContext);
			}
		}
	}

	protected Object getRawMessage(Connection conn, Map threadContext) throws ListenerException {
		boolean inTransaction=false;
		
		try {
			inTransaction=JtaUtil.inTransaction();
		} catch (Exception e) {
			log.warn(getLogPrefix()+"could not determing XA transaction status, assuming not in XA transaction: "+ e.getMessage());
			inTransaction=false;
		}
		try {
			if (!inTransaction) {
				execute(conn,getStartLocalTransactionQuery());
			}

			String query=preparedSelectQuery;
			try {
				Statement stmt= null;
				try {
					stmt = conn.createStatement();
					stmt.setFetchSize(1);
					ResultSet rs=null;
					try {
						if (trace && log.isDebugEnabled()) log.debug("executing query for ["+query+"]");
						rs = stmt.executeQuery(query);
						if (rs.isAfterLast() || !rs.next()) {
							return null;
						}
						Object result;
						String key=rs.getString(getKeyField());
						
						if (StringUtils.isNotEmpty(getMessageField())) {
							String message;
							if ("clob".equalsIgnoreCase(getMessageFieldType())) {
								message=JdbcUtil.getClobAsString(rs,getMessageField(),false);
							} else {
								if ("blob".equalsIgnoreCase(getMessageFieldType())) {
									message=JdbcUtil.getBlobAsString(rs,getMessageField(),getBlobCharset(),false,isBlobsCompressed(),isBlobSmartGet());
								} else {
									message=rs.getString(getMessageField());
								}
							}
							// log.debug("building wrapper for key ["+key+"], message ["+message+"]");
							MessageWrapper mw = new MessageWrapper();
							mw.setId(key);
							mw.setText(message);
							result=mw;
						} else {
							result = key;
						}
						return result;
					} finally {
						if (rs!=null) {
							rs.close();
						}
					}
						
				} finally {
					if (stmt!=null) {
						stmt.close();
					}
				}
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix() + "caught exception retrieving message using query ["+query+"]", e);
			}
		} finally {
			if (!inTransaction) {
				execute(conn,getCommitLocalTransactionQuery());
			}
		}
		
	}

	public String getIdFromRawMessage(Object rawMessage, Map context) throws ListenerException {
		String id;
		if (rawMessage instanceof IMessageWrapper) {
			id = ((IMessageWrapper)rawMessage).getId();
		} else {
			id = (String)rawMessage;
		}
		PipeLineSession.setListenerParameters(context, id, id, null, null);
		return id;
	}

	public String getStringFromRawMessage(Object rawMessage, Map context) throws ListenerException {
		String message;
		if (rawMessage instanceof IMessageWrapper) {
			message = ((IMessageWrapper)rawMessage).getText();
		} else {
			message = (String)rawMessage;
		}
		return message;
	}

	protected void afterMessageProcessed(Connection c, PipeLineResult processResult, String key, Map context) throws ListenerException {
		if (processResult==null || "success".equals(processResult.getState()) || StringUtils.isEmpty(getUpdateStatusToErrorQuery())) {
			execute(c,getUpdateStatusToProcessedQuery(),key);
		} else {
			execute(c,getUpdateStatusToErrorQuery(),key);
		}
	}

	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
		String key=getIdFromRawMessage(rawMessage,context);
		if (isConnectionsArePooled()) {
			Connection c = null;
			try {
//				log.debug("getting connection");
				c = getConnection();
				afterMessageProcessed(c,processResult, key, context);
			} catch (JdbcException e) {
				throw new ListenerException(e);
			} finally {
				if (c!=null) {
					try {
//						log.debug("closing connection");
						c.close();
					} catch (SQLException e) {
						log.warn(new ListenerException(getLogPrefix() + "caught exception closing connection in afterMessageProcessed()", e));
					}
				}
			}
		} else {
			synchronized (connection) {
				afterMessageProcessed(connection,processResult, key, context);
			}
		}
	}
	
	protected ResultSet executeQuery(Connection conn, String query) throws ListenerException {
		if (StringUtils.isEmpty(query)) {
			throw new ListenerException(getLogPrefix()+"cannot execute empty query");
		}
		if (trace && log.isDebugEnabled()) log.debug("executing query ["+query+"]");
		Statement stmt=null;
		try {
			stmt = conn.createStatement();
			return stmt.executeQuery(query);
		} catch (SQLException e) {
			if (stmt!=null) {
				try {
//					log.debug("closing statement for ["+query+"]");
					stmt.close();
				} catch (Throwable t) {
					log.warn(getLogPrefix()+"exception closing statement ["+query+"]", t);
				}
			}
			throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
		}
	}

	protected void execute(Connection conn, String query) throws ListenerException {
		execute(conn,query,null);
	}

	protected void execute(Connection conn, String query, String parameter) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			if (trace && log.isDebugEnabled()) log.debug("executing statement ["+query+"]");
			PreparedStatement stmt=null;
			try {
				stmt = conn.prepareStatement(query);
				stmt.clearParameters();
				if (StringUtils.isNotEmpty(parameter)) {
					log.debug("setting parameter 1 to ["+parameter+"]");
					stmt.setString(1,parameter);
				}
				stmt.execute();
				
			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
			} finally {
				if (stmt!=null) {
					try {
//						log.debug("closing statement for ["+query+"]");
						stmt.close();
					} catch (SQLException e) {
						log.warn(getLogPrefix()+"exception closing statement ["+query+"]",e);
					}
				}
			}
		}
	}

	protected void setSelectQuery(String string) {
		selectQuery = string;
	}
	public String getSelectQuery() {
		return selectQuery;
	}


	protected void setUpdateStatusToErrorQuery(String string) {
		updateStatusToErrorQuery = string;
	}
	public String getUpdateStatusToErrorQuery() {
		return updateStatusToErrorQuery;
	}

	protected void setUpdateStatusToProcessedQuery(String string) {
		updateStatusToProcessedQuery = string;
	}
	public String getUpdateStatusToProcessedQuery() {
		return updateStatusToProcessedQuery;
	}


	protected void setKeyField(String fieldname) {
		keyField = fieldname;
	}
	public String getKeyField() {
		return keyField;
	}
	protected void setMessageField(String fieldname) {
		messageField = fieldname;
	}
	public String getMessageField() {
		return messageField;
	}

	public void setStartLocalTransactionQuery(String string) {
		startLocalTransactionQuery = string;
	}
	public String getStartLocalTransactionQuery() {
		return startLocalTransactionQuery;
	}

	public void setCommitLocalTransactionQuery(String string) {
		commitLocalTransactionQuery = string;
	}
	public String getCommitLocalTransactionQuery() {
		return commitLocalTransactionQuery;
	}

	public void setMessageFieldType(String string) {
		messageFieldType = string;
	}
	public String getMessageFieldType() {
		return messageFieldType;
	}

	public void setBlobCharset(String string) {
		blobCharset = string;
	}
	public String getBlobCharset() {
		return blobCharset;
	}

	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	public void setBlobSmartGet(boolean b) {
		blobSmartGet = b;
	}
	public boolean isBlobSmartGet() {
		return blobSmartGet;
	}

}