/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020 WeAreFrank!

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * QuerySender that interprets the input message as a query, possibly with attributes.
 * Messages are expected to contain sql-text.
 *
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>all parameters present are applied to the statement to be executed</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class DirectQuerySender extends JdbcQuerySenderBase<Connection>{

	@Override
	public void configure() throws ConfigurationException {
		configure(false);
	}

	public void configure(boolean trust) throws ConfigurationException {
		super.configure();
		if (!trust) {
			ConfigurationWarnings.add(this, log, "The class ["+ClassUtils.nameOf(this)+"] is used one or more times. This may cause potential SQL injections!");
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
	public Connection openBlock(IPipeLineSession session) throws SenderException, TimeOutException {
		try {
			return super.getConnectionForSendMessage(null);
		} catch (JdbcException e) {
			throw new SenderException("cannot get Connection",e);
		}
	}

	@Override
	public void closeBlock(Connection connection, IPipeLineSession session) throws SenderException {
		try {
			super.closeConnectionForSendMessage(connection, session);
		} catch (JdbcException | TimeOutException e) {
			throw new SenderException("cannot close Connection",e);
		}
	}
	
	@Override
	protected Connection getConnectionForSendMessage(Connection blockHandle) throws JdbcException, TimeOutException {
		return blockHandle;
	}

	@Override
	protected void closeConnectionForSendMessage(Connection connection, IPipeLineSession session) throws JdbcException, TimeOutException {
		// postpone close to closeBlock()
	}


	@Override
	// implements IBlockEnabledSender.sendMessage()
	public Message sendMessage(Connection blockHandle, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		return sendMessageOnConnection(blockHandle, message, session, null).getResult();
	}

	@Override
	// implements IStreamingSender.sendMessage()
	public PipeRunResult sendMessage(Message message, IPipeLineSession session, IForwardTarget next) throws SenderException, TimeOutException {
		Connection blockHandle = openBlock(session);
		try {
			return sendMessageOnConnection(blockHandle, message, session, next);
		} finally {
			closeBlock(blockHandle, session);
		}
	}

}
