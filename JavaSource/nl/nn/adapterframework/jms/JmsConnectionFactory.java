/*
 * $Log: JmsConnectionFactory.java,v $
 * Revision 1.2.4.1  2007-10-10 14:30:42  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
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
	public static final String version="$RCSfile: JmsConnectionFactory.java,v $ $Revision: 1.2.4.1 $ $Date: 2007-10-10 14:30:42 $";

	static private Map connectionMap = new HashMap();
	
	protected Map getConnectionMap() {
		return connectionMap;
	}

	protected ConnectionBase createConnection(String jmsConnectionFactoryName) throws IbisException {
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
