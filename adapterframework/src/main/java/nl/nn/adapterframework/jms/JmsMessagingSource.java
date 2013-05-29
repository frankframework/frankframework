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

import org.apache.commons.lang.StringUtils;

/**
 * {@link MessagingSource} for JMS connections.
 * 
 * @author 	Gerrit van Brakel
 * @since   4.4
 * @version $Id$
 */
public class JmsMessagingSource extends MessagingSource {
	String jndiContextPrefix;
	
	public JmsMessagingSource(String connectionFactoryName,
			String jndiContextPrefix, Context context,
			ConnectionFactory connectionFactory, Map messagingSourceMap,
			String authAlias, boolean createDestination, boolean useJms102) {
		super(connectionFactoryName, context,
				connectionFactory, messagingSourceMap, authAlias,
				createDestination, useJms102);
		this.jndiContextPrefix = jndiContextPrefix;
	}
	
	public Destination lookupDestination(String destinationName) throws JmsException, NamingException {
		Destination dest=null;
		if (createDestination()) {
			Session session = null;
			log.debug(getLogPrefix() + "looking up destination by creating it [" + destinationName + "]");
			try {
				session = createSession(false,Session.AUTO_ACKNOWLEDGE);
				dest = session.createQueue(destinationName);
			} catch (Exception e) {
				throw new JmsException("cannot create destination", e);
			} finally {
				releaseSession(session);
			}
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

	protected ConnectionFactory getConnectionFactoryDelegate() throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return (ConnectionFactory)ClassUtils.getDeclaredFieldValue(getConnectionFactory(),"wrapped");
	}

	private String getJndiContextPrefix() {
		return jndiContextPrefix;
	}

}
