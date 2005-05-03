/*
 * $Log: IfsaConnectionFactory.java,v $
 * Revision 1.1  2005-05-03 15:58:50  L190409
 * rework of shared connection code
 *
 * Revision 1.2  2005/04/26 15:16:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed most bugs
 *
 * Revision 1.1  2005/04/26 09:36:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IfsaApplicationConnection
 */
package nl.nn.adapterframework.extensions.ifsa;

import java.util.HashMap;
import java.util.Hashtable;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.ConnectionFactoryBase;
import nl.nn.adapterframework.jms.JmsConnection;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import com.ing.ifsa.IFSAContext;
import com.ing.ifsa.IFSAQueueConnectionFactory;

/**
 * Wrapper around Application oriented IFSA connection objects.
 * 
 * IFSA related IBIS objects can obtain an connection from this class. The physical connection is shared
 * between all IBIS objects that have the same ApplicationID.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class IfsaConnectionFactory extends ConnectionFactoryBase {
	public static final String version="$Id: IfsaConnectionFactory.java,v 1.1 2005-05-03 15:58:50 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	private final static String IFSA_INITIAL_CONTEXT_FACTORY="com.ing.ifsa.IFSAContextFactory";
	private final static String IFSA_PROVIDER_URL="IFSA APPLICATION BUS";
	
	static private HashMap connectionMap = new HashMap();

	protected HashMap getConnectionMap() {
		return connectionMap;
	}

	protected JmsConnection createJmsConnection(String id) throws IbisException {
		IFSAContext context = (IFSAContext)getContext();
		IFSAQueueConnectionFactory connectionFactory = (IFSAQueueConnectionFactory)getConnectionFactory(context, id); 
		return new IfsaConnection(id, context, connectionFactory, getConnectionMap());
	}

	protected Context createContext() throws NamingException {
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, IFSA_INITIAL_CONTEXT_FACTORY);
		env.put(Context.PROVIDER_URL, IFSA_PROVIDER_URL);
		// Create context as required by IFSA 2.0. Ignore the deprecation....
		return new IFSAContext((Context) new InitialContext(env));
	}

	protected ConnectionFactory createConnectionFactory(Context context, String applicationId) throws IbisException, NamingException {
		IFSAQueueConnectionFactory ifsaQueueConnectionFactory = (IFSAQueueConnectionFactory) ((IFSAContext)context).lookupBusConnection(applicationId);
		if (log.isDebugEnabled()) {
			log.debug("IfsaConnection for application ["+applicationId+"] got ifsaQueueConnectionFactory with properties:" 
				+ ToStringBuilder.reflectionToString(ifsaQueueConnectionFactory) +"\n" 
				+ " isServer: " +ifsaQueueConnectionFactory.IsServer()+"\n"  
				+ " isClientNonTransactional:" +ifsaQueueConnectionFactory.IsClientNonTransactional()+"\n" 
				+ " isClientTransactional:" +ifsaQueueConnectionFactory.IsClientTransactional()+"\n" 
				+ " isClientServerNonTransactional:" +ifsaQueueConnectionFactory.IsClientServerNonTransactional()+"\n" 
			+ " isServerTransactional:" +ifsaQueueConnectionFactory.IsClientServerTransactional()+"\n" );
		}        
		return ifsaQueueConnectionFactory;
	}


}
