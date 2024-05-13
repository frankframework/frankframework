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
package org.frankframework.unmanaged;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.Message;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IPortConnectedListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.LogUtil;

/**
 * Base class for JMS Configurator implementations.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public abstract class AbstractJmsConfigurator {
	protected Logger log= LogUtil.getLogger(this);

	private @Getter @Setter IPortConnectedListener<Message> listener;
	private @Getter @Setter ConnectionFactory connectionFactory;
	private @Getter @Setter Destination destination;
	private @Getter Receiver<Message> receiver;
	private @Getter IbisExceptionListener exceptionListener;

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
		this.receiver = getListener().getReceiver();
		this.exceptionListener = exceptionListener;
	}

}
