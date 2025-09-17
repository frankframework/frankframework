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
import java.util.Objects;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.protonj2.client.DeliveryMode;
import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.apache.qpid.protonj2.types.messaging.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.DestinationType;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Category;
import org.frankframework.receivers.RawMessageWrapper;

@Category(Category.Type.EXPERIMENTAL)
@DestinationType(DestinationType.Type.AMQP)
@Log4j2
public class AmqpListener implements IPushingListener<Message<?>> {
	public static final long DEFAULT_TIMEOUT_SECONDS = 30L;
	public static final long DEFAULT_TIME_TO_LIVE = Header.DEFAULT_TIME_TO_LIVE;


	private @Getter String connectionName;
	private @Getter String address;
	private @Getter String replyAddress;
	private @Getter boolean durable;
	private @Getter String subscriptionName;
	private @Getter DeliveryMode deliveryMode = DeliveryMode.AT_LEAST_ONCE;
	private @Getter long replyTimeToLive = DEFAULT_TIME_TO_LIVE;
	private @Getter long sendReplyTimeout = DEFAULT_TIMEOUT_SECONDS;

	@Autowired
	private AmqpListenerContainerManager listenerContainerManager;
	private @Setter @Getter IMessageHandler<Message<?>> handler;
	private @Setter @Getter IbisExceptionListener exceptionListener;
	private @Setter @Getter ApplicationContext applicationContext;
	private @Setter @Getter String name;

	@Override
	public RawMessageWrapper<Message<?>> wrapRawMessage(Message<?> rawMessage, PipeLineSession session) throws ListenerException {
		try {
			String messageId = Objects.toString(rawMessage.messageId(), null);
			String correlationId = Objects.toString(rawMessage.correlationId(), null);
			PipeLineSession.updateListenerParameters(session, messageId, correlationId);
			return new RawMessageWrapper<>(rawMessage, messageId, correlationId);
		} catch (ClientException e) {
			throw new ListenerException("Cannot get message ID / correlation ID", e);
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(connectionName)) {
			throw new ConfigurationException("connectionName is empty");
		}
		if (StringUtils.isEmpty(address)) {
			throw new ConfigurationException("Destination Address is empty");
		}
		if (durable) {
			if (StringUtils.isBlank(subscriptionName)) {
				throw new ConfigurationException("The subscriptionName cannot be empty when selecting durable listener");
			}
		} else {
			if (StringUtils.isNotBlank(subscriptionName)) {
				ConfigurationWarnings.add(this, log, "A subscriptionName is set but durable=false. Ignoring subscriptionName");
			}
		}
		if (listenerContainerManager == null) {
			throw new IllegalStateException("No ListenerContainerManager set");
		}
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}

	@Override
	public org.frankframework.stream.Message extractMessage(
			@Nonnull RawMessageWrapper<Message<?>> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		try {
			return Amqp1Helper.convertAmqpMessageToFFMessage(rawMessage.getRawMessage());
		} catch (Exception e) {
			throw new ListenerException("Cannot extract data from AMQP message", e);
		}
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<Message<?>> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		// No-op
	}
	/**
	 * Name of the AMQP connection in the {@literal amqp} section of the {@code resources.yaml} file.
	 */
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	/**
	 * Timeout in seconds for sending messages and receiving replies.
	 *
	 * @ff.default {@value #DEFAULT_TIMEOUT_SECONDS}
	 */
	public void setSendReplyTimeout(long timeout) {
		this.sendReplyTimeout = timeout;
	}

	/**
	 * Set the address (name of the queue or topic) on which to send messages
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * If the adapter needs to send a reply message, and the address of the reply-queue is not
	 * dynamically set on the message, then a {@code replyAddressName} can be configured for the
	 * queue on which to send the reply message. If a {@code replyAddressName} is configured but
	 * the message does have a dynamic reply-queue, then the dynamic reply-queue is used and the
	 * {@code replyAddressName} is ignored.
	 */
	public void setReplyAddress(String replyAddress) {
		this.replyAddress = replyAddress;
	}

	/**
	 * If {@literal true}, then listen for durable messages on a topic
	 */
	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	/**
	 * Set the message time-to-live, in milliseconds, for any reply messages sent by this listener.
	 *
	 * @ff.default {@literal -1}ms, meaning no expiry.
	 */
	public void setReplyTimeToLive(long timeToLive) {
		this.replyTimeToLive = timeToLive;
	}

	/**
	 * DeliveryMode: {@literal AT_LEAST_ONCE} or {@literal AT_MOST_ONCE}.
	 *
	 * @ff.default {@literal AT_LEAST_ONCE}
	 */
	public void setDeliveryMode(DeliveryMode deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * When the listener is durable, then a subscriptionName should be set so the message broker
	 * can keep track of which subscribers have already received each message.
	 */
	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

}
