/*
 * $Log: TibcoConnectionFactory.java,v $
 * Revision 1.1  2008-05-15 14:32:58  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.extensions.tibco;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.ConnectionBase;
import nl.nn.adapterframework.jms.JmsConnectionFactory;

import com.tibco.tibjms.TibjmsQueueConnectionFactory;
import com.tibco.tibjms.TibjmsTopicConnectionFactory;


/**
 * Tibco Connection factory.
 * 
 * JMS related IBIS objects can obtain an connection from this class. The physical connection is shared
 * between all IBIS objects that have the same Connection Factory Name.
 * 
 * @author  Gerrit van Brakel
 * @version Id
 * @since   4.9
 */
public class TibcoConnectionFactory extends JmsConnectionFactory {
	public static final String version="$RCSfile: TibcoConnectionFactory.java,v $ $Revision: 1.1 $ $Date: 2008-05-15 14:32:58 $";

	static private Map connectionMap = new HashMap();
	private boolean useTopic;
	
	protected Map getConnectionMap() {
		return connectionMap;
	}

	public TibcoConnectionFactory(boolean useTopic) {
		super();
		this.useTopic=useTopic;
	}

	protected ConnectionBase createConnection(String serverUrl) throws IbisException {
		ConnectionFactory connectionFactory = getConnectionFactory(null, serverUrl); 
		return new TibcoConnection(serverUrl, null, connectionFactory, getConnectionMap());
	}

	protected Context createContext() throws NamingException {
		return null;
	}

	protected ConnectionFactory createConnectionFactory(Context context, String serverUrl) throws IbisException, NamingException {
		ConnectionFactory connectionFactory;
		
		if (useTopic) {
			connectionFactory = new TibjmsTopicConnectionFactory(serverUrl);
		} else {
			connectionFactory = new TibjmsQueueConnectionFactory(serverUrl);
		}
		return connectionFactory;
	}
}
