/*
   Copyright 2013, 2016, 2018-2020 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.IPeekableListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * JdbcListener base class.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class JdbcListener extends JdbcFacade implements IPeekableListener {

	private String startLocalTransactionQuery;
	private String commitLocalTransactionQuery;
	private String selectQuery;
	private String peekQuery;
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

	private boolean trace=false;
	private boolean peekUntransacted=false;

	@Override
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

	@Override
	public void open() throws ListenerException {
		if (!isConnectionsArePooled()) {
			try {
				connection = getConnection();
			} catch (JdbcException e) {
				throw new ListenerException(e);
			}
		}
	}

	@Override
	public void close() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			log.warn(getLogPrefix() + "caught exception stopping listener", e);
		} finally {
			connection = null;
			super.close();
		}
	}
	
	@Override
	public Map openThread() throws ListenerException {
		return new HashMap();
	}

	@Override
	public void closeThread(Map threadContext) throws ListenerException {
	}

	@Override
	public boolean hasRawMessageAvailable() throws ListenerException {
		if (StringUtils.isEmpty(getPeekQuery())) {
			return true;
		} else {
			if (isConnectionsArePooled()) {
				Connection c = null;
				try {
					c = getConnection();
					return hasRawMessageAvailable(c);
				} catch (JdbcException e) {
					throw new ListenerException(e);
				} finally {
					if (c != null) {
						try {
							c.close();
						} catch (SQLException e) {
							log.warn(new ListenerException(getLogPrefix() + "caught exception closing listener after retrieving message trigger", e));
						}
					}
				}
			}
			synchronized (connection) {
				return hasRawMessageAvailable(connection);
			}
		}
	}

	protected boolean hasRawMessageAvailable(Connection conn) throws ListenerException {
		try {
			return !JdbcUtil.isQueryResultEmpty(conn, getPeekQuery());
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message trigger using query [" + getPeekQuery() + "]", e);
		}
	}
	
	@Override
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
			
		} 
		synchronized (connection) {
			return getRawMessage(connection,threadContext);
		}
	}

	protected Object getRawMessage(Connection conn, Map<String,Object> threadContext) throws ListenerException {
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
						if (!rs.next()) {
							return null;
						}
						Object result;
						String key=rs.getString(getKeyField());
						
						if (StringUtils.isNotEmpty(getMessageField())) {
							Message message;
							if ("clob".equalsIgnoreCase(getMessageFieldType())) {
								message=new Message(JdbcUtil.getClobAsString(rs,getMessageField(),false));
							} else {
								if ("blob".equalsIgnoreCase(getMessageFieldType())) {
									message=new Message(JdbcUtil.getBlobAsString(rs,getMessageField(),getBlobCharset(),false,isBlobsCompressed(),isBlobSmartGet(),false)); // TODO: should not convert Blob to String, but keep as byte array
								} else {
									message=new Message(rs.getString(getMessageField()));
								}
							}
							// log.debug("building wrapper for key ["+key+"], message ["+message+"]");
							MessageWrapper mw = new MessageWrapper();
							mw.setId(key);
							mw.setMessage(message);
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

	@Override
	public String getIdFromRawMessage(Object rawMessage, Map context) throws ListenerException {
		String id;
		if (rawMessage instanceof IMessageWrapper) {
			id = ((IMessageWrapper)rawMessage).getId();
		} else {
			id = (String)rawMessage;
		}
		PipeLineSessionBase.setListenerParameters(context, id, id, null, null);
		return id;
	}

	@Override
	public Message extractMessage(Object rawMessage, Map context) throws ListenerException {
		Message message;
		if (rawMessage instanceof IMessageWrapper) {
			message = ((IMessageWrapper)rawMessage).getMessage();
		} else {
			message = Message.asMessage(rawMessage);
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

	@Override
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
		if (peekQuery==null) {
			peekQuery = selectQuery;
		}
	}
	public String getSelectQuery() {
		return selectQuery;
	}

	@IbisDoc({"(only used when <code>peekUntransacted=true</code>) peek query to determine if the select query should be executed. Peek queries are, unlike select queries, executed without a transaction and without a rowlock", "selectQuery"})
	protected void setPeekQuery(String string) {
		peekQuery = string;
	}
	public String getPeekQuery() {
		return peekQuery;
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

	@IbisDoc({"primary key field of the table, used to identify messages", ""})
	protected void setKeyField(String fieldname) {
		keyField = fieldname;
	}
	public String getKeyField() {
		return keyField;
	}

	@IbisDoc({"(optional) field containing the message data", "<i>same as keyfield</i>"})
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

	@IbisDoc({"type of the field containing the message data: either string, clob or blob", "<i>string</i>"})
	public void setMessageFieldType(String string) {
		messageFieldType = string;
	}
	public String getMessageFieldType() {
		return messageFieldType;
	}

	@IbisDoc({"charset used to read blobs", "utf-8"})
	public void setBlobCharset(String string) {
		blobCharset = string;
	}
	public String getBlobCharset() {
		return blobCharset;
	}

	@IbisDoc({"controls whether blobdata is considered stored compressed in the database", "true"})
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	@IbisDoc({"controls automatically whether blobdata is stored compressed and/or serialized in the database", "false"})
	public void setBlobSmartGet(boolean b) {
		blobSmartGet = b;
	}
	public boolean isBlobSmartGet() {
		return blobSmartGet;
	}

	public boolean isTrace() {
		return trace;
	}
	public void setTrace(boolean trace) {
		this.trace = trace;
	}

	@Override
	public boolean isPeekUntransacted() {
		return peekUntransacted;
	}

	@Override
	public void setPeekUntransacted(boolean b) {
		peekUntransacted = b;
	}
}