/*
 * $Log: JmsConnection.java,v $
 * Revision 1.6  2008-05-15 14:45:45  europe\L190409
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

/**
 * Wrapper around JMS connection objects.
 * 
 * @author 	Gerrit van Brakel
 * @since   4.4
 * @version Id
 */
public class JmsConnection extends ConnectionBase {
	public static final String version="$RCSfile: JmsConnection.java,v $ $Revision: 1.6 $ $Date: 2008-05-15 14:45:45 $";
	
	public JmsConnection(String connectionFactoryName, Context context, ConnectionFactory connectionFactory, Map connectionMap) {
		super(connectionFactoryName, context, connectionFactory, connectionMap);
	}
	
	public Destination lookupDestination(String destinationName) throws JmsException, NamingException {
		Destination dest=null;
		dest=(Destination) getContext().lookup(destinationName);
		return dest;
	}
	
}
