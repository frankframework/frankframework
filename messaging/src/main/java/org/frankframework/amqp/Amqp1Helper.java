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
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.client.StreamDelivery;
import org.apache.qpid.protonj2.client.StreamReceiver;
import org.apache.qpid.protonj2.client.StreamReceiverMessage;
import org.apache.qpid.protonj2.client.exceptions.ClientException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.Message;

@Log4j2
public class Amqp1Helper {
	private Amqp1Helper() {
		// Private constructor for utility class
	}

	public static @Nullable Message getStreamingMessage(@Nonnull AmqpConnectionFactoryFactory connectionFactory, @Nonnull String connectionName, @Nonnull String queueName) throws ClientException, IOException {
		try (Connection connection = connectionFactory.getConnectionFactory(connectionName).connect()) {
			return getStreamingMessage(connection, queueName);
		}
	}

	@Nullable
	public static Message getStreamingMessage(@Nonnull Connection connection, @Nonnull String queueName) throws ClientException, IOException {
		try (StreamReceiver receiver = connection.openStreamReceiver(queueName)) {
			StreamDelivery delivery = receiver.receive(15, TimeUnit.SECONDS);
			if (delivery != null) {
				StreamReceiverMessage amqpMessage = delivery.message();
				Message ffMessage = convertAmqpMessageToFFMessage(amqpMessage);
				delivery.accept();
				return ffMessage;
			}
			log.error("Could not get streaming message from queue [{}]", queueName);
			return null;
		}
	}

	@Nonnull
	public static Message convertAmqpMessageToFFMessage(@Nonnull org.apache.qpid.protonj2.client.Message<?> amqpMessage) throws IOException, ClientException {
		Object body = amqpMessage.body();
		Message result;
		if (body instanceof InputStream is) {
			result = new Message(is);
		} else {
			result = Message.asMessage(body);
		}
		copyMessageContentMetaData(amqpMessage, result);
		return result;
	}

	private static void copyMessageContentMetaData(@Nonnull org.apache.qpid.protonj2.client.Message<?> amqpMessage, Message result) throws ClientException {
		if (amqpMessage.contentEncoding() != null) {
			result.getContext().withCharset(amqpMessage.contentEncoding());
		}
		if (amqpMessage.contentType() != null) {
			result.getContext().withMimeType(amqpMessage.contentType());
		}
	}

	public static @Nullable Message getMessage(@Nonnull AmqpConnectionFactoryFactory connectionFactory, @Nonnull String connectionName, @Nonnull String queueName) throws ClientException, IOException {
		try (Connection connection = connectionFactory.getConnectionFactory(connectionName).connect()) {
			return getMessage(connection, queueName);
		}
	}

	public static @Nullable Message getMessage(@Nonnull Connection connection, @Nonnull String queueName) throws ClientException, IOException {
		ReceiverOptions receiverOptions = new ReceiverOptions();
		receiverOptions.sourceOptions().capabilities("queue");
		try (Receiver receiver = connection.openReceiver(queueName, receiverOptions)) {
			Delivery delivery = receiver.receive(15, TimeUnit.SECONDS);
			if (delivery != null) {
				org.apache.qpid.protonj2.client.Message<Object> amqpMessage = delivery.message();
				Message ffMessage = convertAmqpMessageToFFMessage(amqpMessage);
				delivery.accept();
				return ffMessage;
			}
		}
		log.error("Could not get message from queue [{}]", queueName);
		return null;
	}
}
