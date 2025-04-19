/*
   Copyright 2013 Nationale-Nederlanden, 2024-2025 WeAreFrank!

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

import jakarta.jms.ConnectionFactory;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IbisException;

/**
 * Factory for {@link MessagingSource}s, to share them for JMS Objects that can use the same.
 *
 * @author Gerrit van Brakel
 */
@Log4j2
public class JmsMessagingSourceFactory {
	private final JMSFacade jmsFacade;
	/**
	 * Global JVM-wide cache for JMS Messaging Sources, which hold reference to ConnectionFactories.
	 */
	private static final Map<String, MessagingSource> JMS_MESSAGING_SOURCE_MAP = new HashMap<>();

	public JmsMessagingSourceFactory(JMSFacade jmsFacade) {
		this.jmsFacade = jmsFacade;
	}

	private JmsMessagingSource createMessagingSource(String connectionFactoryName, String authAlias, boolean createDestination) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory = getConnectionFactory(connectionFactoryName);
		return new JmsMessagingSource(connectionFactoryName, jmsFacade.getJndiContextPrefix(), context, connectionFactory, JMS_MESSAGING_SOURCE_MAP, authAlias, createDestination, jmsFacade.getProxiedDestinationNames());
	}

	public synchronized MessagingSource getMessagingSource(String connectionFactoryName, String authAlias, boolean createDestination) throws IbisException {
		MessagingSource result = JMS_MESSAGING_SOURCE_MAP.get(connectionFactoryName);
		if (result == null) {
			result = createMessagingSource(connectionFactoryName, authAlias, createDestination);
			log.debug("created new MessagingSource-object for [{}]", connectionFactoryName);
		}
		result.increaseReferences();
		return result;
	}

	private Context getContext() throws IbisException {
		try {
			return new InitialContext();
		} catch (Throwable t) {
			throw new IbisException("could not obtain context", t);
		}
	}

	private ConnectionFactory getConnectionFactory(String cfName) throws IbisException {
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

		return connectionFactory;
	}
}
