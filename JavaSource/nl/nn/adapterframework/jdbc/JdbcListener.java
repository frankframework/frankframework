/*
 * $Log: JdbcListener.java,v $
 * Revision 1.10  2009-03-26 14:47:36  m168309
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
import nl.nn.adapterframework.util.JtaUtil;

import org.apache.commons.lang.StringUtils;

/**
 * JdbcListener base class.
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
	
	protected Connection connection=null;

	private static final boolean trace=false;

	public void configure() throws ConfigurationException {
		try {
			if (getDatasource()==null) {
				throw new ConfigurationException(getLogPrefix()+"has no datasource");
			}
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

			/*
			 * see:
			 * http://www.psoug.org/reference/deadlocks.html
			 * http://www.psoug.org/reference/select.html
			 * http://www.ss64.com/ora/select.html
			 * http://forums.oracle.com/forums/thread.jspa?threadID=664986
			 */
			String query=getSelectQuery()+JdbcFacade.LOCKROWS_SUFFIX;
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
							String message=rs.getString(getMessageField());
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

}