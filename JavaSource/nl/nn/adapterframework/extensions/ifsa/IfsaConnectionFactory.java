/*
 * $Log: IfsaConnectionFactory.java,v $
 * Revision 1.6  2005-08-31 16:28:38  europe\L190409
 * report use of dynamic reply queues
 *
 * Revision 1.5  2005/07/28 07:30:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show XA status in log
 *
 * Revision 1.4  2005/07/19 12:33:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implements IXAEnabled 
 * polishing of serviceIds, to work around problems with ':' and '/'
 *
 * Revision 1.3  2005/06/20 09:13:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * dynamic determination of provider URL
 *
 * Revision 1.2  2005/06/13 12:30:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * ifsa 2.2 compatibilty connection
 *
 * Revision 1.1  2005/05/03 15:58:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework of shared connection code
 *
 * Revision 1.2  2005/04/26 15:16:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed most bugs
 *
 * Revision 1.1  2005/04/26 09:36:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IfsaApplicationConnection
 */
package nl.nn.adapterframework.extensions.ifsa;

import java.lang.reflect.Field;
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

import com.ing.ifsa.IFSAConstants;
import com.ing.ifsa.IFSAContext;
import com.ing.ifsa.IFSAGate;
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
	public static final String version="$RCSfile: IfsaConnectionFactory.java,v $ $Revision: 1.6 $ $Date: 2005-08-31 16:28:38 $";
	protected Logger log = Logger.getLogger(this.getClass());

	private final static String IFSA_INITIAL_CONTEXT_FACTORY="com.ing.ifsa.IFSAContextFactory";
	private final static String IFSA_PROVIDER_URL_V2_0="IFSA APPLICATION BUS";
	
	static private HashMap connectionMap = new HashMap();

	protected HashMap getConnectionMap() {
		return connectionMap;
	}

	private boolean preJms22Api=false;

	protected JmsConnection createJmsConnection(String id) throws IbisException {
		IFSAContext context = (IFSAContext)getContext();
		IFSAQueueConnectionFactory connectionFactory = (IFSAQueueConnectionFactory)getConnectionFactory(context, id); 
		return new IfsaConnection(id, context, connectionFactory, getConnectionMap(),preJms22Api);
	}

	protected String getProviderUrl() {
		String purl = IFSAConstants.IFSA_BAICNF;
		log.info("IFSA ProviderURL at time of compilation ["+purl+"]");
		try {
			Class clazz = Class.forName("com.ing.ifsa.IFSAConstants");
			Field baicnfField;
			try {
				baicnfField = clazz.getField("IFSA_BAICNF");
				Object baicnfFieldValue = baicnfField.get(null);
				log.info("IFSA ProviderURL specified by installed API ["+baicnfFieldValue+"]");
				purl = baicnfFieldValue.toString();
			} catch (NoSuchFieldException e1) {
				log.info("field [com.ing.ifsa.IFSAConstants.IFSA_BAICNF] not found, assuming IFSA Version 2.0");
				preJms22Api=true;
				purl = IFSA_PROVIDER_URL_V2_0;
			}
		} catch (Exception e) {
			log.warn("exception determining IFSA ProviderURL",e);
		}
		log.info("IFSA ProviderURL used to connect ["+purl+"]");
		return purl;
	}

	protected Context createContext() throws NamingException {
		log.info("IFSA API installed version ["+IFSAConstants.getVersionInfo()+"]");	
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, IFSA_INITIAL_CONTEXT_FACTORY);
		env.put(Context.PROVIDER_URL, getProviderUrl());
		// Create context as required by IFSA 2.0. Ignore the possible deprecation....
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
		if (!preJms22Api) {
			try {
				IFSAGate gate = IFSAGate.getInstance();
				log.info("IFSA JMS XA enabled ["+gate.isXA()+"]");        
				log.info("IFSA JMS hasDynamicReplyQueue: " +((IFSAContext)context).hasDynamicReplyQueue()); 
			} catch (Throwable t) {
				log.info("caught exception determining IfsaJms v2.2+ features:",t);
			}
		}
		return ifsaQueueConnectionFactory;
	}


}
