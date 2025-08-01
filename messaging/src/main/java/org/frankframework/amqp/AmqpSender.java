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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;

import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.OutputStreamOptions;
import org.apache.qpid.protonj2.client.Sender;
import org.apache.qpid.protonj2.client.StreamSender;
import org.apache.qpid.protonj2.client.StreamSenderMessage;
import org.apache.qpid.protonj2.client.Tracker;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.springframework.util.MimeType;

import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.DestinationType;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StreamUtil;

@Category(Category.Type.EXPERIMENTAL)
@DestinationType(DestinationType.Type.AMQP)
public class AmqpSender extends AbstractSenderWithParameters implements ISenderWithParameters, HasPhysicalDestination {
	public static final long DEFAULT_TIMEOUT_SECONDS = 30L;

	private String connectionName;
	private String queueName;
	private long timeout = DEFAULT_TIMEOUT_SECONDS;
	private boolean sendStreaming = false;
	private boolean durable = true;

	private @Setter AmqpConnectionFactory connectionFactory;
	private Connection connection;
	private Sender sender;
	private StreamSender streamSender;

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
			sender = connection.openSender(queueName);
			streamSender = connection.openStreamSender(queueName);
		} catch (ClientException e) {
			throw new LifecycleException("Cannot create connection to AMQP server", e);
		}
	}

	@Override
	public void stop() {
		CloseUtils.closeSilently(sender, streamSender, connection);
		super.stop();
	}

	@Override
	public String getPhysicalDestinationName() {
		return connectionName;
	}

	@Nonnull
	@Override
	public SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		if (sendStreaming) {
			sendStreamingMessage(message);
		} else {
			sendBinaryMessage(message);
		}
		SenderResult senderResult = new SenderResult("");
		return senderResult;
	}

	private void sendBinaryMessage(@Nonnull Message message) throws SenderException, TimeoutException {
		org.apache.qpid.protonj2.client.Message<byte[]> amqpMessage;
		try {
			amqpMessage = org.apache.qpid.protonj2.client.Message.create(message.asByteArray());
			applyContentOptions(message, amqpMessage);
			amqpMessage.durable(durable);
		} catch (IOException | ClientException e) {
			throw new SenderException("Cannot create AMQP message", e);
		}
		try {
			Tracker tracker = sender.send(amqpMessage);
			Tracker tracker1 = tracker.awaitAccepted(timeout, TimeUnit.SECONDS);
			if (tracker1 != null && tracker1.state() != null && !tracker1.state().isAccepted()) {
				throw new TimeoutException("Timed out waiting for AMQP message to send");
			}
		} catch (ClientException e) {
			throw new SenderException("Cannot send AMQP message to AMQP server", e);
		}
	}

	private static void applyContentOptions(@Nonnull Message message, org.apache.qpid.protonj2.client.Message<?> amqpMessage) throws ClientException, IOException {
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

	private void sendStreamingMessage(@Nonnull Message message) throws SenderException {
		try {
			StreamSenderMessage streamSenderMessage = streamSender.beginMessage();
			applyContentOptions(message, streamSenderMessage);
			streamSenderMessage.durable(durable);
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
			streamSenderMessage.tracker().awaitAccepted(timeout, TimeUnit.SECONDS);
//			streamSenderMessage.tracker().awaitSettlement(timeout, TimeUnit.SECONDS);
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
	 * Set the queue on which to send messages
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public void setSendStreaming(boolean sendStreaming) {
		this.sendStreaming = sendStreaming;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}
}
