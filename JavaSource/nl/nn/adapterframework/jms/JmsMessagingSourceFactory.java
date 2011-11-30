/*
 * $Log: JmsMessagingSourceFactory.java,v $
 * Revision 1.4  2011-11-30 13:51:51  europe\m168309
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
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;


/**
 * Factory for {@link JmsMessagingSource}s, to share them for JMS Objects that can use the same. 
 * 
 * JMS related IBIS objects can obtain a MessagingSource from this class. The physical connection is shared
 * between all IBIS objects that have the same connectionFactoryName.
 * 
 * @author  Gerrit van Brakel
 * @version Id
 * @since   4.4
 */
public class JmsMessagingSourceFactory extends MessagingSourceFactory {

	static private Map jmsMessagingSourceMap = new HashMap();
	
	protected Map getMessagingSourceMap() {
		return jmsMessagingSourceMap;
	}

	protected MessagingSource createMessagingSource(String jmsConnectionFactoryName, String authAlias) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory = getConnectionFactory(context, jmsConnectionFactoryName); 
		return new JmsMessagingSource(jmsConnectionFactoryName, context, connectionFactory, getMessagingSourceMap(), authAlias);
	}

	protected Context createContext() throws NamingException {
		return (Context) new InitialContext();
	}

	protected ConnectionFactory createConnectionFactory(Context context, String cfName) throws IbisException, NamingException {
		ConnectionFactory connectionFactory = (ConnectionFactory) getContext().lookup(cfName);
		// wrap ConnectionFactory, to work around bug in JMSQueueConnectionFactoryHandle in combination with Spring
		// see http://forum.springsource.org/archive/index.php/t-43700.html
		if (connectionFactory instanceof QueueConnectionFactory) {
			connectionFactory = new QueueConnectionFactoryWrapper((QueueConnectionFactory)connectionFactory);
		} else if (connectionFactory instanceof TopicConnectionFactory) {
			connectionFactory = new TopicConnectionFactoryWrapper((TopicConnectionFactory)connectionFactory);
		}
		return connectionFactory;
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
			return wrapped.createConnection();
		}

		public Connection createConnection(String arg0, String arg1) throws JMSException {
			return wrapped.createConnection(arg0, arg1);
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
			return wrapped.createConnection();
		}

		public Connection createConnection(String arg0, String arg1) throws JMSException {
			return wrapped.createConnection(arg0, arg1);
		}
}

}
