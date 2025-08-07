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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.DeliveryMode;
import org.apache.qpid.protonj2.client.OutputStreamOptions;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.Sender;
import org.apache.qpid.protonj2.client.SenderOptions;
import org.apache.qpid.protonj2.client.Session;
import org.apache.qpid.protonj2.client.SessionOptions;
import org.apache.qpid.protonj2.client.StreamSender;
import org.apache.qpid.protonj2.client.StreamSenderMessage;
import org.apache.qpid.protonj2.client.StreamSenderOptions;
import org.apache.qpid.protonj2.client.Tracker;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.springframework.util.MimeType;

import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.DestinationType;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.extensions.messaging.MessageProtocol;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.UUIDUtil;

@Category(Category.Type.EXPERIMENTAL)
@DestinationType(DestinationType.Type.AMQP)
public class AmqpSender extends AbstractSenderWithParameters implements ISenderWithParameters, HasPhysicalDestination {
	public static final long DEFAULT_TIMEOUT_SECONDS = 30L;

	private String connectionName;
	private AddressType addressType = AddressType.QUEUE;
	private String address;
	/** Reply queue name is used internally when dynamic reply queues are not supported by the broker but until there is filtering of messages, cannot be configured */
	private String replyQueueName;
	private long timeout = DEFAULT_TIMEOUT_SECONDS;
	private MessageType messageType = MessageType.AUTO;
	private boolean streamingMessages = false;
	private boolean durable = true;
	private DeliveryMode deliveryMode = DeliveryMode.AT_LEAST_ONCE;
	private MessageProtocol messageProtocol = MessageProtocol.FF;

	private @Setter AmqpConnectionFactoryFactory connectionFactoryFactory;
	private AmqpConnectionFactory connectionFactory;
	private Session session;
	private Sender sender;
	private StreamSender streamSender;
	private boolean serverIsRabbitMQ;

	public enum MessageType {
		/**
		 * Automatically determine the type of the outgoing {@link org.apache.qpid.protonj2.client.Message} based
		 * on the value of {@link Message#isBinary()}.
		 */
		AUTO,
		/**
		 * Create the outgoing message with an {@code AmqpValue} section containing character data.
		 */
		TEXT,
		/**
		 * Create the outgoing message with an AMQP {@code Body} section containing the message as binary data.
		 * Only binary messages can be read or created in a streaming manner when messages are large.
		 */
		BINARY
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (connectionFactoryFactory == null) {
			throw new ConfigurationException("ConnectionFactory is null");
		}
		if (StringUtils.isEmpty(connectionName)) {
			throw new ConfigurationException("connectionName is empty");
		}
		if (StringUtils.isEmpty(address)) {
			throw new ConfigurationException("Destination Address is empty");
		}

		if (messageProtocol == MessageProtocol.FF && StringUtils.isNotEmpty(replyQueueName)) {
			ConfigurationWarnings.add(this, log, "Reply queue is ignored for Fire & Forget Senders");
		}

		if (streamingMessages && messageType != MessageType.BINARY) {
			ConfigurationWarnings.add(this, log, "[messageType] is ignored, because [streamingMessages] is set to [true]");
		}
		connectionFactory = connectionFactoryFactory.getConnectionFactory(connectionName);

		try (Connection connection = connectionFactory.getConnection()) {
			List<String> offeredCapabilities;
			if (connection.offeredCapabilities() != null) {
				offeredCapabilities = List.of(connection.offeredCapabilities());
			} else {
				offeredCapabilities = List.of();
			}
			log.info("AMPQ Connection Created, server offers capabilities: {}", offeredCapabilities);
			log.info("AMQP Server Properties: {}", connection.properties());

			// NB: There are a number of things that might go wrong only when using RabbitMQ but we don't know if we're talking to
			// RabbitMQ or another server until we open the connection. So we create a test-connection here.
			serverIsRabbitMQ = "RabbitMQ".equals(connection.properties().get("product"));

			if (serverIsRabbitMQ) {
				// The "/" should be legal in RabbitMQ queue names, but there are errors when I use it. Perhaps because queues are not pre-created? Dunno.
				// For now, giving a warning on it.
				if (Strings.CS.contains(address, "/") || Strings.CS.contains(replyQueueName, "/")) {
					ConfigurationWarnings.add(this, log, "RabbitMQ might not allow slashes in queue names (queue: [" + address + "]; reply queue: [" + replyQueueName + "])");
				}

				if (messageProtocol == MessageProtocol.RR && StringUtils.isEmpty(replyQueueName)) {
					replyQueueName = Misc.getHostname() + ":" + UUIDUtil.createRandomUUID();
					ConfigurationWarnings.add(this, log, "RabbitMQ does not support dynamic request-reply queues. Using randomly generated queue name [" + replyQueueName + "]");
				}
			}
		} catch (ClientException e) {
			throw new ConfigurationException("Cannot connection to the AMQP broker", e);
		}
	}

	@Override
	public void start() {
		super.start();
		try {
			session = connectionFactory.getSession(new SessionOptions());

			if (streamingMessages) {
				StreamSenderOptions streamSenderOptions = new StreamSenderOptions();
				streamSenderOptions.deliveryMode(deliveryMode);
				streamSenderOptions.targetOptions().capabilities(addressType.name().toLowerCase());
				if (messageProtocol == MessageProtocol.RR) {
					streamSenderOptions.targetOptions().capabilities("queue");
				}
				streamSender = session.connection().openStreamSender(address, streamSenderOptions);
			} else {
				SenderOptions senderOptions = new SenderOptions();
				senderOptions.deliveryMode(deliveryMode);
				if (messageProtocol == MessageProtocol.RR) {
					senderOptions.targetOptions().capabilities("queue");
				} else {
					senderOptions.targetOptions().capabilities(addressType.name().toLowerCase());
				}
				sender = session.openSender(address, senderOptions);
			}
		} catch (ClientException | RuntimeException e) {
			throw new LifecycleException("Cannot create connection to AMQP broker", e);
		}
	}

	@Override
	public void stop() {
		CloseUtils.closeSilently(sender, streamSender, session);
		super.stop();
	}

	@Override
	public String getPhysicalDestinationName() {
		return connectionName;
	}

	@Nonnull
	@Override
	public SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		SenderResult senderResult;
		if (messageProtocol == MessageProtocol.FF) {
			senderResult = sendFireForget(message);
		} else {
			senderResult = sendRequestResponse(message);
		}
		return senderResult;
	}

	@Nonnull
	private SenderResult sendFireForget(@Nonnull Message message) throws SenderException, TimeoutException {
		doSend(message, null);
		return new SenderResult("");
	}

	@Nonnull
	private SenderResult sendRequestResponse(@Nonnull Message message) throws SenderException {
		Message responseMessage;
		// It seems that dynamic receivers cannot be streaming?
		try (Receiver responseReceiver = StringUtils.isEmpty(replyQueueName) ? session.openDynamicReceiver() : session.openReceiver(replyQueueName)) {
			String responseQueueAddress = responseReceiver.address();
			doSend(message, responseQueueAddress);
			Delivery response = responseReceiver.receive(timeout, TimeUnit.SECONDS);
			if (response == null) {
				responseMessage = Message.nullMessage();
			} else {
				responseMessage = convertAndAcceptDelivery(response);
			}
		} catch (RuntimeException | ClientException | TimeoutException | IOException e) {
			throw new SenderException("Error sending request/response message", e);
		}
		return new SenderResult(responseMessage);
	}

	@Nonnull
	private static Message convertAndAcceptDelivery(Delivery delivery) throws ClientException, IOException {
		Message responseMessage;
		try {
			responseMessage = Amqp1Helper.convertAmqpMessageToFFMessage(delivery.message());
			delivery.accept();
		} catch (RuntimeException | ClientException | IOException e) {
			delivery.reject(e.getClass().getName(), e.getMessage());
			throw e;
		}
		return responseMessage;
	}

	private void doSend(Message message, String dynamicReplyAddress) throws SenderException, TimeoutException {
		if (streamingMessages) {
			sendStreamingMessage(message, dynamicReplyAddress);
		} else {
			sendObjectMessage(message, dynamicReplyAddress);
		}
	}

	private void sendObjectMessage(@Nonnull Message message, @Nullable String replyAddress) throws SenderException, TimeoutException {
		org.apache.qpid.protonj2.client.Message<?> amqpMessage;
		try {
			if (isCreateBinaryMessage(message)) {
				// Creates a message with a binary-date "Body" section
				amqpMessage = org.apache.qpid.protonj2.client.Message.create(message.asByteArray());
			} else {
				// Creates a message with an "AmqpValue" section
				amqpMessage = org.apache.qpid.protonj2.client.Message.create(message.asString());
			}
			applyMessageMetaData(message, amqpMessage);
			applyMessageOptions(amqpMessage, replyAddress);
		} catch (IOException | ClientException e) {
			throw new SenderException("Cannot create AMQP message", e);
		}
		try {
			Tracker tracker = sender.send(amqpMessage);
			Tracker tracker1 = tracker.awaitSettlement(timeout, TimeUnit.SECONDS);
			if (tracker1 != null && tracker1.state() != null && !tracker1.state().isAccepted()) {
				throw new TimeoutException("Timed out waiting for AMQP message to send");
			}
		} catch (ClientException e) {
			throw new SenderException("Cannot send AMQP message to AMQP server", e);
		}
	}

	private boolean isCreateBinaryMessage(@Nonnull Message message) {
		return switch (messageType) {
			case AUTO -> message.isBinary();
			case BINARY -> true;
			case TEXT -> false;
		};
	}

	private void applyMessageOptions(@Nonnull org.apache.qpid.protonj2.client.Message<?> amqpMessage, @Nullable String replyAddress) throws ClientException {
		amqpMessage.durable(durable);
		if (replyAddress != null) {
			amqpMessage.replyTo(replyAddress);
		}
	}

	private static void applyMessageMetaData(@Nonnull Message message, org.apache.qpid.protonj2.client.Message<?> amqpMessage) throws ClientException, IOException {
		MimeType mimeType = MessageUtils.computeMimeType(message);
		if (mimeType != null) {
			amqpMessage.contentType(mimeType.toString());
		}
		Charset charset = MessageUtils.computeDecodingCharset(message);
		if (charset != null) {
			amqpMessage.contentEncoding(charset.toString());
		} else if (!message.isBinary()) {
			amqpMessage.contentEncoding(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		}
	}

	private void sendStreamingMessage(@Nonnull Message message, @Nullable String replyAddress) throws SenderException {
		try {
			StreamSenderMessage streamSenderMessage = streamSender.beginMessage();
			applyMessageMetaData(message, streamSenderMessage);
			applyMessageOptions(streamSenderMessage, replyAddress);
			OutputStreamOptions outputStreamOptions = new OutputStreamOptions();
			if (!message.isEmpty() && message.size() <= Integer.MAX_VALUE) {
				outputStreamOptions.bodyLength(Math.toIntExact(message.size()));
			}
			try (OutputStream outputStream = streamSenderMessage.body(outputStreamOptions);
				 InputStream inputStream = message.asInputStream()) {
				if (inputStream != null) {
					inputStream.transferTo(outputStream);
				}
			} catch (IOException e) {
				log.warn("Cannot send streaming AMQP message to AMQP server, aborting message", e);
				streamSenderMessage.abort();
				throw e;
			}
			streamSenderMessage.complete();
			streamSenderMessage.tracker().awaitAccepted(timeout, TimeUnit.SECONDS);
		} catch (ClientException | IOException e) {
			throw new SenderException("Cannot send streaming AMQP message to AMQP server", e);
		}
	}

	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	/**
	 * Timeout in seconds for sending messages and receiving replies.
	 *
	 * @ff.default {@value #DEFAULT_TIMEOUT_SECONDS}
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Set the type of address to which messages are being sent, TOPIC or QUEUE.
	 * For {@literal MessageProtocol#RR} the type will always be QUEUE.
	 */
	public void setAddressType(AddressType addressType) {
		this.addressType = addressType;
	}

	/**
	 * Set the address (name of the queue or topic) on which to send messages
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * Set the message type: {@link MessageType#TEXT} to character data to be sent as {@code AmqpValue} section,
	 * {@link MessageType#BINARY} for binary data to be sent as AMQP {@code Data} section, or {@link MessageType#AUTO} to
	 * decide automatically based on the wether the input {@link Message} is binary or not.
	 * <p>
	 * When a message is to be received as a streaming message by the recipient, it has to be sent as a {@link MessageType#BINARY}
	 * message.
	 * <br/>
	 * When {@link #setStreamingMessages(boolean)} is configured {@literal true}, the {@code messageType} is ignored.
	 * </p>
	 *
	 * @ff.default {@literal AUTO}
	 */
	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	/**
	 * Set if messages should be created as streaming messages. Streaming messages are
	 * always sent as binary messages, with an AMQP {@code Body} section.
	 */
	public void setStreamingMessages(boolean streamingMessages) {
		this.streamingMessages = streamingMessages;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
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
	 * Send message as Fire-and-Forget, or as Request-Reply
	 *
	 * @param messageProtocol {@literal FF} for Fire-and-Forget, or {@literal RR} for Request-Reply.
	 * @ff.default {@literal FF}
	 */
	public void setMessageProtocol(MessageProtocol messageProtocol) {
		this.messageProtocol = messageProtocol;
	}
}
