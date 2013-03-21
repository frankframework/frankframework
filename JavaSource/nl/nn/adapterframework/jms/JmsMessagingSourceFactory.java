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
 * $Log: JmsMessagingSourceFactory.java,v $
 * Revision 1.6  2012-09-07 13:15:17  m00f069
 * Messaging related changes:
 * - Use CACHE_CONSUMER by default for ESB RR
 * - Don't use JMSXDeliveryCount to determine whether message has already been processed
 * - Added maxDeliveries
 * - Delay wasn't increased when unable to write to error store (it was reset on every new try)
 * - Don't call session.rollback() when isTransacted() (it was also called in afterMessageProcessed when message was moved to error store)
 * - Some cleaning along the way like making some synchronized statements unnecessary
 * - Made BTM and ActiveMQ work for testing purposes
 *
 * Revision 1.5  2011/12/05 15:33:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * JMS 1.03 compatibilty restored
 *
 * Revision 1.4  2011/11/30 13:51:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2010/03/10 14:20:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * wrapped connectionfactories, to work around bug in IBM implementation of 
 * QueueConnectionFactory, that shows up when SSL is used in combination with Spring
 *
 * Revision 1.1  2010/01/28 14:48:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed 'Connection' classes to 'MessageSource'
 *
 * Revision 1.4  2008/07/24 12:20:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for authenticated JMS
 *
 * Revision 1.3  2007/10/08 12:20:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.2  2005/10/26 08:21:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed createJmsConnection() into createConnection()
 *
 * Revision 1.1  2005/10/20 15:43:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced JmsConnectionFactory special for real Jms connections
 *
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

import org.apache.commons.lang.StringUtils;


/**
 * Factory for {@link JmsMessagingSource}s, to share them for JMS Objects that can use the same. 
 * 
 * JMS related IBIS objects can obtain a MessagingSource from this class. The physical connection is shared
 * between all IBIS objects that have the same connectionFactoryName.
 * 
 * @author  Gerrit van Brakel
 * @version $Id$
 * @since   4.4
 */
public class JmsMessagingSourceFactory extends MessagingSourceFactory {
	static private Map jmsMessagingSourceMap = new HashMap();
	private JMSFacade jmsFacade;

	public JmsMessagingSourceFactory(JMSFacade jmsFacade) {
		this.jmsFacade = jmsFacade;
	}

	protected Map getMessagingSourceMap() {
		return jmsMessagingSourceMap;
	}

	protected MessagingSource createMessagingSource(String jmsConnectionFactoryName, String authAlias, boolean createDestination, boolean useJms102) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory = getConnectionFactory(context, jmsConnectionFactoryName, createDestination, useJms102); 
		return new JmsMessagingSource(jmsConnectionFactoryName, jmsFacade.getJndiContextPrefix(), context, connectionFactory, getMessagingSourceMap(), authAlias, createDestination, useJms102);
	}

	protected Context createContext() throws NamingException {
		return (Context) new InitialContext();
	}

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

		public Connection createConnection() throws JMSException {
			return wrapped.createConnection();
		}

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

		public QueueConnection createQueueConnection() throws JMSException {
			return wrapped.createQueueConnection();
		}

		public QueueConnection createQueueConnection(String arg0, String arg1) throws JMSException {
			return wrapped.createQueueConnection(arg0,arg1);
		}

		public Connection createConnection() throws JMSException {
			return createQueueConnection();
		}

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

		public TopicConnection createTopicConnection() throws JMSException {
			return wrapped.createTopicConnection();
		}

		public TopicConnection createTopicConnection(String arg0, String arg1) throws JMSException {
			return wrapped.createTopicConnection(arg0,arg1);
		}

		public Connection createConnection() throws JMSException {
			return createTopicConnection();
		}

		public Connection createConnection(String arg0, String arg1) throws JMSException {
			return createTopicConnection(arg0, arg1);
		}
	}
}
