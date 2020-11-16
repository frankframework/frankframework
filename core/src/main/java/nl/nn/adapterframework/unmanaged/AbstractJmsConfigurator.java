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
package nl.nn.adapterframework.unmanaged;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.logging.log4j.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
/**
 * Base class for JMS Configurator implementations.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 */
abstract public class AbstractJmsConfigurator {
	protected Logger log=LogUtil.getLogger(this);

	private IPortConnectedListener<Message> listener;
	private ConnectionFactory connectionFactory;
	private Destination destination;
	private Receiver<Message> receiver;
	private IbisExceptionListener exceptionListener;

	public void configureEndpointConnection(IPortConnectedListener<Message> listener, ConnectionFactory connectionFactory, Destination destination, IbisExceptionListener exceptionListener) throws ConfigurationException {
		if (connectionFactory == null) {
			throw new ConfigurationException("ConnectionFactory must be specified");
		}
		if (destination == null) {
			throw new ConfigurationException("Destination must be specified");
		}
		setListener(listener);
		setConnectionFactory(connectionFactory);
		setDestination(destination);
		this.receiver = (Receiver<Message>)getListener().getReceiver();
		this.exceptionListener = exceptionListener;
	}

	public void setListener(IPortConnectedListener<Message> listener) {
		this.listener = listener;
	}
	public IPortConnectedListener<Message> getListener() {
		return listener;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}
	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}
	public Destination getDestination() {
		return destination;
	}

	public Receiver<Message> getReceiver() {
		return receiver;
	}

	public IbisExceptionListener getExceptionListener() {
		return exceptionListener;
	}
}
