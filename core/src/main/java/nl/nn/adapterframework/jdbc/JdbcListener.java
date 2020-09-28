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
import nl.nn.adapterframework.core.IUsesInProcessState;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * JdbcListener base class.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class JdbcListener extends JdbcFacade implements IPeekableListener<Object>, IUsesInProcessState<Object> {

	private String selectQuery;
	private String peekQuery;
	private String updateStatusToInProcessQuery;
	private String updateStatusToProcessedQuery;
	private String updateStatusToErrorQuery;
	private String revertInProcessStatusQuery;

	private String keyField;
	private String messageField;
	private String messageFieldType="String";

	private String blobCharset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private boolean blobsCompressed=true;
	private boolean blobSmartGet=false;
	
	private boolean trace=false;
	private boolean peekUntransacted=true;

	protected Connection connection=null;

	private String preparedSelectQuery;
	private String preparedPeekQuery;

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
			preparedPeekQuery = StringUtils.isNotEmpty(getPeekQuery()) ? getPeekQuery() : getDbmsSupport().prepareQueryTextForWorkQueuePeeking(1, getSelectQuery());
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
	public Map<String,Object> openThread() throws ListenerException {
		return new HashMap<>();
	}

	@Override
	public void closeThread(Map<String,Object> threadContext) throws ListenerException {
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
							message=new Message(JdbcUtil.getBlobAsString(getDbmsSupport(), rs,getMessageField(),getBlobCharset(),false,isBlobsCompressed(),isBlobSmartGet(),false)); // TODO: should not convert Blob to String, but keep as byte array
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
		if (processResult.isSuccessful() || StringUtils.isEmpty(getUpdateStatusToErrorQuery())) {
			execute(conn, getUpdateStatusToProcessedQuery(), key);
		} else {
			execute(conn, getUpdateStatusToErrorQuery(), key);
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
	public boolean setMessageStateToInProcess(Object rawMessage, Map<String, Object> threadContext) throws ListenerException {
		if (StringUtils.isEmpty(getUpdateStatusToInProcessQuery())) {
			return false;
		} 
		return doConnected(rawMessage, threadContext, setMessageStateToInProcess);
	}

	public ConnectedOperation setMessageStateToInProcess = (Connection conn, Object rawMessage, Map<String, Object> threadContext) -> {
		if (StringUtils.isEmpty(getUpdateStatusToInProcessQuery())) {
			return false;
		}
		String key = getIdFromRawMessage(rawMessage, threadContext);
		execute(conn,getUpdateStatusToInProcessQuery(), key);
		return true;
	};


	@Override
	public void revertInProcessStatusToAvailable(Object rawMessage, Map<String, Object> threadContext) throws ListenerException {
		if (StringUtils.isEmpty(getRevertInProcessStatusQuery())) {
			return;
		}
		doConnected(rawMessage, threadContext, revertInProcessStatusToAvailable);	
	}

	protected ConnectedOperation revertInProcessStatusToAvailable = (Connection conn, Object rawMessage, Map<String, Object> threadContext) -> {
		if (StringUtils.isEmpty(getRevertInProcessStatusQuery())) {
			return false;
		}
		String key=getIdFromRawMessage(rawMessage,threadContext);
		execute(conn,getRevertInProcessStatusQuery(), key);
		return true;
	};

	protected interface ConnectedOperation {
		boolean operate(Connection conn, Object rawMessage, Map<String, Object> threadContext) throws ListenerException;
	}
	
	protected boolean doConnected(Object rawMessage, Map<String, Object> threadContext, ConnectedOperation operation) throws ListenerException {
		if (isConnectionsArePooled()) {
			try (Connection c = getConnection()) {
				return operation.operate(c, rawMessage, threadContext);
			} catch (JdbcException|SQLException e) {
				throw new ListenerException(e);
			}
		}
		synchronized (connection) {
			return operation.operate(connection, rawMessage, threadContext);
		}
	}
	


	protected void execute(Connection conn, String query) throws ListenerException {
		execute(conn,query,null);
	}

	protected void execute(Connection conn, String query, String parameter) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			if (trace && log.isDebugEnabled()) log.debug("executing statement ["+query+"]");
			try (PreparedStatement stmt=conn.prepareStatement(query)) {
				stmt.clearParameters();
				if (StringUtils.isNotEmpty(parameter)) {
					log.debug("setting parameter 1 to ["+parameter+"]");
					JdbcUtil.setParameter(stmt, 1, parameter, getDbmsSupport().isParameterTypeMatchRequired());
				}
				stmt.execute();
				
			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
			}
		}
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

	protected void setUpdateStatusToInProcessQuery(String string) {
		updateStatusToInProcessQuery = string;
	}
	public String getUpdateStatusToInProcessQuery() {
		return updateStatusToInProcessQuery;
	}

	protected void setRevertInProcessStatusQuery(String string) {
		revertInProcessStatusQuery = string;
	}
	public String getRevertInProcessStatusQuery() {
		return revertInProcessStatusQuery;
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

	@IbisDoc({"Type of the field containing the message data: either String, clob or blob", "<i>String</i>"})
	public void setMessageFieldType(String string) {
		messageFieldType = string;
	}
	public String getMessageFieldType() {
		return messageFieldType;
	}

	@IbisDoc({"Charset used to read blobs", "UTF-8"})
	public void setBlobCharset(String string) {
		blobCharset = string;
	}
	public String getBlobCharset() {
		return blobCharset;
	}

	@IbisDoc({"Controls whether blobdata is considered stored compressed in the database", "true"})
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	@IbisDoc({"Controls automatically whether blobdata is stored compressed and/or serialized in the database", "false"})
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