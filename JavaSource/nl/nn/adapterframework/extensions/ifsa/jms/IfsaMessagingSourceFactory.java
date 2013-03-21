/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: IfsaMessagingSourceFactory.java,v $
 * Revision 1.4  2012-09-07 13:15:16  m00f069
 * Messaging related changes:
 * - Use CACHE_CONSUMER by default for ESB RR
 * - Don't use JMSXDeliveryCount to determine whether message has already been processed
 * - Added maxDeliveries
 * - Delay wasn't increased when unable to write to error store (it was reset on every new try)
 * - Don't call session.rollback() when isTransacted() (it was also called in afterMessageProcessed when message was moved to error store)
 * - Some cleaning along the way like making some synchronized statements unnecessary
 * - Made BTM and ActiveMQ work for testing purposes
 *
 * Revision 1.3  2011/11/30 13:51:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/01/28 14:49:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed 'Connection' classes to 'MessageSource'
 *
 * Revision 1.2  2008/07/24 12:26:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for authenticated JMS
 *
 * Revision 1.1  2007/10/16 08:15:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced switch class for jms and ejb
 *
 * Revision 1.13  2007/10/08 12:17:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.12  2007/09/05 15:46:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved XA determination capabilities to IfsaConnection
 *
 * Revision 1.11  2007/08/10 11:08:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added functions to check XA capabilities
 *
 * Revision 1.10  2007/02/21 15:58:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add message for XA under v2.0
 *
 * Revision 1.9  2005/12/28 08:48:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.8  2005/10/26 08:21:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed createJmsConnection() into createConnection()
 *
 * Revision 1.7  2005/10/20 15:34:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed JmsConnection into ConnectionBase
 *
 * Revision 1.6  2005/08/31 16:28:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
package nl.nn.adapterframework.extensions.ifsa.jms;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.MessagingSource;
import nl.nn.adapterframework.jms.MessagingSourceFactory;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.ing.ifsa.IFSAConstants;
import com.ing.ifsa.IFSAContext;
import com.ing.ifsa.IFSAGate;
import com.ing.ifsa.IFSAQueueConnectionFactory;

/**
 * Factory for {@link IfsaMessagingSource}s, to share them for IFSA Objects that can use the same. 
 * 
 * IFSA related IBIS objects can obtain a MessagingSource from this class. The physical connection is shared
 * between all IBIS objects that have the same ApplicationID.
 * 
 * @author Gerrit van Brakel
 * @version $Id$
 */
public class IfsaMessagingSourceFactory extends MessagingSourceFactory {

	private final static String IFSA_INITIAL_CONTEXT_FACTORY="com.ing.ifsa.IFSAContextFactory";
	private final static String IFSA_PROVIDER_URL_V2_0="IFSA APPLICATION BUS";
	
	static private Map ifsaMessagingSourceMap = new HashMap();	

	protected Map getMessagingSourceMap() {
		return ifsaMessagingSourceMap;
	}

	// the following two booleans are only valid if an IFSAQueueConnectionFactory has been created
	// using createConnectionFactory() 
	private boolean preJms22Api=false; 
	private boolean xaEnabled=false;

	protected MessagingSource createMessagingSource(String id, String authAlias, boolean createDestination, boolean useJms102) throws IbisException {
		IFSAContext context = (IFSAContext)getContext();
		IFSAQueueConnectionFactory connectionFactory = (IFSAQueueConnectionFactory)getConnectionFactory(context, id, createDestination, useJms102); 
		return new IfsaMessagingSource(id, context, connectionFactory, getMessagingSourceMap(),preJms22Api, xaEnabled);
	}

	public synchronized MessagingSource getConnection(String id) throws IbisException {
		return super.getMessagingSource(id,null,false,true);
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

	protected ConnectionFactory createConnectionFactory(Context context, String applicationId, boolean createDestination, boolean useJms102) throws IbisException, NamingException {
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
				xaEnabled=gate.isXA();
				log.info("IFSA JMS XA enabled ["+xaEnabled+"]");        
				log.info("IFSA JMS hasDynamicReplyQueue: " +((IFSAContext)context).hasDynamicReplyQueue()); 
			} catch (Throwable t) {
				log.info("caught exception determining IfsaJms v2.2+ features:",t);
			}
		} else {
			log.info("for IFSA JMS versions prior to 2.2 capability of XA support cannot be determined");
		}
		return ifsaQueueConnectionFactory;
	}


}
