/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
import java.sql.Connection;
import java.sql.SQLException;

import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * QuerySender that interprets the input message as a query, possibly with attributes.
 * Messages are expected to contain sql-text.
 *
 * @ff.parameters All parameters present are applied to the query to be executed.
 *
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class DirectQuerySender extends JdbcQuerySenderBase<Connection>{

	@Override
	public void configure() throws ConfigurationException {
		configure(null); //No adapter? Don't trust!
	}

	public void configure(boolean ignoreSQLInjectionWarning) throws ConfigurationException {
		if(ignoreSQLInjectionWarning) {
			super.configure();
		} else {
			configure(null);
		}
	}

	public void configure(IAdapter adapter) throws ConfigurationException {
		super.configure();

		if (adapter != null) {
			ConfigurationWarnings.add(adapter, log, "has a ["+ClassUtils.nameOf(this)+"]. This may cause potential SQL injections!", SuppressKeys.SQL_INJECTION_SUPPRESS_KEY, adapter);
		} else {
			//This can still be triggered when a Sender is inside a SenderSeries wrapper such as ParallelSenders
			ApplicationWarnings.add(log, "The class ["+ClassUtils.nameOf(this)+"] is used one or more times. This may cause potential SQL injections!");
		}
	}

	@Override
	protected String getQuery(Message message) throws SenderException {
		try {
			return message.asString();
		} catch (IOException e) {
			throw new SenderException(e);
		}
	}


	@Override
	public Connection openBlock(PipeLineSession session) throws SenderException, TimeoutException {
		try {
			return super.getConnectionForSendMessage(null);
		} catch (JdbcException e) {
			throw new SenderException("cannot get Connection",e);
		}
	}

	@Override
	public void closeBlock(Connection connection, PipeLineSession session) throws SenderException {
		try {
			super.closeConnectionForSendMessage(connection, session);
		} catch (JdbcException | TimeoutException e) {
			throw new SenderException("cannot close Connection",e);
		}
	}

	@Override
	protected Connection getConnectionForSendMessage(Connection blockHandle) throws JdbcException, TimeoutException {
		return blockHandle;
	}

	@Override
	protected void closeConnectionForSendMessage(Connection connection, PipeLineSession session) throws JdbcException, TimeoutException {
		// postpone close to closeBlock()
	}


	@Override
	// implements IBlockEnabledSender.sendMessage()
	public SenderResult sendMessage(Connection blockHandle, Message message, PipeLineSession session) throws SenderException, TimeoutException {
		return new SenderResult(sendMessageOnConnection(blockHandle, message, session, null).getResult());
	}

	@Override
	// implements IStreamingSender.sendMessage()
	public PipeRunResult sendMessage(Message message, PipeLineSession session, IForwardTarget next) throws SenderException, TimeoutException {
		Connection blockHandle = openBlock(session);
		try {
			return sendMessageOnConnection(blockHandle, message, session, next);
		} finally {
			closeBlock(blockHandle, session);
		}
	}

	protected PipeRunResult sendMessageOnConnection(Connection connection, Message message, PipeLineSession session, IForwardTarget next) throws SenderException, TimeoutException {
		try (JdbcSession jdbcSession = isAvoidLocking() ? getDbmsSupport().prepareSessionForNonLockingRead(connection) : null) {
			QueryExecutionContext queryExecutionContext = prepareStatementSet(connection, message, session);
			try {
				return executeStatementSet(queryExecutionContext, message, session, next);
			} finally {
				closeStatementSet(queryExecutionContext, session);
			}
		} catch (SenderException|TimeoutException e) {
			throw e;
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	protected QueryExecutionContext prepareStatementSet(Connection connection, Message message, PipeLineSession session) throws SenderException {
		try {
			QueryExecutionContext result = getQueryExecutionContext(connection, message, session);
			if (getBatchSize()>0) {
				result.getStatement().clearBatch();
			}
			return result;
		} catch (JdbcException | ParameterException | SQLException e) {
			throw new SenderException(getLogPrefix() + "cannot getQueryExecutionContext",e);
		}
	}
}
