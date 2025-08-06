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
package org.frankframework.jms;

import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.Session;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.jdbc.datasource.IObjectLocator;

/**
 * {@link MessagingSource} for JMS connections.
 *
 * @author 	Gerrit van Brakel
 * @since   4.4
 */
public class JmsMessagingSource extends MessagingSource {
	private final String jndiContextPrefix;
	private final Map<String, String> proxiedDestinationNames;

	public JmsMessagingSource(String connectionFactoryName, String jndiContextPrefix, Context context,
			ConnectionFactory connectionFactory, Map<String,MessagingSource> messagingSourceMap,
			String authAlias, boolean createDestination, Map<String, String> proxiedDestinationNames) {
		super(connectionFactoryName, context, connectionFactory, messagingSourceMap, authAlias, createDestination);
		this.jndiContextPrefix = jndiContextPrefix;
		this.proxiedDestinationNames = proxiedDestinationNames;
	}

	/**
	 * Misleading name, it attempts to lookup the destination in the JNDI, but falls back to
	 * {@link #createDestination(String)} if {@link #createDestination()} ({@code jms.createDestination}) is {@code true}.
	 */
	public Destination lookupDestination(String destinationName) throws JmsException, NamingException {
		Destination dest=null;
		if (createDestination()) {
			try {
				dest = lookupDestinationInJndi(destinationName);
			} catch (Exception e) {
				log.warn("could not lookup destination in jndi, will try to create it ({}): {}", e.getClass(), e.getMessage());
			}
			if (dest==null) {
				log.debug("{}looking up destination by creating it [{}]", getLogPrefix(), destinationName);
				if (proxiedDestinationNames != null) {
					String proxiedDestinationName = proxiedDestinationNames.get(destinationName);
					if (proxiedDestinationName != null) {
						log.debug("{}replacing destination name with proxied destination name [{}]", getLogPrefix(), proxiedDestinationName);
						destinationName = proxiedDestinationName;
					}
				}
				dest = createDestination(destinationName);
			}
		} else {
			dest = lookupDestinationInJndi(destinationName);
		}
		return dest;
	}

	/**
	 * We could make it so you can fetch these dynamically through an {@link IObjectLocator}.
	 * In our tests we always 'assume' the client can create destinations, or already know the destination.
	 * In practice this 'used' to be nice because it allowed system administrators to dictate which queues should be used.
	 *
	 * See issue 8851.
	 *
	 * @deprecated Now that most deployments are containerized as well as the fact that the same result can be achieved
	 * with a simple environment variable, the need for this has become obsolete.
	 */
	@Deprecated
	private Destination lookupDestinationInJndi(String destinationName) throws NamingException {
		String prefixedDestinationName = getJndiContextPrefix() + destinationName;
		log.debug("{}looking up destination [{}]", getLogPrefix(), prefixedDestinationName);
		if (StringUtils.isNotEmpty(getJndiContextPrefix())) {
			log.debug("{}using JNDI context prefix [{}]", getLogPrefix(), getJndiContextPrefix());
		}
		return (Destination)getContext().lookup(prefixedDestinationName);
	}

	/**
	 * Get or create the JMS destination.
	 */
	public Destination createDestination(String destinationName) throws JmsException {
		Destination dest;
		Session session = null;
		try {
			session = createSession(false, Session.AUTO_ACKNOWLEDGE);
			dest = session.createQueue(destinationName);
		} catch (Exception e) {
			throw new JmsException("cannot create destination ["+destinationName+"]", e);
		} finally {
			releaseSession(session);
		}
		return dest;
	}

	private String getJndiContextPrefix() {
		return jndiContextPrefix;
	}

}
