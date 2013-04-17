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
package nl.nn.adapterframework.core;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * Interface specifying method to configure a JMS receiver or some sort
 * from a provided {@link nl.nn.adapterframework.jms.ConnectionBase appConnection} instance.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version $Id$
 */
public interface IListenerConnector {

	public final static String THREAD_CONTEXT_SESSION_KEY="JmsSession";

   
    void configureEndpointConnection(IPortConnectedListener listener, ConnectionFactory connectionFactory, Destination destination, IbisExceptionListener exceptionListener, String cacheMode, int acknowledgeMode, boolean sessionTransacted, String selector) throws ConfigurationException;

	/**
	 * Start Listener-port to which the Listener is connected.
	 */
    void start() throws ListenerException;
 
	/**
	 * Stop Listener-port to which the Listener is connected.
	 */
    void stop() throws ListenerException;
}
