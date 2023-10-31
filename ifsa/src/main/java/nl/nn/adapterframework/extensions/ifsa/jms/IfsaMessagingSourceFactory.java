/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
package nl.nn.adapterframework.extensions.ifsa.jms;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ing.ifsa.IFSAConstants;
import com.ing.ifsa.IFSAContext;
import com.ing.ifsa.IFSAGate;
import com.ing.ifsa.IFSAQueueConnectionFactory;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.MessagingSource;
import nl.nn.adapterframework.jms.MessagingSourceFactory;

/**
 * Factory for {@link IfsaMessagingSource}s, to share them for IFSA Objects that can use the same.
 *
 * IFSA related IBIS objects can obtain a MessagingSource from this class. The physical connection is shared
 * between all IBIS objects that have the same ApplicationID.
 *
 * @author Gerrit van Brakel
 */
public class IfsaMessagingSourceFactory extends MessagingSourceFactory {

	private static final String IFSA_INITIAL_CONTEXT_FACTORY="com.ing.ifsa.IFSAContextFactory";
	private static final String IFSA_PROVIDER_URL_V2_0="IFSA APPLICATION BUS";
	private static final Map<String, MessagingSource> IFSA_MESSAGING_SOURCE_MAP = new HashMap<>();

	@Override
	protected Map<String, MessagingSource> getMessagingSourceMap() {
		return IFSA_MESSAGING_SOURCE_MAP;
	}

	// the following two booleans are only valid if an IFSAQueueConnectionFactory has been created
	// using createConnectionFactory()
	private boolean preJms22Api=false;
	private boolean xaEnabled=false;

	@Override
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
			Class<?> clazz = Class.forName("com.ing.ifsa.IFSAConstants");
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

	@Override
	protected Context createContext() throws NamingException {
		log.info("IFSA API installed version ["+IFSAConstants.getVersionInfo()+"]");
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, IFSA_INITIAL_CONTEXT_FACTORY);
		env.put(Context.PROVIDER_URL, getProviderUrl());
		// Create context as required by IFSA 2.0. Ignore the possible deprecation....
		return new IFSAContext((Context) new InitialContext(env));
	}

	@Override
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
