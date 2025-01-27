/*
   Copyright 2013, 2016, 2018-2020 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IHasProcessState;
import org.frankframework.core.IPeekableListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.ProcessState;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.JdbcUtil;

/**
 * JdbcListener base class.
 *
 * @param <M> MessageWrapper or key. Key is also used as messageId
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class JdbcListener<M> extends JdbcFacade implements IPeekableListener<M>, IHasProcessState<M> {

	private @Getter String selectQuery;
	private @Getter String peekQuery;

	private @Getter String keyField;
	private @Getter String messageField;
	private @Getter String messageIdField;
	private @Getter String correlationIdField;
	private @Getter MessageFieldType messageFieldType=MessageFieldType.STRING;
	private @Getter String sqlDialect = AppConstants.getInstance().getString("jdbc.sqlDialect", null);

	private @Getter String blobCharset = null;
	private @Getter boolean blobsCompressed=true;
	private @Getter boolean blobSmartGet=false;

	private @Setter @Getter boolean trace=false;
	private @Getter boolean peekUntransacted=true;

	private Map<ProcessState, String> updateStatusQueries = new HashMap<>();
	private Map<ProcessState,Set<ProcessState>> targetProcessStates = new HashMap<>();

	protected Connection connection=null;

	private String preparedSelectQuery;
	private String preparedPeekQuery;

	public enum MessageFieldType {
		STRING,
		CLOB,
		BLOB
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			String convertedSelectQuery = convertQuery(getSelectQuery());
			preparedSelectQuery = getDbmsSupport().prepareQueryTextForWorkQueueReading(1, convertedSelectQuery);
			preparedPeekQuery = StringUtils.isNotEmpty(getPeekQuery()) ? convertQuery(getPeekQuery()) : getDbmsSupport().prepareQueryTextForWorkQueuePeeking(1, convertedSelectQuery);
			Map<ProcessState, String> orderedUpdateStatusQueries = new LinkedHashMap<>();
			for (ProcessState state : ProcessState.values()) {
				if(updateStatusQueries.containsKey(state)) {
					String convertedUpdateStatusQuery = convertQuery(updateStatusQueries.get(state));
					orderedUpdateStatusQueries.put(state, convertedUpdateStatusQuery);
				}
			}
			updateStatusQueries=orderedUpdateStatusQueries;
			targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates());
		} catch (JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public void start() {
		if (!isConnectionsArePooled()) {
			try {
				connection = getConnection();
			} catch (JdbcException e) {
				throw new LifecycleException(e);
			}
		} else {
			try (Connection c = getConnection()) {
				//do nothing, eat a connection from the pool to validate connectivity
			} catch (JdbcException | SQLException e) {
				throw new LifecycleException(e);
			}
		}
	}

	@Override
	public void stop() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			log.warn("{}caught exception stopping listener", getLogPrefix(), e);
		} finally {
			connection = null;
			super.stop();
		}
	}

	@Nonnull
	@Override
	public Map<String,Object> openThread() throws ListenerException {
		return new HashMap<>();
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		// nothing special
	}

	@Override
	public boolean hasRawMessageAvailable() throws ListenerException {
		if (StringUtils.isEmpty(preparedPeekQuery)) {
			return true;
		}
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

	protected boolean hasRawMessageAvailable(Connection conn) throws ListenerException {
		try {
			return !JdbcUtil.isQueryResultEmpty(conn, preparedPeekQuery);
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message trigger using query [" + preparedPeekQuery + "]", e);
		}
	}

	@Override
	public RawMessageWrapper<M> getRawMessage(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		if (isConnectionsArePooled()) {
			try (Connection c = getConnection()) {
				return getRawMessage(c, threadContext);
			} catch (JdbcException | SQLException e) {
				throw new ListenerException(e);
			}
		}
		synchronized (connection) {
			return getRawMessage(connection, threadContext);
		}
	}

	protected RawMessageWrapper<M> getRawMessage(Connection conn, Map<String,Object> threadContext) throws ListenerException {
		String query = preparedSelectQuery;
		try (Statement stmt = conn.createStatement()) {
			stmt.setFetchSize(1);
			if (trace && log.isDebugEnabled()) log.debug("executing query for [{}]", query);
			try (ResultSet rs=stmt.executeQuery(query)) {
				if (!rs.next()) {
					return null;
				}
				return extractRawMessage(rs);
			} catch (SQLException e) {
				if (!getDbmsSupport().hasSkipLockedFunctionality()) {
					String errorMessage = e.getMessage();
					if (errorMessage.toLowerCase().contains("timeout") && errorMessage.toLowerCase().contains("lock")) {
						log.debug("{}caught lock timeout exception, returning null: ({}){}", getLogPrefix(), e.getClass().getName(), e.getMessage());
						return null; // resolve locking conflict for dbmses that do not support SKIP LOCKED
					}
				}
				throw e;
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message using query ["+query+"]", e);
		}
	}

	/**
	 * Get column value from {@link ResultSet}, or the default if either the column-name is empty (unconfigured) or if
	 * the result-set does not contain a column of this name.
	 *
	 * @param rs The {@link ResultSet} from which to get the column.
	 * @param columnName The name of the column, can be {@code null} or empty.
	 * @param defaultValue Default value for the column if column name was empty, or not present in the {@code ResultSet}. Can be {@code null}.
	 * @return Value from the {@code ResultSet}, or the default.
	 * @throws SQLException Propagates the {@link SQLException} which may be thrown from the {@link ResultSet}.
	 */
	private String getColumnValueOrDefault(ResultSet rs, String columnName, String defaultValue) throws SQLException {
		if (StringUtils.isEmpty(columnName)) {
			return defaultValue;
		}
		int index;
		try {
			index = rs.findColumn(columnName);
		} catch (SQLException e) {
			// Assume the cause of exception is that the column does not exist in this ResultSet and return default
			return defaultValue;
		}
		return rs.getString(index);
	}

	/**
	 * This method returns a {@link MessageWrapper} containing contents of the message stored in the database.
	 *
	 * @param rs JDBC {@link ResultSet} from which to extract message data.
	 * @return Either a {@link String} being the message key, or a {@link MessageWrapper}.
	 * The message key as {@link String} is returned if {@link #messageField}, {@link #messageIdField} and {@link #correlationIdField} all are not
	 * set.
	 * If {@link #messageIdField} and / or {@link  #correlationIdField} are set but {@link #messageField} is not, then the
	 * message key is returned as value of a {@link Message} wrapped in a {@link MessageWrapper}.
	 * Otherwise the message is loaded from the {@code rs} parameter and returned wrapped in a {@link MessageWrapper}.
	 * @throws JdbcException If loading the message resulted in a database exception.
	 */
	protected RawMessageWrapper<M> extractRawMessage(ResultSet rs) throws JdbcException {
		// TODO: This needs to be reviewed, if all complications are needed. Some branches are never touched in tests.
		try {
			String key = rs.getString(getKeyField());
			Message message;
			if (StringUtils.isNotEmpty(getMessageField())) {
				switch (getMessageFieldType()) {
					case CLOB:
						// TESTCOVERAGE: Untested branch
						message = new Message(getDbmsSupport().getClobReader(rs, getMessageField()));
						break;
					case BLOB:
						if (isBlobSmartGet() || StringUtils.isNotEmpty(getBlobCharset())) { // in this case blob contains a String
							message = new Message(JdbcUtil.getBlobAsString(getDbmsSupport(), rs,getMessageField(), getBlobCharset(), isBlobsCompressed(), isBlobSmartGet(),false));
						} else {
							// TESTCOVERAGE: Untested branch
							try (InputStream blobStream = JdbcUtil.getBlobInputStream(getDbmsSupport(), rs, getMessageField(), isBlobsCompressed())) {
								message = new Message(blobStream);
								message.preserve();
							}
						}
						break;
					case STRING:
						message = new Message(rs.getString(getMessageField()));
						break;
					default:
						throw new IllegalArgumentException("Illegal messageFieldType [" + getMessageFieldType() + "]");
				}
			} else {
				message = new Message(key);
			}
			log.debug("building wrapper for key [{}], message [{}]", key, message);
			String messageId = getColumnValueOrDefault(rs, getMessageIdField(), key);
			String correlationId = getColumnValueOrDefault(rs, getCorrelationIdField(), messageId);
			MessageWrapper<M> mw = new MessageWrapper<>(message, messageId, correlationId);
			mw.getContext().put(PipeLineSession.STORAGE_ID_KEY, key);
			return mw;
		} catch (SQLException | IOException e) {
			throw new JdbcException(e);
		}
	}

	protected String getKeyFromRawMessage(RawMessageWrapper<M> rawMessage) throws ListenerException {

		Map<String, Object> mwContext = rawMessage.getContext();
		String key = (String) mwContext.get(PipeLineSession.STORAGE_ID_KEY);
		if (StringUtils.isNotEmpty(key)) {
			return key;
		}

		// TESTCOVERAGE: TODO: Below code appears untouched in our unit tests and IAF-Test but might be needed for some stored messages?
		if (rawMessage.getId() != null) {
			return rawMessage.getId();
		} else if (rawMessage instanceof MessageWrapper) {
			try {
				return ((MessageWrapper<M>) rawMessage).getMessage().asString();
			} catch (IOException e) {
				throw new ListenerException(e);
			}
		} else if (rawMessage.getRawMessage() != null) {
			return rawMessage.getRawMessage().toString();
		} else {
			throw new IllegalArgumentException("Cannot extract JDBC message key from raw message [" + rawMessage + "]");
		}
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<M> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		if (rawMessage.getRawMessage() instanceof MessageWrapper<?> messageWrapper) {
			return messageWrapper.getMessage();
		}
		return Message.asMessage(rawMessage.getRawMessage());
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<M> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		// required action already done via ChangeProcessState()
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
	public RawMessageWrapper<M> changeProcessState(RawMessageWrapper<M> rawMessage, ProcessState toState, String reason) throws ListenerException {
		if (!knownProcessStates().contains(toState)) {
			return null; // if toState does not exist, the message can/will not be moved to it, so return null.
		}
		if (isConnectionsArePooled()) {
			try (Connection conn = getConnection()) {
				return changeProcessState(conn, rawMessage, toState, reason);
			} catch (JdbcException|SQLException e) {
				throw new ListenerException(e);
			}
		}
		synchronized (connection) {
			return changeProcessState(connection, rawMessage, toState, reason);
		}
	}

	protected RawMessageWrapper<M> changeProcessState(Connection connection, RawMessageWrapper<M> rawMessage, ProcessState toState, String reason) throws ListenerException {
		String query = getUpdateStatusQuery(toState);
		String key=getKeyFromRawMessage(rawMessage);
		return execute(connection, query, List.of(key)) ? rawMessage : null;
	}

	protected boolean execute(Connection conn, String query, List<String> parameters) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			if (trace && log.isDebugEnabled()) log.debug("executing statement [{}]", query);
			try (PreparedStatement stmt=conn.prepareStatement(query)) {
				stmt.clearParameters();
				int i = 1;
				for (String parameter : parameters) {
					log.debug("setting parameter {} to [{}]", i, parameter);
					JdbcUtil.setParameter(stmt, i++, parameter, getDbmsSupport().isParameterTypeMatchRequired());
				}

				return stmt.executeUpdate() > 0;
			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
			}
		}
		return false;
	}

	protected String convertQuery(String query) throws SQLException, DbmsException {
		if (StringUtils.isEmpty(getSqlDialect())) {
			return query;
		}
		return getDbmsSupport().convertQuery(query, getSqlDialect());
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

	@Override
	public void setPeekUntransacted(boolean b) {
		peekUntransacted = b;
	}

	/**
	 * (only used when <code>peekUntransacted</code>=<code>true</code>) peek query to determine if the select query should be executed. Peek queries are, unlike select queries, executed without a transaction and without a rowlock
	 * @ff.default selectQuery
	 */
	public void setPeekQuery(String string) {
		peekQuery = string;
	}


	/**
	 * Primary key field of the table, used to identify and differentiate messages.
	 * <b>NB: there should be an index on this field!</b>
	 */
	public void setKeyField(String fieldname) {
		keyField = fieldname;
	}

	/**
	 * Field containing the message data
	 * @ff.default <i>same as keyField</i>
	 */
	public void setMessageField(String fieldname) {
		messageField = fieldname;
	}

	/**
	 * Type of the field containing the message data
	 * @ff.default <i>String</i>
	 */
	public void setMessageFieldType(MessageFieldType value) {
		messageFieldType = value;
	}

	/**
	 * Field containing the <code>messageId</code>.
	 * <b>NB: If this column is not set the default (primary key) {@link #setKeyField(String) keyField} will be used as messageId!</b>
	 * @ff.default <i>same as keyField</i>
	 */
	public void setMessageIdField(String fieldname) {
		messageIdField = fieldname;
	}

	/**
	 * Field containing the <code>correlationId</code>.
	 * <b>NB: If this column is not set, the <code>messageId</code> and <code>correlationId</code> will be the same!</b>
	 * @ff.default <i>same as messageIdField</i>
	 */
	public void setCorrelationIdField(String fieldname) {
		correlationIdField = fieldname;
	}

	/** If set, the SQL dialect in which the queries are written and should be translated from to the actual SQL dialect */
	public void setSqlDialect(String string) {
		sqlDialect = string;
	}

	/**
	 * Controls whether BLOB is considered stored compressed in the database
	 * @ff.default true
	 */
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}

	/** Charset used to read BLOB. When specified, then the BLOB will be converted into a string */
	@Deprecated(forRemoval = true, since = "7.6.0")
	public void setBlobCharset(String string) {
		blobCharset = string;
	}

	/**
	 * Controls automatically whether blobdata is stored compressed and/or serialized in the database. N.B. When set true, then the BLOB will be converted into a string
	 * @ff.default false
	 */
	public void setBlobSmartGet(boolean b) {
		blobSmartGet = b;
	}

}
