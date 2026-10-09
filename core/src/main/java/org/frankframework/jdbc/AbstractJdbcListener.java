/*
   Copyright 2026 WeAreFrank!

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
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import lombok.Getter;
import lombok.Lombok;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IPeekableListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.StringUtil;

/**
 * Abstract base class for JDBC listeners (excluding the deprecated and broken "SimpleJdbcListener").
 * @param <M> Type of messages
 */
public abstract class AbstractJdbcListener<M> extends JdbcFacade implements IPeekableListener<M> {
	public static final String ADDITIONAL_QUERY_FIELDS_KEY = "ADDITIONAL_QUERY_FIELDS";
	protected @Getter String selectQuery;
	protected @Getter String messageField;
	protected @Getter String messageIdField;
	protected @Getter String correlationIdField;
	protected String preparedSelectQuery;
	protected String preparedPeekQuery;
	protected @Getter String peekQuery;
	protected @Getter String keyField;
	protected @Getter String additionalFields;
	protected @Getter @NonNull List<String> additionalFieldsList = List.of();
	protected @Getter boolean peekUntransacted=true;

	protected @Setter @Getter boolean trace=false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(selectQuery)) {
			throw new ConfigurationException(getLogPrefix() + "selectQuery may not be empty");
		}
		try {
			String convertedSelectQuery = convertQuery(getSelectQuery());
			preparedSelectQuery = getDbmsSupport().prepareQueryTextForWorkQueueReading(1, convertedSelectQuery);
			preparedPeekQuery = StringUtils.isNotEmpty(getPeekQuery()) ? convertQuery(getPeekQuery()) : getDbmsSupport().prepareQueryTextForWorkQueuePeeking(1, convertedSelectQuery);
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
		// Check that the SELECT query contains the fields wanted
		List<String> fieldsNotInQuery = getAdditionalFieldsList().stream()
				.filter(f -> !selectQuery.matches(".*\\W" + f + "\\W.*"))
				.toList();
		if (!fieldsNotInQuery.isEmpty()) {
			throw new ConfigurationException("additionalFields contains fields not in the select query: " + fieldsNotInQuery);
		}
	}

	@Override
	public @NonNull Map<String,Object> openThread() {
		return new HashMap<>();
	}

	@Override
	public void closeThread(@NonNull Map<String, Object> threadContext) {
		// No-op
	}

	@Override
	public boolean hasRawMessageAvailable() throws ListenerException {
		if (StringUtils.isEmpty(preparedPeekQuery)) {
			return true;
		}
		try {
			return withConnection(conn ->  !JdbcUtil.isQueryResultEmpty(conn, preparedPeekQuery));
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message trigger using query [" + preparedPeekQuery + "]", e);
		}
	}

	/**
	 * Get column value from {@link ResultSet}, or the default if either the column-name is empty (unconfigured) or if
	 * the result-set does not contain a column of this name.
	 * <br/>
	 * Notably, if the column exists in the resultset but has a null value, null is returned instead of the defaultValue.
	 *
	 * @param rs The {@link ResultSet} from which to get the column.
	 * @param columnName The name of the column, can be {@code null} or empty.
	 * @param defaultValue Default value for the column if columnName was empty, or not present in the {@code ResultSet}. Can be {@code null}.
	 * @return Value from the {@code ResultSet}, or the default if columName was empty or not present in the ResultSet.
	 * @throws SQLException Propagates the {@link SQLException} which may be thrown from the {@link ResultSet}.
	 */
	protected @Nullable String getColumnValueOrDefault(@NonNull ResultSet rs, @Nullable String columnName, @Nullable String defaultValue) throws SQLException {
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

	protected void addAdditionalValuesToMessageWrapper(ResultSet rs, RawMessageWrapper<M> mw) throws SQLException {
		ResultSetMetaData metaData = rs.getMetaData();
		Function<String, Map.Entry<String, Message>> extractFieldValue = fieldName -> {
			try {
				int colNum = rs.findColumn(fieldName);
				Message value = JdbcUtil.getValueAsMessage(getDbmsSupport(), rs, colNum, metaData, getBlobCharset(), isBlobsCompressed());
				return Map.entry(fieldName, value);
			} catch (Exception e) {
				throw Lombok.sneakyThrow(e);
			}
		};
		// Make sure that the sub-map supports case-insensitive lookup of entries
		Map<String, Message> additionalValues = getAdditionalFieldsList().stream()
				.map(extractFieldValue)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (m1, m2) -> m2, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));

		if (!additionalValues.isEmpty()) {
			mw.getContext().put(ADDITIONAL_QUERY_FIELDS_KEY, additionalValues);
		}
	}

	protected boolean execute(@NonNull Connection conn, @Nullable String query, @NonNull List<String> parameters) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			if (trace && log.isDebugEnabled()) log.debug("executing statement [{}]", query);
			//noinspection SqlSourceToSinkFlow
			try (PreparedStatement stmt=conn.prepareStatement(query)) {
				stmt.clearParameters();
				ParameterMetaData parameterMetaData = stmt.getParameterMetaData();
				int i = 1;
				for (String parameter : parameters) {
					log.debug("setting parameter {} to [{}]", i, parameter);
					JdbcUtil.setParameter(stmt, i++, parameter, getDbmsSupport().isParameterTypeMatchRequired(), parameterMetaData);
				}

				return stmt.executeUpdate() > 0;
			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix()+"exception executing statement ["+query+"]",e);
			}
		}
		return false;
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
	 * @throws SQLException If loading the message resulted in a database exception.
	 * @throws IOException If loading the message resulted in an IO exception
	 */
	protected @NonNull RawMessageWrapper<M> extractRawMessage(@NonNull ResultSet rs) throws SQLException, IOException, DbmsException {
		String key = getColumnValueOrDefault(rs, getKeyField(), null);
		Message message;
		if (StringUtils.isNotEmpty(getMessageField())) {
			message = JdbcUtil.getValueAsMessage(getDbmsSupport(), rs, rs.findColumn(getMessageField()), rs.getMetaData(), getBlobCharset(), isBlobsCompressed(), false, isBlobSmartGet(), false);
		} else {
			message = Message.asMessage(key);
		}
		log.debug("building wrapper for key [{}], message [{}]", key, message);
		String messageId = getColumnValueOrDefault(rs, getMessageIdField(), key);
		String correlationId = getColumnValueOrDefault(rs, getCorrelationIdField(), messageId);
		MessageWrapper<M> mw = new MessageWrapper<>(message, messageId, correlationId); // Creating instance of MessageWrapper instead of RawMessageWrapper means the Receiver will not call #extractMessage
		if (key != null) {
			mw.getContext().put(PipeLineSession.STORAGE_ID_KEY, key);
		}
		addAdditionalValuesToMessageWrapper(rs, mw);
		return mw;
	}

	@Override
	public Message extractMessage(@NonNull RawMessageWrapper<M> rawMessage, @NonNull Map<String, Object> context) throws ListenerException {
		if (rawMessage.getRawMessage() instanceof MessageWrapper<?> messageWrapper) {
			return messageWrapper.getMessage();
		}
		return Message.asMessage(rawMessage.getRawMessage());
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
	 * Field containing the <code>messageId</code>.
	 * <b>NB: If this column is not, or set to a column which cannot be found in the query result, the default (primary key) {@link #setKeyField(String) keyField}
	 * will be used as messageId!</b>
	 *
	 * @ff.default <i>same as keyField</i>
	 */
	public void setMessageIdField(String fieldname) {
		messageIdField = fieldname;
	}

	/**
	 * Field containing the <code>correlationId</code>.
	 * <b>NB: If this column is not set, or set to a column which cannot be found in the query result, the <code>messageId</code> and <code>correlationId</code>
	 * will be the same!</b>
	 *
	 * @ff.default <i>same as messageIdField</i>
	 */
	public void setCorrelationIdField(String fieldname) {
		correlationIdField = fieldname;
	}

	/**
	 * Comma-separated list of additional fields to be loaded from the table, besides Message, Key, MessageID and CorrelationID. Any fields listed here will
	 * be added to the session as session-variables, with the prefix {@literal ADDITIONAL_QUERY_FIELDS_KEY}. So if for example you specify {@code additionalFields = "updated_at"},
	 * then in the session there will be a variable {@code ADDITIONAL_QUERY_FIELDS.updated_at}.
	 */
	public void setAdditionalFields(String fieldNames) {
		this.additionalFields = fieldNames;
		this.additionalFieldsList = StringUtil.split(getAdditionalFields());
	}
}
