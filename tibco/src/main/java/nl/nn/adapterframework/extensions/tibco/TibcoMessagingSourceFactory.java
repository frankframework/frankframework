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
package nl.nn.adapterframework.extensions.tibco;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import com.tibco.tibjms.TibjmsQueueConnectionFactory;
import com.tibco.tibjms.TibjmsTopicConnectionFactory;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.jms.JmsMessagingSourceFactory;
import nl.nn.adapterframework.jms.MessagingSource;


/**
 * Factory for {@link TibcoMessagingSource}s, to share them for Tibco Objects that can use the same.
 *
 * Tibco related IBIS objects can obtain a MessagingSource from this class. The physical connection is shared
 * between all IBIS objects that have the same (Tibco)connectionFactoryName.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class TibcoMessagingSourceFactory extends JmsMessagingSourceFactory {

	private static final Map<String,MessagingSource> TIBCO_MESSAGING_SOURCE_MAP = new HashMap<>();
	private final boolean useTopic;

	@Override
	protected Map<String, MessagingSource> getMessagingSourceMap() {
		return TIBCO_MESSAGING_SOURCE_MAP;
	}

	public TibcoMessagingSourceFactory(JMSFacade jmsFacade, boolean useTopic) {
		super(jmsFacade);
		this.useTopic=useTopic;
	}

	@Override
	protected MessagingSource createMessagingSource(String serverUrl, String authAlias, boolean createDestination, boolean useJms102) throws IbisException {
		ConnectionFactory connectionFactory = getConnectionFactory(null, serverUrl, createDestination, useJms102);
		return new TibcoMessagingSource(serverUrl, null, connectionFactory, getMessagingSourceMap(),authAlias);
	}

	@Override
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
