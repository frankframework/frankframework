/*
 * $Log: JmsConnection.java,v $
 * Revision 1.4  2005-10-20 15:42:10  europe\L190409
 * introduced JmsConnection special for real Jms connections
 *
 */
package nl.nn.adapterframework.jms;

import java.util.HashMap;

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
	public static final String version="$RCSfile: JmsConnection.java,v $ $Revision: 1.4 $ $Date: 2005-10-20 15:42:10 $";
	
	public JmsConnection(String connectionFactoryName, Context context, ConnectionFactory connectionFactory, HashMap connectionMap) {
		super(connectionFactoryName, context, connectionFactory, connectionMap);
	}
	
	public Destination lookupDestination(String destinationName) throws NamingException {
		Destination dest=null;
		dest=(Destination) getContext().lookup(destinationName);
		return dest;
	}
	
}
