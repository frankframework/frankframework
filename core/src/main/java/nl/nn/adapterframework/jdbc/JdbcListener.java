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
package nl.nn.adapterframework.jdbc;

import java.io.IOException;
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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IHasProcessState;
import nl.nn.adapterframework.core.IPeekableListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * JdbcListener base class.
 *
 * @param <M> MessageWrapper or key. Key is also used as messageId
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class JdbcListener<M extends Object> extends JdbcFacade implements IPeekableListener<M>, IHasProcessState<M> {

	public static final String CORRELATION_ID_KEY="cid";
	public static final String STORAGE_KEY_KEY="key";

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

	private @Getter boolean trace=false;
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
			String convertedSelectQuery = convertQuery(getSelectQuery(), QueryType.SELECT);
			preparedSelectQuery = getDbmsSupport().prepareQueryTextForWorkQueueReading(1, convertedSelectQuery);
			preparedPeekQuery = StringUtils.isNotEmpty(getPeekQuery()) ? convertQuery(getPeekQuery(), QueryType.SELECT) : getDbmsSupport().prepareQueryTextForWorkQueuePeeking(1, convertedSelectQuery);
			Map<ProcessState, String> orderedUpdateStatusQueries = new LinkedHashMap<>();
			for (ProcessState state : ProcessState.values()) {
				if(updateStatusQueries.containsKey(state)) {
					String convertedUpdateStatusQuery = convertQuery(updateStatusQueries.get(state), QueryType.OTHER);
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
			try (JdbcSession session = getDbmsSupport().prepareSessionForNonLockingRead(conn)) {
				return !JdbcUtil.isQueryResultEmpty(conn, preparedPeekQuery);
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message trigger using query [" + preparedPeekQuery + "]", e);
		}
	}

	@Override
	public RawMessageWrapper<M> getRawMessage(Map<String,Object> threadContext) throws ListenerException {
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
			if (trace && log.isDebugEnabled()) log.debug("executing query for ["+query+"]");
			try (ResultSet rs=stmt.executeQuery(query)) {
				if (!rs.next()) {
					return null;
				}
				return extractRawMessage(rs);
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

	private String getValueOrDefaultIfColumnDoesNotExistInTable(ResultSet rs, String columnName, String defaultValue) {
		if (StringUtils.isEmpty(columnName)) {
			return defaultValue;
		}
		try {
			int index = rs.findColumn(columnName);
			if (index>0) {
				return rs.getString(index);
			}
		} catch (SQLException e) {
			// ignore exception, assume columnName does not exist
		}
		return null; // do not return defaultValue, as the column probably exists, but not in this result set
	}

	/**
	 * TODO: Fix this JavaDoc
	 * This wonderful little method returns either a {@link String} or a {@link MessageWrapper} (but never an instance
	 * of type {@code <M>}.
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
		// TODO: This needs to be reviewed (and fixed in some way)
		try {
			String key=rs.getString(getKeyField());
			Message message;
			if (StringUtils.isNotEmpty(getMessageField())) {
				switch (getMessageFieldType()) {
					case CLOB:
						message=new Message(JdbcUtil.getClobAsString(getDbmsSupport(), rs,getMessageField(),false));
						break;
					case BLOB:
						if (isBlobSmartGet() || StringUtils.isNotEmpty(getBlobCharset())) { // in this case blob contains a String
							message=new Message(JdbcUtil.getBlobAsString(getDbmsSupport(), rs,getMessageField(),getBlobCharset(),isBlobsCompressed(),isBlobSmartGet(),false));
						} else {
							try (InputStream blobStream = JdbcUtil.getBlobInputStream(getDbmsSupport(), rs, getMessageField(), isBlobsCompressed())) {
								message=new Message(blobStream);
								message.preserve();
							}
						}
						break;
					case STRING:
						message=new Message(rs.getString(getMessageField()));
						break;
					default:
						throw new IllegalArgumentException("Illegal messageFieldType ["+getMessageFieldType()+"]");
				}
			} else {
				message = new Message(key);
			}
			// log.debug("building wrapper for key ["+key+"], message ["+message+"]");
			String messageId = getValueOrDefaultIfColumnDoesNotExistInTable(rs, getMessageIdField(), key);
			MessageWrapper<M> mw = new MessageWrapper<>(message, messageId);
			String correlationId = getValueOrDefaultIfColumnDoesNotExistInTable(rs, getCorrelationIdField(), messageId);
			mw.getContext().put(CORRELATION_ID_KEY, correlationId);
			mw.getContext().put(STORAGE_KEY_KEY, key);
			return mw;
		} catch (SQLException | IOException e) {
			throw new JdbcException(e);
		}
	}

	@Override
	public String getIdFromRawMessage(RawMessageWrapper<M> rawMessage, Map<String,Object> threadContext) throws ListenerException {
		if (rawMessage == null) {
			updateThreadContextWithIds(threadContext, null, null, null);
			return null;
		}
		String key;
		String cid;
		String mid;
		if (rawMessage.getId() != null) {
			mid = rawMessage.getId();
		} else if (rawMessage instanceof MessageWrapper) {
			try {
				mid = ((MessageWrapper) rawMessage).getMessage().asString();
			} catch (IOException e) {
				throw new ListenerException(e);
			}
		} else if (rawMessage.getRawMessage() != null) {
			mid = rawMessage.getRawMessage().toString();
		} else {
			mid = null;
		}
		Map<String,Object> mwContext = rawMessage.getContext();
		cid = (String)mwContext.getOrDefault(CORRELATION_ID_KEY, mid);
		key = (String)mwContext.getOrDefault(STORAGE_KEY_KEY, mid);
		if (StringUtils.isEmpty(key)) {
			key = mid; // backward compatibility
		}
		updateThreadContextWithIds(threadContext, key, cid, mid);
		return mid;
	}

	private static void updateThreadContextWithIds(Map<String, Object> threadContext, String key, String cid, String mid) {
		if (threadContext != null) {
			PipeLineSession.setListenerParameters(threadContext, mid, cid, null, null);
			threadContext.put(STORAGE_KEY_KEY, key);
		}
	}

	protected String getKeyFromRawMessage(RawMessageWrapper<M> rawMessage) throws ListenerException {
		Map<String,Object> context = new HashMap<>();
		getIdFromRawMessage(rawMessage, context); // populate context with storage key
		return (String)context.get(STORAGE_KEY_KEY);
	}

	@Override
	public Message extractMessage(RawMessageWrapper<M> rawMessage, Map<String,Object> threadContext) throws ListenerException {
		Message message;
		if (rawMessage instanceof MessageWrapper) {
			message = ((MessageWrapper)rawMessage).getMessage();
		} else {
			message = Message.asMessage(rawMessage.getRawMessage());
		}
		return message;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<M> rawMessage, Map<String,Object> threadContext) throws ListenerException {
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
		return execute(connection, query, key) ? rawMessage : null;
	}

	protected boolean execute(Connection conn, String query, String... parameters) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			if (trace && log.isDebugEnabled()) log.debug("executing statement ["+query+"]");
			try (PreparedStatement stmt=conn.prepareStatement(query)) {
				stmt.clearParameters();
				int i=1;
				for(String parameter:parameters) {
					log.debug("setting parameter "+i+" to ["+parameter+"]");
					JdbcUtil.setParameter(stmt, i++, parameter, getDbmsSupport().isParameterTypeMatchRequired());
				}

				return stmt.executeUpdate() > 0;
			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
			}
		}
		return false;
	}

	protected String convertQuery(String query, QueryType queryType) throws JdbcException, SQLException {
		if (StringUtils.isEmpty(getSqlDialect())) {
			return query;
		}
		QueryExecutionContext qec = new QueryExecutionContext(query, queryType, null);
		getDbmsSupport().convertQuery(qec, getSqlDialect());
		return qec.getQuery();
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


	/** Primary key field of the table, used to identify messages. For optimal performance, there should be an index on this field. */
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
	 * Field containing the message Id
	 * @ff.default <i>same as keyField</i>
	 */
	public void setMessageIdField(String fieldname) {
		messageIdField = fieldname;
	}

	/**
	 * Field containing the correlationId
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
	@Deprecated
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

	public void setTrace(boolean trace) {
		this.trace = trace;
	}

}
