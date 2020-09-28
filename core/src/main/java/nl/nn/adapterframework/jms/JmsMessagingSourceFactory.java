/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.util.AppConstants;

import org.apache.commons.lang.StringUtils;


/**
 * Factory for {@link JmsMessagingSource}s, to share them for JMS Objects that can use the same. 
 * 
 * JMS related IBIS objects can obtain a MessagingSource from this class. The physical connection is shared
 * between all IBIS objects that have the same connectionFactoryName.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4
 */
public class JmsMessagingSourceFactory extends MessagingSourceFactory {
	static private Map jmsMessagingSourceMap = new HashMap();
	private JMSFacade jmsFacade;
	private String applicationServerType = AppConstants.getInstance().getResolvedProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY);

	public JmsMessagingSourceFactory(JMSFacade jmsFacade) {
		this.jmsFacade = jmsFacade;
	}

	@Override
	protected Map getMessagingSourceMap() {
		return jmsMessagingSourceMap;
	}

	@Override
	protected MessagingSource createMessagingSource(String jmsConnectionFactoryName,
			String authAlias, boolean createDestination, boolean useJms102) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory =
				getConnectionFactory(context, jmsConnectionFactoryName, createDestination, useJms102); 
		return new JmsMessagingSource(jmsConnectionFactoryName,
				jmsFacade.getJndiContextPrefix(), context, connectionFactory,
				getMessagingSourceMap(), authAlias, createDestination,
				jmsFacade.getProxiedDestinationNames(), useJms102);
	}

	@Override
	protected Context createContext() throws NamingException {
		return (Context) new InitialContext();
	}

	@Override
	protected ConnectionFactory createConnectionFactory(Context context, String cfName, boolean createDestination, boolean useJms102) throws IbisException {
		ConnectionFactory connectionFactory;
		if (jmsFacade.getProxiedConnectionFactories() != null
				&& jmsFacade.getProxiedConnectionFactories().containsKey(cfName)) {
			log.debug(jmsFacade.getLogPrefix()+"looking up proxied connection factory ["+cfName+"]");
			connectionFactory = jmsFacade.getProxiedConnectionFactories().get(cfName);
		} else {
			String prefixedCfName=jmsFacade.getJndiContextPrefix()+cfName;
			log.debug(jmsFacade.getLogPrefix()+"looking up connection factory ["+prefixedCfName+"]");
			if (StringUtils.isNotEmpty(jmsFacade.getJndiContextPrefix())) {
				log.debug(jmsFacade.getLogPrefix()+"using JNDI context prefix ["+jmsFacade.getJndiContextPrefix()+"]");
			}
			try {
				connectionFactory = (ConnectionFactory)getContext().lookup(prefixedCfName);
			} catch (NamingException e) {
				throw new JmsException("Could not find connection factory ["+prefixedCfName+"]", e);
			}
		}
		if (connectionFactory == null) {
			throw new JmsException("Could not find connection factory ["+cfName+"]");
		}
		// wrap ConnectionFactory, to work around bug in JMSQueueConnectionFactoryHandle in combination with Spring
		// see http://forum.springsource.org/archive/index.php/t-43700.html
		if (jmsFacade.useJms102()) {
			if (connectionFactory instanceof QueueConnectionFactory) {
				connectionFactory = new QueueConnectionFactoryWrapper((QueueConnectionFactory)connectionFactory);
			} else if (connectionFactory instanceof TopicConnectionFactory) {
				connectionFactory = new TopicConnectionFactoryWrapper((TopicConnectionFactory)connectionFactory);
			}
		} else {
			connectionFactory = new ConnectionFactoryWrapper(connectionFactory);
		}
		String connectionFactoryInfo = getConnectionFactoryInfo(connectionFactory);
		if (connectionFactoryInfo==null) {
			connectionFactoryInfo = connectionFactory.toString();
		}
		log.info(jmsFacade.getLogPrefix()+"looked up connection factory ["+cfName+"]: ["+connectionFactoryInfo+"]");
		return connectionFactory;
	}

	public String getConnectionFactoryInfo(ConnectionFactory connectionFactory) {
		if ("TIBCOAMX".equals(applicationServerType)) {
			// Workaround to prevent the following exception:
			// [org.apache.geronimo.connector.outbound.MCFConnectionInterceptor] - Error occurred creating ManagedConnection for org.apache.geronimo.connector.outbound.ConnectionInfo@#######
			// javax.resource.ResourceException: JMSJCA-E084: Failed to create session: The JNDI name is null
			return null;
		}
		String info=null;
		Connection connection = null;
		try {
			connection = connectionFactory.createConnection();
			ConnectionMetaData metaData = connection.getMetaData();
			info = "jms provider name [" + metaData.getJMSProviderName()
					+ "] jms provider version [" + metaData.getProviderVersion()
					+ "] jms version [" + metaData.getJMSVersion()
					+ "]";
		} catch (JMSException e) {
			log.warn("Exception determining connection factory info",e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e1) {
					log.warn("Exception closing connection for metadata", e1);
				}
			}
		}
		return info;
	}

	private class ConnectionFactoryWrapper implements ConnectionFactory {
		private ConnectionFactory wrapped;

		public ConnectionFactoryWrapper(ConnectionFactory connectionFactory) {
			super();
			wrapped=connectionFactory;
		}

		@Override
		public Connection createConnection() throws JMSException {
			return wrapped.createConnection();
		}

		@Override
		public Connection createConnection(String arg0, String arg1) throws JMSException {
			return wrapped.createConnection(arg0,arg1);
		}
	}

	private class QueueConnectionFactoryWrapper implements QueueConnectionFactory {
		private QueueConnectionFactory wrapped;

		public QueueConnectionFactoryWrapper(QueueConnectionFactory connectionFactory) {
			super();
			wrapped=connectionFactory;
		}

		@Override
		public QueueConnection createQueueConnection() throws JMSException {
			return wrapped.createQueueConnection();
		}

		@Override
		public QueueConnection createQueueConnection(String arg0, String arg1) throws JMSException {
			return wrapped.createQueueConnection(arg0,arg1);
		}

		@Override
		public Connection createConnection() throws JMSException {
			return createQueueConnection();
		}

		@Override
		public Connection createConnection(String arg0, String arg1) throws JMSException {
			return createQueueConnection(arg0, arg1);
		}
	}

	private class TopicConnectionFactoryWrapper implements TopicConnectionFactory {
		private TopicConnectionFactory wrapped;

		public TopicConnectionFactoryWrapper(TopicConnectionFactory connectionFactory) {
			super();
			wrapped=connectionFactory;
		}

		@Override
		public TopicConnection createTopicConnection() throws JMSException {
			return wrapped.createTopicConnection();
		}

		@Override
		public TopicConnection createTopicConnection(String arg0, String arg1) throws JMSException {
			return wrapped.createTopicConnection(arg0,arg1);
		}

		@Override
		public Connection createConnection() throws JMSException {
			return createTopicConnection();
		}

		@Override
		public Connection createConnection(String arg0, String arg1) throws JMSException {
			return createTopicConnection(arg0, arg1);
		}
	}
}
