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
 * $Log: TibcoMessagingSourceFactory.java,v $
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
 * Revision 1.3  2011/11/30 13:51:58  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
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
import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.jms.JmsMessagingSourceFactory;
import nl.nn.adapterframework.jms.MessagingSource;

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
 * @version $Id$
 */
public class TibcoMessagingSourceFactory extends JmsMessagingSourceFactory {

	static private Map tibcoMessagingSourceMap = new HashMap();
	private boolean useTopic;
	
	protected Map getMessagingSourceMap() {
		return tibcoMessagingSourceMap;
	}

	public TibcoMessagingSourceFactory(JMSFacade jmsFacade, boolean useTopic) {
		super(jmsFacade);
		this.useTopic=useTopic;
	}

	protected MessagingSource createMessagingSource(String serverUrl, String authAlias, boolean createDestination, boolean useJms102) throws IbisException {
		ConnectionFactory connectionFactory = getConnectionFactory(null, serverUrl, createDestination, useJms102); 
		return new TibcoMessagingSource(serverUrl, null, connectionFactory, getMessagingSourceMap(),authAlias);
	}

	protected Context createContext() throws NamingException {
		return null;
	}

	protected ConnectionFactory createConnectionFactory(Context context, String serverUrl) throws IbisException {
		ConnectionFactory connectionFactory;
		
		if (useTopic) {
			connectionFactory = new TibjmsTopicConnectionFactory(serverUrl);
		} else {
			connectionFactory = new TibjmsQueueConnectionFactory(serverUrl);
		}
		return connectionFactory;
	}
}
