/*
   Copyright 2013, 2016, 2018-2020 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IHasProcessState;
import org.frankframework.core.IPeekableListener;
import org.frankframework.core.IRedeliveringListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.ProcessState;
import org.frankframework.dbms.JdbcException;
import org.frankframework.receivers.RawMessageWrapper;

/**
 * JdbcListener base class.
 *
 * @param <M> MessageWrapper or key. Key is also used as messageId
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class JdbcListener<M> extends AbstractJdbcListener<M> implements IPeekableListener<M>, IHasProcessState<M>, IRedeliveringListener<M> {

	private Map<ProcessState, String> updateStatusQueries = new EnumMap<>(ProcessState.class);
	private Map<ProcessState, Set<ProcessState>> targetProcessStates = new EnumMap<>(ProcessState.class);

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			Map<ProcessState, String> orderedUpdateStatusQueries = new LinkedHashMap<>();
			for (ProcessState state : ProcessState.values()) {
				if(updateStatusQueries.containsKey(state)) {
					String convertedUpdateStatusQuery = convertQuery(updateStatusQueries.get(state));
					orderedUpdateStatusQueries.put(state, convertedUpdateStatusQuery);
				}
			}
			updateStatusQueries=orderedUpdateStatusQueries;
			targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates());
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public boolean messageWillBeRedeliveredOnExitStateError() {
		return knownProcessStates().contains(ProcessState.INPROCESS);
	}

	@Override
	public @Nullable RawMessageWrapper<M> getRawMessage(@NonNull Map<String, Object> threadContext) throws ListenerException {
		return withConnection(conn -> {
			return getRawMessage(conn, threadContext);
		});
	}

	protected @Nullable RawMessageWrapper<M> getRawMessage(Connection conn, Map<String,Object> threadContext) throws ListenerException {
		String query = preparedSelectQuery;
		try (Statement stmt = conn.createStatement()) {
			stmt.setFetchSize(1);
			if (trace && log.isDebugEnabled()) log.debug("executing query for [{}]", preparedSelectQuery);
			//noinspection SqlSourceToSinkFlow
			try (ResultSet rs=stmt.executeQuery(preparedSelectQuery)) {
				if (!rs.next()) {
					return null;
				}
				return extractRawMessage(rs);
			} catch (SQLException e) {
				if (!getDbmsSupport().hasSkipLockedFunctionality()) {
					String errorMessage = e.getMessage();
					if (errorMessage != null && errorMessage.toLowerCase().contains("timeout") && errorMessage.toLowerCase().contains("lock")) {
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

	protected String getKeyFromRawMessage(RawMessageWrapper<M> rawMessage) {

		Map<String, Object> mwContext = rawMessage.getContext();
		String key = (String) mwContext.get(PipeLineSession.STORAGE_ID_KEY);
		if (StringUtils.isNotEmpty(key)) {
			return key;
		}
		throw new IllegalArgumentException("Cannot extract JDBC message key from raw message [" + rawMessage + "]");
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<M> rawMessage, PipeLineSession pipeLineSession) {
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
	public @Nullable RawMessageWrapper<M> changeProcessState(RawMessageWrapper<M> rawMessage, ProcessState toState, String reason) throws ListenerException {
		if (!knownProcessStates().contains(toState)) {
			return null; // if toState does not exist, the message can/will not be moved to it, so return null.
		}
		return withConnection(c -> {
			return changeProcessState(c, rawMessage, toState, reason);
		});
	}

	protected @Nullable RawMessageWrapper<M> changeProcessState(Connection connection, RawMessageWrapper<M> rawMessage, ProcessState toState, String reason) throws ListenerException {
		String query = getUpdateStatusQuery(toState);
		String key=getKeyFromRawMessage(rawMessage);
		return execute(connection, query, List.of(key)) ? rawMessage : null;
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

	/**
	 * Type of the field containing the message data - no longer needed as the listener will now automatically determine the message field type
	 * @deprecated No longer needed, automatically determined
	 */
	@Deprecated(forRemoval = true, since = "10.4")
	@ConfigurationWarning("It is no longer necessary to configure this")
	public void setMessageFieldType(@NonNull String ignored) {
		// No-op
	}
}
