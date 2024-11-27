/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020, 2022-2024 WeAreFrank!

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
import java.sql.SQLException;

import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;

/**
 * QuerySender that interprets the input message as a query, possibly with attributes.
 * Messages are expected to contain sql-text.
 *
 * @ff.info Please note that the default value of {@code trimSpaces} is {@literal true}
 * @ff.parameters All parameters present are applied to the query to be executed.
 *
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class DirectQuerySender extends AbstractJdbcQuerySender<Connection> {

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

	public void configure(Adapter adapter) throws ConfigurationException {
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
			return getConnectionForSendMessage();
		} catch (JdbcException e) {
			throw new SenderException("cannot get Connection",e);
		}
	}

	@Override
	public void closeBlock(Connection connection, PipeLineSession session) throws SenderException {
		super.closeConnectionForSendMessage(connection, session);
	}

	@Override
	protected void closeConnectionForSendMessage(Connection connection, PipeLineSession session) {
		// postpone close to closeBlock()
	}


	@Override
	// implements IBlockEnabledSender.sendMessage()
	public SenderResult sendMessage(Connection connection, Message message, PipeLineSession session) throws SenderException, TimeoutException {
		try {
			QueryExecutionContext queryExecutionContext = prepareStatementSet(connection, message);
			try {
				return executeStatementSet(queryExecutionContext, message, session);
			} finally {
				closeStatementSet(queryExecutionContext);
			}
		} catch (SenderException|TimeoutException e) {
			throw e;
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	protected QueryExecutionContext prepareStatementSet(Connection connection, Message message) throws SenderException {
		try {
			QueryExecutionContext result = getQueryExecutionContext(connection, message);
			if (getBatchSize()>0) {
				result.getStatement().clearBatch();
			}
			return result;
		} catch (JdbcException | SQLException e) {
			throw new SenderException("cannot getQueryExecutionContext",e);
		}
	}
}
