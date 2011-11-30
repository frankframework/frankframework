/*
 * $Log: TibcoMessagingSourceFactory.java,v $
 * Revision 1.3  2011-11-30 13:51:58  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/01/28 14:49:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.2  2008/07/24 12:30:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for authenticated JMS
 *
 * Revision 1.1  2008/05/15 14:32:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.jms.MessagingSource;
import nl.nn.adapterframework.jms.JmsMessagingSourceFactory;

import com.tibco.tibjms.TibjmsQueueConnectionFactory;
import com.tibco.tibjms.TibjmsTopicConnectionFactory;


/**
 * Factory for {@link TibcoMessagingSource}s, to share them for Tibco Objects that can use the same. 
 * 
 * Tibco related IBIS objects can obtain a MessagingSource from this class. The physical connection is shared
 * between all IBIS objects that have the same (Tibco)connectionFactoryName.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class TibcoMessagingSourceFactory extends JmsMessagingSourceFactory {

	static private Map tibcoMessagingSourceMap = new HashMap();
	private boolean useTopic;
	
	protected Map getMessagingSourceMap() {
		return tibcoMessagingSourceMap;
	}

	public TibcoMessagingSourceFactory(boolean useTopic) {
		super();
		this.useTopic=useTopic;
	}

	protected MessagingSource createMessagingSource(String serverUrl, String authAlias) throws IbisException {
		ConnectionFactory connectionFactory = getConnectionFactory(null, serverUrl); 
		return new TibcoMessagingSource(serverUrl, null, connectionFactory, getMessagingSourceMap(),authAlias);
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
