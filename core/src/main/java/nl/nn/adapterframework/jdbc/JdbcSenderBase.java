/*
   Copyright 2013 Nationale-Nederlanden

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.IStreamingSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;

/**
 * Base class for building JDBC-senders.
 *
 * @author  Gerrit van Brakel
 * @since 	4.2.h
 */
public abstract class JdbcSenderBase extends JdbcFacade implements IStreamingSender {

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
	public void configure(ParameterList parameterList) throws ConfigurationException {
		configure();		
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getDatasourceName())) {
			throw new ConfigurationException(getLogPrefix()+"has no datasource");
		}
		if (paramList!=null) {
			paramList.configure();
		}
	}

	@Override
	public void open() throws SenderException {
		if (!isConnectionsArePooled()) {
			try {
				connection = getConnection();
			} catch (JdbcException e) {
				throw new SenderException(e);
			}
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
	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}

	@Override
	public final String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		Object result = sendMessage(correlationID, new Message(message), prc, null);
		try {
			return result==null?null:new Message(result).asString();
		} catch (IOException e) {
			throw new SenderException(e);
		}
	}

	@Override
	public Object sendMessage(String correlationID, Message message, ParameterResolutionContext prc, MessageOutputStream target) throws SenderException, TimeOutException {
		if (isConnectionsArePooled()) {
			Connection c = null;
			try {
				c = getConnectionWithTimeout(getTimeout());
				Object result = sendMessage(c, correlationID, message, prc, target);
				return result;
			} catch (JdbcException e) {
				throw new SenderException(e);
			} finally {
				if (c!=null) {
					try {
						c.close();
					} catch (SQLException e) {
						log.warn(new SenderException(getLogPrefix() + "caught exception closing sender after sending message, ID=["+correlationID+"]", e));
					}
				}
			}
			
		} 
		synchronized (connection) {
			return sendMessage(connection, correlationID, message, prc, target);
		}
	}

	protected abstract Object sendMessage(Connection connection, String correlationID, Message message, ParameterResolutionContext prc, MessageOutputStream target) throws SenderException, TimeOutException;

	@Override
	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        ts.append("name", getName() );
        result += ts.toString();
        return result;
	}

	public int getTimeout() {
		return timeout;
	}

	@IbisDoc({"the number of seconds the driver will wait for a statement object to execute. if the limit is exceeded, a timeoutexception is thrown. 0 means no timeout", "0"})
	public void setTimeout(int i) {
		timeout = i;
	}

}
