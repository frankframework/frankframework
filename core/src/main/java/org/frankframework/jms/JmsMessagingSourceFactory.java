/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020, 2021, 2024 WeAreFrank!

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
package org.frankframework.jms;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.JMSException;

import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IbisException;


/**
 * Factory for {@link JmsMessagingSource}s, to share them for JMS Objects that can use the same.
 * <p>
 * JMS related IBIS objects can obtain a MessagingSource from this class. The physical connection is shared
 * between all IBIS objects that have the same connectionFactoryName.
 *
 * @author  Gerrit van Brakel
 * @since   4.4
 */
public class JmsMessagingSourceFactory extends AbstractMessagingSourceFactory {

	/**
	 * Global JVM-wide cache for JMS Messaging Sources, which hold reference to ConnectionFactories.
	 */
	private static final Map<String,MessagingSource> JMS_MESSAGING_SOURCE_MAP = new HashMap<>();

	private final JMSFacade jmsFacade;

	public JmsMessagingSourceFactory(JMSFacade jmsFacade) {
		this.jmsFacade = jmsFacade;
	}

	@Override
	protected Map<String, MessagingSource> getMessagingSourceMap() {
		return JMS_MESSAGING_SOURCE_MAP;
	}

	@Override
	protected MessagingSource createMessagingSource(String jmsConnectionFactoryName, String authAlias, boolean createDestination) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory = getConnectionFactory(context, jmsConnectionFactoryName, createDestination);
		return new JmsMessagingSource(jmsConnectionFactoryName, jmsFacade.getJndiContextPrefix(), context, connectionFactory, getMessagingSourceMap(), authAlias, createDestination, jmsFacade.getProxiedDestinationNames());
	}

	@Override
	protected Context createContext() throws NamingException {
		return new InitialContext();
	}

	/**
	 * Removed the suggested wrap ConnectionFactory, to work around bug in JMSQueueConnectionFactoryHandle in combination with Spring.
	 * This was a bug in WAS 6.1 in combination with Spring 2.1, taking a risk here, but I'm assuming WebSphere has fixed this by now.
	 * see https://web.archive.org/web/20130510092515/http://forum.springsource.org/archive/index.php/t-43700.html
	 */
	@Override
	protected ConnectionFactory createConnectionFactory(Context context, String cfName, boolean createDestination) throws IbisException {
		IConnectionFactoryFactory connectionFactoryFactory = jmsFacade.getConnectionFactoryFactory();
		if (connectionFactoryFactory == null) {
			throw new ConfigurationException("No ConnectionFactoryFactory was configured");
		}

		ConnectionFactory connectionFactory;
		try {
			connectionFactory = connectionFactoryFactory.getConnectionFactory(cfName, jmsFacade.getJndiEnv());
		} catch (NamingException e) {
			throw new JmsException("Could not find connection factory ["+cfName+"]", e);
		}
		if (connectionFactory == null) {
			throw new JmsException("Could not find connection factory ["+cfName+"]");
		}

		if(log.isInfoEnabled()) {
			String connectionFactoryInfo = getConnectionFactoryInfo(connectionFactory);
			if (connectionFactoryInfo==null) {
				connectionFactoryInfo = connectionFactory.toString();
			}
			log.info("{}looked up connection factory [{}]: [{}]", jmsFacade.getLogPrefix(), cfName, connectionFactoryInfo);
		}
		return new TransactionAwareConnectionFactoryProxy(connectionFactory);
	}

	public String getConnectionFactoryInfo(ConnectionFactory connectionFactory) {
		String info=null;
		Connection connection = null;
		try {
			connection = connectionFactory.createConnection();
			ConnectionMetaData metaData = connection.getMetaData();
			info = "jms provider name [" + metaData.getJMSProviderName() + "] jms provider version [" + metaData.getProviderVersion() + "] jms version [" + metaData.getJMSVersion() + "]";
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
}
