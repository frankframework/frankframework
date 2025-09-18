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

import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.client.Session;
import org.apache.qpid.protonj2.client.SessionOptions;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;

@Log4j2
public class AmqpListenerContainer implements InitializingBean {
	@Autowired
	private AmqpConnectionFactoryFactory connectionFactoryFactory;
	private String connectionName;
	private Connection connection;
	private Session session;

	private final Map<Receiver, AmqpListener> receivers = new ConcurrentHashMap<>();
	private final Map<AmqpListener, Receiver> listeners = new ConcurrentHashMap<>();

	private @Getter @Setter TaskExecutor taskExecutor;

	private boolean isRunning;
	private boolean isClosing;

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	public void openConnection(@Nonnull String connectionName) throws LifecycleException {
		if (this.connectionName != null) {
			throw new IllegalStateException("connectionName is already set");
		}
		this.connectionName = connectionName;

		try {
			connection = connectionFactoryFactory.getConnectionFactory(connectionName).getConnection();
			SessionOptions sessionOptions = new SessionOptions();
			session = connection.openSession(sessionOptions);
		} catch (ClientException e) {
			throw new LifecycleException("Cannot open AMQP connection", e);
		}
	}

	public boolean isOpen() {
		return connection != null;
	}

	public void openListener(@Nonnull AmqpListener amqpListener) {
		Receiver receiver;
		ReceiverOptions options = new ReceiverOptions();
		options.deliveryMode(amqpListener.getDeliveryMode());

		try {
			if (amqpListener.isDurable()) {
				receiver = session.openDurableReceiver(amqpListener.getAddress(), amqpListener.getSubscriptionName(), options);
			} else {
				receiver = session.openReceiver(amqpListener.getAddress(), options);
			}
			receivers.put(receiver, amqpListener);
			listeners.put(amqpListener, receiver);
		} catch (ClientException e) {
			throw new LifecycleException(e);
		}

		if (!isRunning) {
			startListenerThread();
		}
	}

	/**
	 * Return true when last listener is closed
	 *
	 * @param amqpListener
	 * @return
	 */
	public boolean closeListener(AmqpListener amqpListener) {
		Receiver receiver = listeners.remove(amqpListener);
		if (receiver != null) {
			receivers.remove(receiver);
			receiver.close();
		}
		isClosing = receivers.isEmpty();
		return receivers.isEmpty();
	}

	private void startListenerThread() {
		Thread listenerThread = new Thread(() -> {
			isRunning = true;
			try {
				while (!isClosing) {
					final Receiver receiver = session.nextReceiver();
					if (receiver == null) {
						break;
					}
					AmqpListener listener = receivers.get(receiver);
					Delivery delivery = receiver.receive();
					processDelivery(listener, delivery);
				}
			} catch (ClientException e) {
				log.warn("Error receiving messages", e);
			} finally {
				isRunning = false;
			}
		});
		listenerThread.start();
	}

	private void processDelivery(AmqpListener listener, Delivery delivery) {
		taskExecutor.execute(() -> {
			try {
				listener.getResourceLimiter().acquire();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Interrupted while processing request", e);
				tryRejectDelivery(e, delivery);
			}
			try (PipeLineSession pipeLineSession = new PipeLineSession()) {
				RawMessageWrapper<Message<?>> rawMessageWrapper = listener.wrapRawMessage(delivery.message(), pipeLineSession);
				org.frankframework.stream.Message message = listener.extractMessage(rawMessageWrapper, pipeLineSession);
				MessageWrapper<Message<?>> messageWrapper = new MessageWrapper<>(message, rawMessageWrapper.getId(), rawMessageWrapper.getId());
				listener.getHandler().processRequest(listener, messageWrapper, pipeLineSession);
				delivery.accept();
			} catch (ListenerException | ClientException e) {
				log.warn("ListenerException while processing request", e);
				if (!tryRejectDelivery(e, delivery)) {
					; // TODO
				}
			} finally {
				listener.getResourceLimiter().release();
			}
		});
	}

	private static boolean tryRejectDelivery(Exception e, Delivery delivery) {
		try {
			delivery.reject("error processing request", e.getMessage());
			return true;
		} catch (ClientException ex) {
			log.warn("Error rejecting request", e);
			return false;
		}
	}

}
