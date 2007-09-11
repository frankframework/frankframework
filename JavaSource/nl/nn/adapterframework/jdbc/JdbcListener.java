/*
 * $Log: JdbcListener.java,v $
 * Revision 1.1  2007-09-11 11:53:01  europe\L190409
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

import javax.transaction.UserTransaction;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.JtaUtil;

import org.apache.commons.lang.StringUtils;

/**
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class JdbcListener extends JdbcFacade implements IPullingListener {

	public static final String KEY_FIELD_KEY="KeyField";
	public static final String MESSAGE_FIELD_KEY="MessageField";

	private String lockQuery;
	private String unlockQuery;
	private String selectQuery;
	private String updateStatusToInProcessQuery;
	private String updateStatusToProcessedQuery;
	private String updateStatusToErrorQuery;

	private String keyField;
	private String messageField;
	
	protected Connection connection=null;

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
	
	public HashMap openThread() throws ListenerException {
		return new HashMap();
	}

	public void closeThread(HashMap threadContext) throws ListenerException {
	}

	public Object getRawMessage(HashMap threadContext) throws ListenerException {
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

	protected Object getRawMessage(Connection conn, HashMap threadContext) throws ListenerException {
		try {
			execute(conn,getLockQuery());
			UserTransaction utx = JtaUtil.getUserTransaction();
			utx.begin();
			try {
				ResultSet rs=null;
				try {
					rs = executeQuery(conn,getSelectQuery());
					String key=rs.getString(getKeyField());
					threadContext.put(KEY_FIELD_KEY,key);
					threadContext.put(MESSAGE_FIELD_KEY,rs.getString(getMessageField()));
					execute(conn,getUpdateStatusToInProcessQuery(),key);
					utx.commit();
					return key;
				} catch (SQLException e) {
					throw new ListenerException(getLogPrefix() + "caught exception retrieving message", e);
				} finally {
					JdbcUtil.fullClose(rs);
				}
			} finally {
				if (JtaUtil.inTransaction(utx)) {
					utx.rollback();
				}
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "cannot manage UserTransaction", e);
		} finally {
			execute(conn,getUnlockQuery());
		}
	}

	public String getIdFromRawMessage(Object rawMessage, HashMap context) throws ListenerException {
		String id = (String)context.get(KEY_FIELD_KEY);
		return id;
	}

	public String getStringFromRawMessage(Object rawMessage, HashMap context) throws ListenerException {
		String message = (String)context.get(MESSAGE_FIELD_KEY);
		return message;
	}

	public void afterMessageProcessed(Connection c, PipeLineResult processResult, HashMap context) throws ListenerException {
		String key=(String)context.get(KEY_FIELD_KEY);
		if (processResult==null || "success".equals(processResult.getState())) {
			execute(c,getUpdateStatusToProcessedQuery());
		} else {
			execute(c,getUpdateStatusToErrorQuery());
		}
	}

	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap context) throws ListenerException {
		if (isConnectionsArePooled()) {
			Connection c = null;
			try {
				c = getConnection();
				afterMessageProcessed(c,processResult, context);
			} catch (JdbcException e) {
				throw new ListenerException(e);
			} finally {
				if (c!=null) {
					try {
						c.close();
					} catch (SQLException e) {
						log.warn(new ListenerException(getLogPrefix() + "caught exception closing connection in afterMessageProcessed()", e));
					}
				}
			}
		} else {
			synchronized (connection) {
				afterMessageProcessed(connection,processResult, context);
			}
		}
	}
	
	protected ResultSet executeQuery(Connection conn, String query) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			throw new ListenerException(getLogPrefix()+"cannot execute empty query");
		}
		Statement stmt=null;
		try {
			stmt = conn.createStatement();
			return stmt.executeQuery(query);
		} catch (SQLException e) {
			throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					log.warn(getLogPrefix()+"exception closing statement ["+query+"]",e);
				}
			}
		}
	}

	protected void execute(Connection conn, String query) throws ListenerException {
		execute(conn,query,null);
	}

	protected void execute(Connection conn, String query, String parameter) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			PreparedStatement stmt=null;
			try {
				stmt = conn.prepareStatement(query);
				stmt.clearParameters();
				if (StringUtils.isNotEmpty(parameter)) {
					stmt.setString(1,parameter);
				}
				stmt.execute(query);
				
			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
			} finally {
				if (stmt!=null) {
					try {
						stmt.close();
					} catch (SQLException e) {
						log.warn(getLogPrefix()+"exception closing statement ["+query+"]",e);
					}
				}
			}
		}
	}

	protected void setLockQuery(String string) {
		lockQuery = string;
	}
	public String getLockQuery() {
		return lockQuery;
	}

	protected void setUnlockQuery(String string) {
		unlockQuery = string;
	}
	public String getUnlockQuery() {
		return unlockQuery;
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
	public String getUpdateStatusToInProcessQuery() {
		return updateStatusToInProcessQuery;
	}

	protected void setUpdateStatusToInProcessQuery(String string) {
		updateStatusToInProcessQuery = string;
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
}
