/*
   Copyright 2013, 2016, 2018-2020 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IHasProcessState;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.IPeekableListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * JdbcListener base class.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class JdbcListener extends JdbcFacade implements IPeekableListener<Object>, IHasProcessState<Object> {

	private String selectQuery;
	private String peekQuery;

	private String keyField;
	private String messageField;
	private String messageFieldType="String";

	private String blobCharset = null;
	private boolean blobsCompressed=true;
	private boolean blobSmartGet=false;
	
	private boolean trace=false;
	private boolean peekUntransacted=true;

	private Map<ProcessState, String> updateStatusQueries = new HashMap<>();
	private Map<ProcessState,Set<ProcessState>> targetProcessStates = new HashMap<>();

	protected Connection connection=null;

	private String preparedSelectQuery;
	private String preparedPeekQuery;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			preparedSelectQuery = getDbmsSupport().prepareQueryTextForWorkQueueReading(1, getSelectQuery());
			preparedPeekQuery = StringUtils.isNotEmpty(getPeekQuery()) ? getPeekQuery() : getDbmsSupport().prepareQueryTextForWorkQueuePeeking(1, getSelectQuery());
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
		Map<ProcessState, String> orderedUpdateStatusQueries = new LinkedHashMap<>();
		for (ProcessState state : ProcessState.values()) {
			if(updateStatusQueries.containsKey(state)) {
				orderedUpdateStatusQueries.put(state, updateStatusQueries.get(state));
			}
		}
		updateStatusQueries=orderedUpdateStatusQueries;
		targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates());
	}

	@Override
	public void open() throws ListenerException {
		if (!isConnectionsArePooled()) {
			try {
				connection = getConnection();
			} catch (JdbcException e) {
				throw new ListenerException(e);
			}
		} else {
			try (Connection c = getConnection()) {
				//do nothing, eat a connection from the pool to validate connectivity
			} catch (JdbcException|SQLException e) {
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
	public Map<String,Object> openThread() throws ListenerException {
		return new HashMap<>();
	}

	@Override
	public void closeThread(Map<String,Object> threadContext) throws ListenerException {
		// nothing special
	}

	@Override
	public boolean hasRawMessageAvailable() throws ListenerException {
		if (StringUtils.isEmpty(preparedPeekQuery)) {
			return true;
		} else {
			if (isConnectionsArePooled()) {
				try (Connection c = getConnection()) {
					return hasRawMessageAvailable(c);
				} catch (JdbcException|SQLException e) {
					throw new ListenerException(e);
				}
			}
			synchronized (connection) {
				return hasRawMessageAvailable(connection);
			}
		}
	}

	protected boolean hasRawMessageAvailable(Connection conn) throws ListenerException {
		try {
			try (JdbcSession session = getDbmsSupport().prepareSessionForNonLockingRead(conn)) {
				return !JdbcUtil.isQueryResultEmpty(conn, preparedPeekQuery);
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message trigger using query [" + preparedPeekQuery + "]", e);
		}
	}
	
	@Override
	public Object getRawMessage(Map<String,Object> threadContext) throws ListenerException {
		if (isConnectionsArePooled()) {
			try (Connection c = getConnection()) {
				return getRawMessage(c,threadContext);
			} catch (JdbcException | SQLException e) {
				throw new ListenerException(e);
			}
		} 
		synchronized (connection) {
			return getRawMessage(connection,threadContext);
		}
	}

	protected Object getRawMessage(Connection conn, Map<String,Object> threadContext) throws ListenerException {
		String query=preparedSelectQuery;
		try (Statement stmt= conn.createStatement()) {
			stmt.setFetchSize(1);
			if (trace && log.isDebugEnabled()) log.debug("executing query for ["+query+"]");
			try (ResultSet rs=stmt.executeQuery(query)) {
				if (!rs.next()) {
					return null;
				}
				Object result;
				String key=rs.getString(getKeyField());

				if (StringUtils.isNotEmpty(getMessageField())) {
					Message message;
					if ("clob".equalsIgnoreCase(getMessageFieldType())) {
						message=new Message(JdbcUtil.getClobAsString(getDbmsSupport(), rs,getMessageField(),false));
					} else {
						if ("blob".equalsIgnoreCase(getMessageFieldType())) {
							if (isBlobSmartGet() || StringUtils.isNotEmpty(getBlobCharset())) {
								message=new Message(JdbcUtil.getBlobAsString(getDbmsSupport(), rs,getMessageField(),getBlobCharset(),isBlobsCompressed(),isBlobSmartGet(),false));
							} else {
								try (InputStream blobStream = JdbcUtil.getBlobInputStream(getDbmsSupport(), rs, getMessageField(), isBlobsCompressed())) {
									message=new Message(blobStream);
									message.preserve();
								}
							}
						} else {
							message=new Message(rs.getString(getMessageField()));
						}
					}
					// log.debug("building wrapper for key ["+key+"], message ["+message+"]");
					MessageWrapper<?> mw = new MessageWrapper<Object>();
					mw.setId(key);
					mw.setMessage(message);
					result=mw;
				} else {
					result = key;
				}
				return result;
			} catch (SQLException e) {
				if (!getDbmsSupport().hasSkipLockedFunctionality()) {
					String errorMessage = e.getMessage();
					if (errorMessage.toLowerCase().contains("timeout") && errorMessage.toLowerCase().contains("lock")) {
						log.debug(getLogPrefix()+"caught lock timeout exception, returning null: ("+e.getClass().getName()+")"+e.getMessage());
						return null; // resolve locking conflict for dbmses that do not support SKIP LOCKED
					}
				}
				throw e;
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message using query ["+query+"]", e);
		}
	}

	@Override
	public String getIdFromRawMessage(Object rawMessage, Map<String,Object> context) throws ListenerException {
		String id;
		if (rawMessage instanceof IMessageWrapper) {
			id = ((IMessageWrapper)rawMessage).getId();
		} else {
			id = (String)rawMessage;
		}
		if (context!=null) {
			PipeLineSessionBase.setListenerParameters(context, id, id, null, null);
		}
		return id;
	}

	@Override
	public Message extractMessage(Object rawMessage, Map<String,Object> context) throws ListenerException {
		Message message;
		if (rawMessage instanceof IMessageWrapper) {
			message = ((IMessageWrapper)rawMessage).getMessage();
		} else {
			message = Message.asMessage(rawMessage);
		}
		return message;
	}

	protected void afterMessageProcessed(Connection conn, PipeLineResult processResult, String key, Map<String,Object> context) throws ListenerException {
		if (processResult.isSuccessful() || StringUtils.isEmpty(getUpdateStatusQuery(ProcessState.ERROR))) {
			execute(conn, getUpdateStatusQuery(ProcessState.DONE), key);
		} else {
			execute(conn, getUpdateStatusQuery(ProcessState.ERROR), key);
		}
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map<String,Object> context) throws ListenerException {
		String key=getIdFromRawMessage(rawMessage,context);
		if (isConnectionsArePooled()) {
			try (Connection c = getConnection()) {
				afterMessageProcessed(c,processResult, key, context);
			} catch (JdbcException|SQLException e) {
				throw new ListenerException(e);
			}
		} else {
			synchronized (connection) {
				afterMessageProcessed(connection,processResult, key, context);
			}
		}
	}

	@Override
	public Set<ProcessState> knownProcessStates() {
		return updateStatusQueries.keySet();
	}

	@Override
	public Map<ProcessState,Set<ProcessState>> targetProcessStates() {
		return targetProcessStates;
	}

	@Override
	public Object changeProcessState(Object rawMessage, ProcessState toState) throws ListenerException {
		if (!knownProcessStates().contains(toState)) {
			return null;
		}
		if (isConnectionsArePooled()) {
			try (Connection conn = getConnection()) {
				return changeProcessState(conn, rawMessage, toState);
			} catch (JdbcException|SQLException e) {
				throw new ListenerException(e);
			}
		} else {
			synchronized (connection) {
				return changeProcessState(connection, rawMessage, toState);
			}
		}
	}

	public Object changeProcessState(Connection connection, Object rawMessage, ProcessState toState) throws ListenerException {
		if (!knownProcessStates().contains(toState)) {
			return null;
		}
		String query = getUpdateStatusQuery(toState);
		String key=getIdFromRawMessage(rawMessage, null);
		return execute(connection, query, key) ? rawMessage : null;
	}

	protected boolean execute(Connection conn, String query) throws ListenerException {
		return execute(conn,query,null);
	}

	protected boolean execute(Connection conn, String query, String parameter) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			if (trace && log.isDebugEnabled()) log.debug("executing statement ["+query+"]");
			try (PreparedStatement stmt=conn.prepareStatement(query)) {
				stmt.clearParameters();
				if (StringUtils.isNotEmpty(parameter)) {
					log.debug("setting parameter 1 to ["+parameter+"]");
					JdbcUtil.setParameter(stmt, 1, parameter, getDbmsSupport().isParameterTypeMatchRequired());
				}

				return stmt.executeUpdate() > 0;
			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
			}
		}
		return false;
	}

	protected void setUpdateStatusQuery(ProcessState state, String query) {
		if (StringUtils.isNotEmpty(query)) {
			updateStatusQueries.put(state, query);
		} else {
			updateStatusQueries.remove(state);
		}
	}
	public String getUpdateStatusQuery(ProcessState state) {
		return updateStatusQueries.get(state);
	}

	protected void setSelectQuery(String string) {
		selectQuery = string;
	}
	public String getSelectQuery() {
		return selectQuery;
	}

	@Override
	public void setPeekUntransacted(boolean b) {
		peekUntransacted = b;
	}
	@Override
	public boolean isPeekUntransacted() {
		return peekUntransacted;
	}

	@IbisDoc({"(only used when <code>peekUntransacted=true</code>) peek query to determine if the select query should be executed. Peek queries are, unlike select queries, executed without a transaction and without a rowlock", "selectQuery"})
	public void setPeekQuery(String string) {
		peekQuery = string;
	}
	public String getPeekQuery() {
		return peekQuery;
	}


	@IbisDoc({"1", "Primary key field of the table, used to identify messages", ""})
	public void setKeyField(String fieldname) {
		keyField = fieldname;
	}
	public String getKeyField() {
		return keyField;
	}

	@IbisDoc({"2", "(Optional) field containing the message data", "<i>same as keyField</i>"})
	public void setMessageField(String fieldname) {
		messageField = fieldname;
	}
	public String getMessageField() {
		return messageField;
	}

	@IbisDoc({"3", "Type of the field containing the message data: either String, clob or blob", "<i>String</i>"})
	public void setMessageFieldType(String string) {
		messageFieldType = string;
	}
	public String getMessageFieldType() {
		return messageFieldType;
	}

	@IbisDoc({"4", "Controls whether BLOB is considered stored compressed in the database", "true"})
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	@IbisDoc({"5", "Charset used to read BLOB. When specified, then the BLOB will be converted into a string", ""})
	@Deprecated
	public void setBlobCharset(String string) {
		blobCharset = string;
	}
	public String getBlobCharset() {
		return blobCharset;
	}

	@IbisDoc({"6", "Controls automatically whether blobdata is stored compressed and/or serialized in the database. N.B. When set true, then the BLOB will be converted into a string", "false"})
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

}