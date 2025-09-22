/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.messaging.amqp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;

/**
 * Manage all {@link AmqpListenerContainer}s for all {@link AmqpListener}s. Create a listener-container
 * per AMQP connection.
 */
public class AmqpListenerContainerManager implements ApplicationContextAware {

	@Qualifier("webApplicationContext") @Autowired
	private @Setter ApplicationContext applicationContext;

	private final Map<String, AmqpListenerContainer> listenerContainers = new ConcurrentHashMap<>();

	public void openListener(@Nonnull AmqpListener amqpListener) {
		getListenerContainer(amqpListener).openListener(amqpListener);
	}

	public void closeListener(@Nonnull AmqpListener amqpListener) {
		boolean closed = getListenerContainer(amqpListener).closeListener(amqpListener);
		if (closed) {
			listenerContainers.remove(amqpListener.getConnectionName());
		}
	}

	private @Nonnull AmqpListenerContainer getListenerContainer(@Nonnull AmqpListener listener) {
		String connectionName = listener.getConnectionName();
		return listenerContainers.computeIfAbsent(connectionName, key -> {
			AmqpListenerContainer listenerContainer = applicationContext.getAutowireCapableBeanFactory().createBean(AmqpListenerContainer.class);
			listenerContainer.openConnection(connectionName);
			return listenerContainer;
		});
	}
}
