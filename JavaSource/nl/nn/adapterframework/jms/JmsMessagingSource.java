/*
 * $Log: JmsMessagingSource.java,v $
 * Revision 1.2  2010-04-01 12:01:52  L190409
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
import javax.naming.Context;
import javax.naming.NamingException;

import nl.nn.adapterframework.util.ClassUtils;

/**
 * {@link MessagingSource} for JMS connections.
 * 
 * @author 	Gerrit van Brakel
 * @since   4.4
 * @version Id
 */
public class JmsMessagingSource extends MessagingSource {
	
	public JmsMessagingSource(String connectionFactoryName, Context context, ConnectionFactory connectionFactory, Map messagingSourceMap, String authAlias) {
		super(connectionFactoryName, context, connectionFactory, messagingSourceMap, authAlias);
	}
	
	public Destination lookupDestination(String destinationName) throws JmsException, NamingException {
		Destination dest=null;
		dest=(Destination) getContext().lookup(destinationName);
		return dest;
	}

	protected ConnectionFactory getConnectionFactoryDelegate() throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return (ConnectionFactory)ClassUtils.getDeclaredFieldValue(getConnectionFactory(),"wrapped");
	}
	
}
