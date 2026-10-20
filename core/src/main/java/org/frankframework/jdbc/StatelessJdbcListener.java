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
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IPeekableListener;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.doc.Category;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.JdbcUtil;

/**
 * A JDBC Listener that does not maintain any processing-state in the table to which it listens. The listener will pick up the message-field,
 * key field, message-id field, correlation-id and additional fields from the first record returned by the select-query.
 *
 * <p>
 * This listener does not maintain any processing-state in the table, like the {@link JdbcTableListener} or {@link MessageStoreListener} do. To prevent messages
 * from being picked up by multiple instances of the same listener, queries will always use {@code SELECT .... FOR UPDATE} to make sure the rows are locked.
 * </p>
 * <p>
 * In transactional {@link org.frankframework.receivers.Receiver}s this will keep the row locked for the entire duration of the pipeline, potentially stalling
 * other processes. Because there is no state-change management, everything will happen in a single transaction.
 * </p>
 * <p>
 *     To prevent the same message being picked up again after the pipeline completes, you should either remove it, or update it so that it no longer matches the {@code SELECT}
 *     conditions of the query. You can either:
 *     <ul>
 *         <li>Use a {@link FixedQuerySender} in your pipeline to delete the row</li>
 *         <li>Use a {@link #setAfterProcessQuery(String)} which is automatically executed when the pipeline finishes</li>
 *         <li>Use a {@link #setAfterSelectQuery(String)} which can delete the row immediately after it has been selected and read</li>
 *     </ul>
 *     <em>NB: If the pipeline fails and was executed transactionally, the transaction will be rolled back and so any SQL statements that have deleted or updated the row will also be rolled back.</em>
 * </p>
 * <p>
 *     The {@link #setSelectQuery(String)} and {@link #setPeekQuery(String)} will be automatically modified to be {@code SELECT ... FOR UPDATE} or {@code SELECT ... FOR SHARE SKIP LOCKED}
 *     queries. With some databases this means you cannot grouping queries or queries using {@code COUNT(...)}, {@code SUM(...)} or similar statements.
 * </p>
 * <p>
 *     This listener does not do any validation of the queries in configuration, or when opened. Any errors in your queries can result in a heavy load on the
 *     system as queries are retried in quick succession, so be careful with the configuration.
 *     <br/>
 *     All the fields which you want to use and have configured as either key, message, message-id, correlation-id or additionalFields need to be added to
 *     the {@code SELECT} clause of your query with the exact same name.
 *     <br/>
 *     If a field is missing or incorrectly spelled, it will either be silently dropped or may default to another value.
 * </p>
 */
@Log4j2
@SuppressWarnings("SqlSourceToSinkFlow") // Do not need warnings about unsafe SQL on this class
@Category(Category.Type.EXPERIMENTAL)
public class StatelessJdbcListener extends AbstractJdbcListener implements IPeekableListener<Serializable>, IPullingListener<Serializable> {

	private @Getter String afterSelectQuery;
	private @Getter String afterProcessQuery;
	private String preparedAfterSelectQuery;
	private String preparedAfterProcessQuery;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			if (StringUtils.isNotEmpty(afterSelectQuery)) {
				preparedAfterSelectQuery = convertQuery(afterSelectQuery);
			}
			if (StringUtils.isNotEmpty(afterProcessQuery)) {
				preparedAfterProcessQuery = convertQuery(afterProcessQuery);
			}
		} catch (DbmsException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public boolean hasRawMessageAvailable() throws ListenerException {
		try {
			return withConnection(conn -> !JdbcUtil.isQueryResultEmpty(conn, preparedPeekQuery));
		} catch (JdbcException e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message trigger using query [" + peekQuery + "]", e);
		}
	}

	@Override
	public @Nullable RawMessageWrapper<Serializable> getRawMessage(@NonNull Map<String, Object> threadContext) throws ListenerException {
		return withConnection(conn -> {
			RawMessageWrapper<Serializable> messageWrapper = executeSelectMessageQuery(conn);
			if (messageWrapper == null) {
				return null;
			}
			execute(conn, preparedAfterSelectQuery, getUpdateParams(messageWrapper));
			return messageWrapper;
		});
	}

	private static @NonNull List<String> getUpdateParams(RawMessageWrapper<Serializable> messageWrapper) {
		String[] params = new String[]{ getMessageKeyOrNull(messageWrapper), messageWrapper.getId(), messageWrapper.getCorrelationId() };
		return Arrays.stream(params)
				.filter(Objects::nonNull)
				.toList();
	}

	private @Nullable MessageWrapper<Serializable> executeSelectMessageQuery(@NonNull Connection conn) throws ListenerException {
		try (Statement statement = conn.createStatement()) {
			statement.setFetchSize(1);
			//noinspection SqlSourceToSinkFlow
			try (ResultSet resultSet = statement.executeQuery(preparedSelectQuery)) {
				if (!resultSet.next()) {
					return null;
				}
				return extractRawMessage(resultSet);
			}
		} catch (SQLException | IOException | DbmsException e) {
			throw new ListenerException("Cannot load message from database", e);
		}

	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<Serializable> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		if (processResult.isSuccessful()) {
			withConnection(conn -> {
				execute(conn, preparedAfterProcessQuery, getUpdateParams(rawMessage));
			});
		}
	}

	/**
	 * A SQL statement ({@code UPDATE} or {@code DELETE}) that can optionally be used to make sure the {@code SELECT} query does not pick up the same row again. The
	 * {@code afterSelectQuery} is executed immediately after the {@code SELECT} query, before the pipeline is executed.
	 * <br/>
	 * The query can have up to 3 parameters, values of which are taken in order from the fields mapped to {@link #setKeyField(String)}, {@link #setMessageIdField(String)}
	 * and {@link #setCorrelationIdField(String)} in the {@link #setSelectQuery(String)}.
	 * <p>
	 * Deleting or updating the selected row immediately might reduce lock-contention on the table, however the row is not available anymore in the pipeline
	 * for other queries.
	 * </p>
	 * <p>
	 *     If the {@link org.frankframework.receivers.Receiver} is transactional, and the pipeline fails, the transaction is rolled back so the
	 *     {@code afterSelectQuery} is rolled back.
	 * </p>
	 */
	public void setAfterSelectQuery(String afterSelectQuery) {
		this.afterSelectQuery = afterSelectQuery;
	}

	/**
	 * A SQL statement ({@code UPDATE} or {@code DELETE}) that can optionally be used to make sure the {@code SELECT} query does not pick up the same row again. The
	 * {@code afterProcessQuery} is executed after the pipeline has finished.
	 * <br/>
	 * The query can have up to 3 parameters, values of which are taken in order from the fields mapped to {@link #setKeyField(String)}, {@link #setMessageIdField(String)}
	 * and {@link #setCorrelationIdField(String)} in the {@link #setSelectQuery(String)}.
	 * <p>
	 * Deleting or updating the selected row after processing in the pipeline is complete means the data of the row can be queried again in the pipeline, but
	 * also keeps the row locked for other queries outside the transaction.
	 * </p>
	 * <p>
	 *     The {@code afterProcessQuery} is executed only when the pipeline has finished successfully.
	 * </p>
	 * <p>
	 *     If the {@link org.frankframework.receivers.Receiver} is transactional, and the transaction is rolled back, the
	 *     {@code afterProcessQuery} is rolled back if it was executed.
	 * </p>
	 */
	public void setAfterProcessQuery(String afterProcessQuery) {
		this.afterProcessQuery = afterProcessQuery;
	}

	@Override
	public void setSelectQuery(String string) {
		super.setSelectQuery(string);
	}
}
