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
/*
 * $Log: JmsMessagingSource.java,v $
 * Revision 1.5  2012-09-07 13:15:17  m00f069
 * Messaging related changes:
 * - Use CACHE_CONSUMER by default for ESB RR
 * - Don't use JMSXDeliveryCount to determine whether message has already been processed
 * - Added maxDeliveries
 * - Delay wasn't increased when unable to write to error store (it was reset on every new try)
 * - Don't call session.rollback() when isTransacted() (it was also called in afterMessageProcessed when message was moved to error store)
 * - Some cleaning along the way like making some synchronized statements unnecessary
 * - Made BTM and ActiveMQ work for testing purposes
 *
 * Revision 1.4  2011/11/30 13:51:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2010/04/01 12:01:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed getConnectionFactoryDelegate()
 *
 * Revision 1.1  2010/01/28 14:48:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed 'Connection' classes to 'MessageSource'
 *
 * Revision 1.7  2008/07/24 12:20:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for authenticated JMS
 *
 * Revision 1.6  2008/05/15 14:45:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to throw more exceptions in lookupDestination
 *
 * Revision 1.5  2007/10/08 12:20:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.4  2005/10/20 15:42:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced JmsConnection special for real Jms connections
 *
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
