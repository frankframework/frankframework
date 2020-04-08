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
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
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
public class FixedQuerySender extends JdbcQuerySenderBase<QueryContext> {

	private String query=null;

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

	/**
	 * Sets the SQL-query text to be executed each time sendMessage() is called.
	 */
	@IbisDoc({"the sql query text to be excecuted each time sendmessage() is called", ""})
	public void setQuery(String query) {
		this.query = query;
	}
	public String getQuery() {
		return query;
	}

	@Override
	public boolean canProvideOutputStream() {
		return  "updateClob".equalsIgnoreCase(getQueryType()) && StringUtils.isEmpty(getClobSessionKey()) ||
				"updateBlob".equalsIgnoreCase(getQueryType()) && StringUtils.isEmpty(getBlobSessionKey());
	}


	@Override
	public QueryContext openBlock(IPipeLineSession session) throws SenderException, TimeOutException {
		try {
			Connection connection = getConnectionForSendMessage(null);
			return super.prepareStatementSet(null, connection, null, session);
		} catch (JdbcException e) {
			throw new SenderException("cannot get StatementSet",e);
		}
	}


	@Override
	public void closeBlock(QueryContext blockHandle, IPipeLineSession session) throws SenderException {
		Connection connection=null;
		try {
			connection = blockHandle.getStatement().getConnection();
			super.closeStatementSet(blockHandle, session);
		} catch (SQLException e) {
			throw new SenderException("cannot close StatementSet",e);
		} finally {
			try {
				closeConnectionForSendMessage(connection, session);
			} catch (JdbcException | TimeOutException e) {
				throw new SenderException("cannot close connection", e);
			}
		}
	}

	@Override
	protected void closeConnectionForSendMessage(Connection connection, IPipeLineSession session) throws JdbcException, TimeOutException {
		// postpone close to closeBlock()
	}

	@Override
	protected QueryContext prepareStatementSet(QueryContext blockHandle, Connection connection, Message message, IPipeLineSession session) throws SenderException {
		return blockHandle;
	}

	@Override
	protected void closeStatementSet(QueryContext statementSet, IPipeLineSession session) {
		// postpone close to closeBlock()
	}

	@Override
	public Message sendMessage(QueryContext blockHandle, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		return Message.asMessage(executeStatementSet(blockHandle, message, session));
	}

	@Override
	public PipeRunResult sendMessage(QueryContext blockHandle, Message message, IPipeLineSession session, IOutputStreamingSupport next) throws SenderException, TimeOutException {
		return new PipeRunResult(null, executeStatementSet(blockHandle, message, session));
	}

	@Override
	protected final String sendMessageOnConnection(Connection connection, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		throw new IllegalStateException("This method should not be used or overriden for this class. Override or use sendMessage(QueryContext,...)");
	}

}
