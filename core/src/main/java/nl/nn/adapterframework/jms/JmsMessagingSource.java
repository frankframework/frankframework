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
package nl.nn.adapterframework.jms;

import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;

import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * {@link MessagingSource} for JMS connections.
 * 
 * @author 	Gerrit van Brakel
 * @since   4.4
 */
public class JmsMessagingSource extends MessagingSource {
	private String jndiContextPrefix;
	private Map<String, String> proxiedDestinationNames;

	public JmsMessagingSource(String connectionFactoryName, String jndiContextPrefix, Context context,
			ConnectionFactory connectionFactory, Map<String,MessagingSource> messagingSourceMap,
			String authAlias, boolean createDestination, Map<String, String> proxiedDestinationNames, boolean useJms102) {
		super(connectionFactoryName, context, connectionFactory, messagingSourceMap, authAlias, createDestination, useJms102);
		this.jndiContextPrefix = jndiContextPrefix;
		this.proxiedDestinationNames = proxiedDestinationNames;
	}

	public Destination lookupDestination(String destinationName) throws JmsException, NamingException {
		Destination dest=null;
		if (createDestination()) {
			log.debug(getLogPrefix() + "looking up destination by creating it [" + destinationName + "]");
			if (proxiedDestinationNames != null) {
				String proxiedDestinationName = proxiedDestinationNames.get(destinationName);
				if (proxiedDestinationName != null) {
					log.debug(getLogPrefix() + "replacing destination name with proxied destination name [" + proxiedDestinationName + "]");
					destinationName = proxiedDestinationName;
				}
			}
			dest = createDestination(destinationName);
		} else {
			String prefixedDestinationName = getJndiContextPrefix() + destinationName;
			log.debug(getLogPrefix() + "looking up destination [" + prefixedDestinationName + "]");
			if (StringUtils.isNotEmpty(getJndiContextPrefix())) {
				log.debug(getLogPrefix() + "using JNDI context prefix [" + getJndiContextPrefix() + "]");
			}
			dest = (Destination)getContext().lookup(prefixedDestinationName);
		}
		return dest;
	}

	public Destination createDestination(String destinationName) throws JmsException {
		Destination dest = null;
		Session session = null;
		try {
			session = createSession(false, Session.AUTO_ACKNOWLEDGE);
			dest = session.createQueue(destinationName);
		} catch (Exception e) {
			throw new JmsException("cannot create destination", e);
		} finally {
			releaseSession(session);
		}
		return dest;
	}

	@Override
	protected ConnectionFactory getConnectionFactoryDelegate() throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return (ConnectionFactory)ClassUtils.getDeclaredFieldValue(getConnectionFactory(),"wrapped");
	}

	private String getJndiContextPrefix() {
		return jndiContextPrefix;
	}

}
