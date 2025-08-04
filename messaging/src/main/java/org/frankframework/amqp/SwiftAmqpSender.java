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
package org.frankframework.amqp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.springframework.util.MimeType;

import com.swiftmq.amqp.v100.client.AMQPException;
import com.swiftmq.amqp.v100.client.AuthenticationException;
import com.swiftmq.amqp.v100.client.Connection;
import com.swiftmq.amqp.v100.client.Consumer;
import com.swiftmq.amqp.v100.client.Producer;
import com.swiftmq.amqp.v100.client.Session;
import com.swiftmq.amqp.v100.client.UnsupportedProtocolVersionException;
import com.swiftmq.amqp.v100.generated.messaging.message_format.AddressIF;
import com.swiftmq.amqp.v100.generated.messaging.message_format.AmqpValue;
import com.swiftmq.amqp.v100.generated.messaging.message_format.Data;
import com.swiftmq.amqp.v100.generated.messaging.message_format.Properties;
import com.swiftmq.amqp.v100.generated.transport.definitions.Fields;
import com.swiftmq.amqp.v100.messaging.AMQPMessage;
import com.swiftmq.amqp.v100.types.AMQPSymbol;
import com.swiftmq.amqp.v100.types.AMQPType;

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
import org.frankframework.util.MessageUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;

@Category(Category.Type.EXPERIMENTAL)
@DestinationType(DestinationType.Type.AMQP)
public class SwiftAmqpSender extends AbstractSenderWithParameters implements ISenderWithParameters, HasPhysicalDestination {
	public static final long DEFAULT_TIMEOUT_SECONDS = 30L;

	enum DeliveryMode { AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE }

	private String connectionName;
	private String queueName;
	private long timeout = DEFAULT_TIMEOUT_SECONDS;
	private DeliveryMode deliveryMode = DeliveryMode.AT_LEAST_ONCE;
	private MessageProtocol messageProtocol = MessageProtocol.FF;

	private @Setter SwiftAmqpConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (connectionFactory == null) {
			throw new ConfigurationException("ConnectionFactory is null");
		}
	}

	@Override
	public void start() {
		super.start();
		try {
			connection = connectionFactory.getConnection(connectionName);
			connection.setContainerId(Misc.getHostname());
			connection.setProperties(new Fields(Map.of()));
			connection.connect();

			Fields remoteProperties = connection.getRemoteProperties();
			log.info("AMQP Server Properties: {}", remoteProperties);

			session = connection.createSession(100, 100);
			if (messageProtocol == MessageProtocol.RR) {

				AMQPType product = remoteProperties.getValue().get(new AMQPSymbol("product"));
				if ("RabbitMQ".equals(product.getName())) {
					ConfigurationWarnings.add(this, log, "RabbitMQ does not support dynamic request-reply queues");
				}
			}

		} catch (IOException | UnsupportedProtocolVersionException | AuthenticationException | AMQPException e) {
			throw new LifecycleException("Cannot create connection to AMQP server", e);
		}
	}

	@Override
	public void stop() {
		if (session != null) {
			session.close();
			session = null;
		}
		if (connection != null) {
			connection.close();
			connection = null;
		}
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
		try {
			Consumer replyConsumer = session.createConsumer(200, deliveryMode.ordinal());
			AddressIF remoteAddress = replyConsumer.getRemoteAddress();
			doSend(message, remoteAddress);

			AMQPMessage response = replyConsumer.receive(timeout * 1_000L);
			if (response == null) {
				responseMessage = Message.nullMessage();
			} else {
				responseMessage = convertAmqpMessage(response);
				response.accept();
			}
		} catch (AMQPException | TimeoutException | IOException e) {
			throw new SenderException("Error sending request/response message", e);
		}
		return new SenderResult(responseMessage);
	}

	private Message convertAmqpMessage(AMQPMessage amqpMessage) throws IOException {
		Properties messageProperties = amqpMessage.getProperties();

		Message message;
		List<Data> data = amqpMessage.getData();
		if (data != null) {
			List<ByteArrayInputStream> dataStreams = data.stream().map(d -> new ByteArrayInputStream(d.getValue()))
					.toList();
			InputStream is = new SequenceInputStream(Collections.enumeration(dataStreams));

			message = new Message(is);
		} else {
			AmqpValue amqpValue = amqpMessage.getAmqpValue();
			if (amqpValue != null) {
				message = new Message(amqpValue.getValue().getValueString());
			} else {
				message = Message.nullMessage();
			}
		}
		if (messageProperties != null) {
			AMQPSymbol contentEncoding = messageProperties.getContentEncoding();
			if (contentEncoding != null) {
				message.getContext().withCharset(contentEncoding.getValue());
			}
			AMQPSymbol contentType = messageProperties.getContentType();
			if (contentType != null) {
				message.getContext().withMimeType(contentType.getValue());
			}
		}
		return message;
	}

	private void doSend(Message message, AddressIF dynamicReplyAddress) throws SenderException, TimeoutException {
		sendBinaryMessage(message, dynamicReplyAddress);
	}

	private void sendBinaryMessage(@Nonnull Message message, @Nullable AddressIF replyAddress) throws SenderException, TimeoutException {
		AMQPMessage amqpMessage;
		try {
			Data data = new Data(message.asByteArray());
			amqpMessage = new AMQPMessage();
			amqpMessage.addData(data);
			amqpMessage.setProperties(new Properties());
			applyMessageMetaData(message, amqpMessage);
			applyMessageOptions(amqpMessage, replyAddress);
		} catch (Exception e) {
			throw new SenderException("Cannot create AMQP message", e);
		}
		try {
			Producer sender = session.createProducer(queueName, deliveryMode.ordinal());
			sender.send(amqpMessage);
			sender.close();
		} catch (AMQPException e) {
			throw new SenderException("Cannot send AMQP message to AMQP server", e);
		}
	}

	private void applyMessageOptions(@Nonnull AMQPMessage amqpMessage, @Nullable AddressIF replyAddress) throws AMQPException {
//		amqpMessage.durable(durable);

		if (replyAddress != null) {
			amqpMessage.getProperties().setReplyTo(replyAddress);
		}
	}

	private static void applyMessageMetaData(@Nonnull Message message, AMQPMessage amqpMessage) throws AMQPException, IOException {
		Properties messageProperties = amqpMessage.getProperties();
		MimeType mimeType = MessageUtils.computeMimeType(message);
		if (mimeType != null) {
			messageProperties.setContentType(new AMQPSymbol(mimeType.toString()));
		}
		Charset charset = MessageUtils.computeDecodingCharset(message);
		if (charset != null) {
			messageProperties.setContentEncoding(new AMQPSymbol(charset.toString()));
		} else if (!message.isBinary()) {
			messageProperties.setContentEncoding(new AMQPSymbol(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
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
	 * Set the queue on which to send messages
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
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
	 * @param messageProtocol {@literal FF} for Fire-and-Forget, or {@literal RR} for Request-Reply.
	 *
	 * @ff.default {@literal FF}
	 */
	public void setMessageProtocol(MessageProtocol messageProtocol) {
		this.messageProtocol = messageProtocol;
	}
}
