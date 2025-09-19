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

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.client.Sender;
import org.apache.qpid.protonj2.client.Session;
import org.apache.qpid.protonj2.client.SessionOptions;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.messaging.MessageProtocol;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.ResourceLimiter;

/**
 * Run all AMQP listeners that use the same connection in a single Session, for more
 * efficient use of system resources.
 *
 */
@Log4j2
public class AmqpListenerContainer {
	@Autowired
	private @Setter AmqpConnectionFactoryFactory connectionFactoryFactory;
	private String connectionName;
	private Connection connection;
	private Session session;

	private final Map<Receiver, AmqpListener> receivers = new ConcurrentHashMap<>();
	private final Map<AmqpListener, Receiver> listeners = new ConcurrentHashMap<>();

	private @Getter @Setter TaskExecutor taskExecutor;

	private final AtomicBoolean isRunning = new AtomicBoolean(false);
	private final AtomicBoolean isClosing = new AtomicBoolean(false);

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

		if (!isRunning.get()) {
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
		isClosing.set(receivers.isEmpty());
		return receivers.isEmpty();
	}

	private void startListenerThread() {
		Thread listenerThread = new Thread(() -> {
			isRunning.set(true);
			try {
				while (!isClosing.get()) {
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
				isRunning.set(false);
			}
		});
		listenerThread.start();
	}

	private void processDelivery(AmqpListener listener, Delivery delivery) {
		taskExecutor.execute(() -> {
			// We acquire a permit to do the work within the task-thread so that the
			// main thread can continue with a next message
			final ResourceLimiter resourceLimiter = listener.getResourceLimiter();
			try {
				resourceLimiter.acquire();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Interrupted while waiting for thread to process request", e);
				rejectDelivery(e, delivery);
				return;
			}
			try (PipeLineSession pipeLineSession = new PipeLineSession()) {
				RawMessageWrapper<Message<?>> rawMessageWrapper = listener.wrapRawMessage(delivery.message(), pipeLineSession);
				org.frankframework.stream.Message message = listener.extractMessage(rawMessageWrapper, pipeLineSession);
				MessageWrapper<Message<?>> messageWrapper = new MessageWrapper<>(message, rawMessageWrapper.getId(), rawMessageWrapper.getId());

				// Receiver#processRequest() takes care of transaction requirements
				org.frankframework.stream.Message result = listener.getHandler().processRequest(listener, messageWrapper, pipeLineSession);
				if (listener.getMessageProtocol() == MessageProtocol.RR) {
					sendReply(listener, delivery, result);
				}
				delivery.accept();
			} catch (ListenerException | ClientException | IOException e) {
				log.warn("ListenerException while processing request", e);
				rejectDelivery(e, delivery);
			} finally {
				resourceLimiter.release();
			}
		});
	}

	private void sendReply(AmqpListener listener, Delivery delivery, org.frankframework.stream.Message result) throws ClientException, ListenerException, IOException {
		String replyAddress;
		if (delivery.message().replyTo() != null) {
			replyAddress = delivery.message().replyTo();
		} else if (StringUtils.isNotBlank(listener.getReplyAddress())) {
			replyAddress = listener.getReplyAddress();
		} else {
			throw new ListenerException("No reply-to address found, cannot deliver message reply");
		}

		try (Sender sender = session.openSender(replyAddress)) {
			Message<?> replyMessage;
			if (result.isBinary()) {
				replyMessage = Message.create(result.asByteArray());
			} else {
				replyMessage = Message.create(result.asString());
			}
			sender.send(replyMessage);
		}
	}

	private void rejectDelivery(Exception e, Delivery delivery) {
		try {
			delivery.reject("error processing request", e.getMessage());
		} catch (ClientException ex) {
			log.warn("Error rejecting request, closing listeners associated with connection [" + connectionName + "]", e);
			closeAllListeners(ex);
		}
	}

	private void closeAllListeners(ClientException ex) {
		Set<AmqpListener> allListeners = new HashSet<>(listeners.keySet());
		for (AmqpListener listener : allListeners) {
			listener.stop();
			IMessageHandler<Message<?>> handler = listener.getHandler();
			if (handler instanceof IbisExceptionListener exceptionListener) {
				exceptionListener.exceptionThrown(listener, ex);
			}
		}

		session.close();
		connection.close();
		isClosing.set(true);
	}

}
