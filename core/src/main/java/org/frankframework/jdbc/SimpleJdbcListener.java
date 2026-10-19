/*
   Copyright 2013 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

/**
 * Database Listener that returns a count of messages available, but does not perform any locking or
 * other management of processing messages in parallel.
 *
 * @author  Peter Leeuwenburgh
 */
@Deprecated(forRemoval = true, since = "10.2")
@ConfigurationWarning("Please use JdbcTableListener (also non-locking) to perform a count query or let it directly return messages.")
public class SimpleJdbcListener extends JdbcFacade implements IPullingListener<String> {
	protected static final String KEYWORD_SELECT_COUNT = "select count(";

	private String selectQuery;
	private boolean trace = false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT_COUNT)) {
			throw new ConfigurationException(getLogPrefix() + "query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT_COUNT + "]");
		}
	}

	@NonNull
	@Override
	public Map<String,Object> openThread() {
		return new LinkedHashMap<>();
	}

	@Override
	public void closeThread(@NonNull Map<String, Object> threadContext) {
		// No-op
	}

	@Override
	public @Nullable RawMessageWrapper<String> getRawMessage(@NonNull Map<String, Object> threadContext) throws ListenerException {
		return withConnection((ThrowingFunction<Connection, RawMessageWrapper<String>, ListenerException>) this::getRawMessage);
	}

	protected @Nullable RawMessageWrapper<String> getRawMessage(Connection conn) throws ListenerException {
		String query = getSelectQuery();
		try (Statement stmt = conn.createStatement()) {
			stmt.setFetchSize(1);
			if (trace && log.isDebugEnabled()) log.debug("executing query for [{}]", query);
			try (ResultSet rs = stmt.executeQuery(query)) {
				if (!rs.next()) {
					return null;
				}
				int count = rs.getInt(1);
				if (count == 0) {
					return null;
				}
				return new RawMessageWrapper<>("<count>" + count + "</count>");
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message using query [" + query + "]", e);
		}
	}

	@Override
	public Message extractMessage(@NonNull RawMessageWrapper<String> rawMessage, @NonNull Map<String,Object> context) {
		return Message.asMessage(rawMessage.getRawMessage());
	}

	protected ResultSet executeQuery(Connection conn, String query) throws ListenerException {
		if (StringUtils.isEmpty(query)) {
			throw new ListenerException(getLogPrefix() + "cannot execute empty query");
		}
		if (trace && log.isDebugEnabled()) log.debug("executing query [{}]", query);
		try (Statement stmt = conn.createStatement()) {
			return stmt.executeQuery(query);
		} catch (SQLException e) {
			throw new ListenerException(getLogPrefix() + "exception executing statement [" + query + "]", e);
		}
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<String> rawMessage, PipeLineSession pipeLineSession) {
		// No-op
	}

	protected void execute(Connection conn, String query) throws ListenerException {
		execute(conn, query, null);
	}

	protected void execute(Connection conn, String query, String parameter) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			if (trace && log.isDebugEnabled()) log.debug("executing statement [{}]", query);
			try (PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.clearParameters();
				if (StringUtils.isNotEmpty(parameter)) {
					log.debug("setting parameter 1 to [{}]", parameter);
					stmt.setString(1, parameter);
				}
				stmt.execute();

			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix() + "exception executing statement [" + query + "]", e);
			}
		}
	}

	/** count query that returns the number of available records. when there are available records the pipeline is activated */
	public void setSelectQuery(String string) {
		selectQuery = string;
	}

	public String getSelectQuery() {
		return selectQuery;
	}

	public boolean isTrace() {
		return trace;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}
}
