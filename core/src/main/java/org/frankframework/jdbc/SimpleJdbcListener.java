/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021, 2023 WeAreFrank!

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

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.JdbcException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

/**
 * Database Listener that returns a count of messages available, but does not perform any locking or
 * other management of processing messages in parallel.
 *
 * @author  Peter Leeuwenburgh
 */
public class SimpleJdbcListener extends JdbcFacade implements IPullingListener<String> {
	protected static final String KEYWORD_SELECT_COUNT = "select count(";

	private String selectQuery;
	private boolean trace = false;

	protected Connection connection = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT_COUNT)) {
			throw new ConfigurationException(getLogPrefix() + "query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT_COUNT + "]");
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
		return new LinkedHashMap<>();
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		// No-op
	}

	@Override
	public RawMessageWrapper<String> getRawMessage(@Nonnull Map<String, Object> threadContext) throws ListenerException {
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

	protected RawMessageWrapper<String> getRawMessage(Connection conn, Map<String,Object> threadContext) throws ListenerException {
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
	public Message extractMessage(@Nonnull RawMessageWrapper<String> rawMessage, @Nonnull Map<String,Object> context) {
		return new Message(rawMessage.getRawMessage());
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
