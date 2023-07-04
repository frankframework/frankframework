/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DB2XMLWriter;

/**
 * QuerySender that assumes a fixed query, possibly with attributes.
 *
 * <p><b>NOTE:</b> See {@link DB2XMLWriter} for Resultset!</p>
 *
 * @ff.parameters All parameters present are applied to the query to be executed.
 *
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class FixedQuerySender extends JdbcQuerySenderBase<QueryExecutionContext> {

	private @Getter String query=null;
	private @Getter int batchSize;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getQuery())) {
			throw new ConfigurationException(getLogPrefix()+"query must be specified");
		}
	}

	@Override
	protected String getQuery(Message message) {
		return getQuery();
	}

	@Override
	protected boolean canProvideOutputStream() {
		return (getQueryTypeEnum()==QueryType.UPDATECLOB && StringUtils.isEmpty(getClobSessionKey()) ||
				getQueryTypeEnum()==QueryType.UPDATEBLOB && StringUtils.isEmpty(getBlobSessionKey()))
				&& (getParameterList()==null || !getParameterList().isInputValueOrContextRequiredForResolution());
	}


	@Override
	public QueryExecutionContext openBlock(PipeLineSession session) throws SenderException, TimeoutException {
		try {
			Connection connection = getConnectionForSendMessage();
			QueryExecutionContext result;
			try {
				QueryExecutionContext result1 = getQueryExecutionContext(connection, null);
				if (getBatchSize()>0) {
					result1.getStatement().clearBatch();
				}
				result = result1;
			} catch (JdbcException | ParameterException | SQLException e) {
				throw new SenderException(getLogPrefix() + "cannot getQueryExecutionContext",e);
			}
			return result;
		} catch (JdbcException e) {
			throw new SenderException("cannot get StatementSet",e);
		}
	}

	@Override
	public void closeBlock(QueryExecutionContext blockHandle, PipeLineSession session) throws SenderException {
		try {
			super.closeStatementSet(blockHandle, session);
		} catch (Exception e) {
			log.warn("{} Unhandled exception closing statement-set", getLogPrefix(), e);
		}
		try {
			closeConnectionForSendMessage(blockHandle.getConnection(), session);
		} catch (JdbcException | TimeoutException e) {
			log.warn("cannot close connection", e);
		}
	}

	@Override
	protected void closeStatementSet(QueryExecutionContext statementSet, PipeLineSession session) {
		// postpone close to closeBlock()
	}

	@Override
	// implements IBlockEnabledSender.sendMessage()
	public SenderResult sendMessage(QueryExecutionContext blockHandle, Message message, PipeLineSession session) throws SenderException, TimeoutException {
		return new SenderResult(executeStatementSet(blockHandle, message, session, null).getResult());
	}

	@Override
	// implements IStreamingSender.sendMessage()
	public PipeRunResult sendMessage(Message message, PipeLineSession session, IForwardTarget next) throws SenderException, TimeoutException {
		QueryExecutionContext blockHandle = openBlock(session);
		try {
			return executeStatementSet(blockHandle, message, session, next);
		} finally {
			closeBlock(blockHandle, session);
		}
	}

	/** The SQL query text to be excecuted each time sendMessage() is called
	 * @ff.mandatory
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/** When set larger than 0 and used as a child of an IteratingPipe, then the database calls are made in batches of this size. Only for queryType=other.
	  * @ff.default 0
	  */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

}
