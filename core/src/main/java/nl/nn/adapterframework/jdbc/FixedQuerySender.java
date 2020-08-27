/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.stream.Message;

/**
 * QuerySender that assumes a fixed query, possibly with attributes.
 * 
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>all parameters present are applied to the statement to be executed</td></tr>
 * </table>
 * </p>
 * 
 * <p><b>NOTE:</b> See {@link nl.nn.adapterframework.util.DB2XMLWriter DB2XMLWriter} for Resultset!</p>
 * 
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class FixedQuerySender extends JdbcQuerySenderBase<QueryExecutionContext> {

	private String query=null;
	private int batchSize;

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
	public boolean canProvideOutputStream() {
		return  "updateClob".equalsIgnoreCase(getQueryType()) && StringUtils.isEmpty(getClobSessionKey()) ||
				"updateBlob".equalsIgnoreCase(getQueryType()) && StringUtils.isEmpty(getBlobSessionKey());
	}


	@Override
	public QueryExecutionContext openBlock(IPipeLineSession session) throws SenderException, TimeOutException {
		try {
			Connection connection = getConnectionForSendMessage(null);
			QueryExecutionContext result = super.prepareStatementSet(null, connection, null, session);
			JdbcSession jdbcSession = isAvoidLocking()?getDbmsSupport().prepareSessionForNonLockingRead(connection):null;
			result.setJdbcSession(jdbcSession);
			return result;
		} catch (JdbcException e) {
			throw new SenderException("cannot get StatementSet",e);
		}
	}


	@Override
	public void closeBlock(QueryExecutionContext blockHandle, IPipeLineSession session) throws SenderException {
		try {
			super.closeStatementSet(blockHandle, session);
		} finally {
			try {
				if (blockHandle.getJdbcSession()!=null) {
					try {
						blockHandle.getJdbcSession().close();
					} catch (Exception e) {
						throw new SenderException(getLogPrefix() + "cannot return connection to repeatable read", e);
					}
				}
			} finally {
				try {
					closeConnectionForSendMessage(blockHandle.getConnection(), session);
				} catch (JdbcException | TimeOutException e) {
					throw new SenderException("cannot close connection", e);
				}
			}
		}
	}


	@Override
	protected QueryExecutionContext prepareStatementSet(QueryExecutionContext blockHandle, Connection connection, Message message, IPipeLineSession session) throws SenderException {
		return blockHandle;
	}

	@Override
	protected void closeStatementSet(QueryExecutionContext statementSet, IPipeLineSession session) {
		// postpone close to closeBlock()
	}

	@Override
	public Message sendMessage(QueryExecutionContext blockHandle, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		return executeStatementSet(blockHandle, message, session);
	}

	@Override
	protected final Message sendMessageOnConnection(Connection connection, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		throw new IllegalStateException("This method should not be used or overriden for this class. Override or use sendMessage(QueryExecutionContext,...)");
	}


	@IbisDoc({"1", "The SQL query text to be excecuted each time sendMessage() is called", ""})
	public void setQuery(String query) {
		this.query = query;
	}
	public String getQuery() {
		return query;
	}

	@IbisDoc({"2", "When set larger than 0 and used as a child of an IteratingPipe, then the database calls are made in batches of this size. Only for queryType=other.", "0"})
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
	@Override
	public int getBatchSize() {
		return batchSize;
	}

}
