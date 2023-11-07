/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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

import org.apache.commons.lang3.builder.ToStringBuilder;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IBlockEnabledSender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.IStreamingSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Base class for building JDBC-senders.
 *
 * @author  Gerrit van Brakel
 * @since 	4.2.h
 */
public abstract class JdbcSenderBase<H> extends JdbcFacade implements IBlockEnabledSender<H>, IStreamingSender {

	private int timeout = 0;

	protected Connection connection=null;
	protected ParameterList paramList = null;

	public JdbcSenderBase() {
		super();
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (paramList!=null) {
			paramList.configure();
		}
	}

	@Override
	public void open() throws SenderException {
		try {
			connection = getConnection();
			connection.getMetaData(); //We have to perform some DB action, it could be stale or not present (yet)
		} catch (Throwable t) {
			JdbcUtil.close(connection);
			connection = null;

			throw new SenderException(t);
		}

		//When we use pooling connections we need to ask for a new connection every time we want to use it
		if (isConnectionsArePooled()) {
			close();
		}
	}

	@Override
	public void close() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			log.warn(getLogPrefix() + "caught exception stopping sender", e);
		} finally {
			connection = null;
			super.close();
		}
	}

	@Override
	// implements ISender.sendMessage()
	// can make this sendMessage() 'final', debugging handled by the newly implemented sendMessage() below, that includes the MessageOutputStream
	public final SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		H blockHandle = openBlock(session);
		try {
			return sendMessage(blockHandle, message, session);
		} finally {
			closeBlock(blockHandle, session);
		}
	}

	@Override
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("name", getName());
		result += ts.toString();
		return result;
	}

	/**
	 * The number of seconds the driver will wait for a statement object to execute. If the limit is exceeded, a TimeoutException is thrown. A value of 0 means execution time is not limited
	 * @ff.default 0
	 */
	public void setTimeout(int i) {
		timeout = i;
	}
	public int getTimeout() {
		return timeout;
	}

}
