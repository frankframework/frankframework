/*
 * $Log: JmsMessagingSourceFactory.java,v $
 * Revision 1.1  2010-01-28 14:48:42  L190409
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

import javax.jms.ConnectionFactory;
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
		return connectionFactory;
	}
}
