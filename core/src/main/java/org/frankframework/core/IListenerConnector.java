/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.core;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.util.CredentialFactory;

/**
 * Interface specifying method to configure a JMS receiver of some sort.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public interface IListenerConnector<M> {

	String THREAD_CONTEXT_SESSION_KEY = PipeLineSession.SYSTEM_MANAGED_RESOURCE_PREFIX + "JmsSession";

	void configureEndpointConnection(IPortConnectedListener<M> listener, ConnectionFactory connectionFactory,
			CredentialFactory credentialFactory, Destination destination, IbisExceptionListener exceptionListener, CacheMode cacheMode,
			int acknowledgeMode, boolean sessionTransacted, String selector, long receiveTimeout, long pollGuardInterval)
			throws ConfigurationException;

	/**
	 * Start Listener-port to which the Listener is connected.
	 */
	void start();

	/**
	 * Stop Listener-port to which the Listener is connected.
	 */
	void stop();

	enum CacheMode {
		CACHE_NONE,
		CACHE_CONNECTION,
		CACHE_SESSION,
		CACHE_CONSUMER
	}
}
