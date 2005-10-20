/*
 * $Log: JmsConnectionFactory.java,v $
 * Revision 1.1  2005-10-20 15:43:10  europe\L190409
 * introduced JmsConnectionFactory special for real Jms connections
 *
 */
package nl.nn.adapterframework.jms;

import java.util.HashMap;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.ConnectionFactoryBase;
import nl.nn.adapterframework.jms.ConnectionBase;


/**
 * Wrapper Jms connection objects.
 * 
 * JMS related IBIS objects can obtain an connection from this class. The physical connection is shared
 * between all IBIS objects that have the same Connection Factory Name.
 * 
 * @author  Gerrit van Brakel
 * @version Id
 * @since   4.4
 */
public class JmsConnectionFactory extends ConnectionFactoryBase {
	public static final String version="$RCSfile: JmsConnectionFactory.java,v $ $Revision: 1.1 $ $Date: 2005-10-20 15:43:10 $";

	static private HashMap connectionMap = new HashMap();
	
	protected HashMap getConnectionMap() {
		return connectionMap;
	}

	protected ConnectionBase createJmsConnection(String jmsConnectionFactoryName) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory = getConnectionFactory(context, jmsConnectionFactoryName); 
		return new JmsConnection(jmsConnectionFactoryName, context, connectionFactory, getConnectionMap());
	}

	protected Context createContext() throws NamingException {
		return (Context) new InitialContext();
	}

	protected ConnectionFactory createConnectionFactory(Context context, String cfName) throws IbisException, NamingException {
		ConnectionFactory connectionFactory = (ConnectionFactory) getContext().lookup(cfName);
		return connectionFactory;
	}
}
