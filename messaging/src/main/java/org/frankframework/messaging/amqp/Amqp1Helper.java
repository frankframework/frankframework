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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.client.Sender;
import org.apache.qpid.protonj2.client.SenderOptions;
import org.apache.qpid.protonj2.client.StreamDelivery;
import org.apache.qpid.protonj2.client.StreamReceiver;
import org.apache.qpid.protonj2.client.StreamReceiverMessage;
import org.apache.qpid.protonj2.client.StreamReceiverOptions;
import org.apache.qpid.protonj2.client.Tracker;
import org.apache.qpid.protonj2.client.exceptions.ClientException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.Message;

@Log4j2
public class Amqp1Helper {
	private Amqp1Helper() {
		// Private constructor for utility class
	}

	public static @Nullable Message getStreamingMessage(@Nonnull AmqpConnectionFactoryFactory connectionFactory, @Nonnull String connectionName, @Nonnull String address, @Nonnull AddressType addressType) throws ClientException, IOException {
		try (Connection connection = connectionFactory.getConnectionFactory(connectionName).getConnection()) {
			return getStreamingMessage(connection, address, addressType);
		}
	}

	@Nullable
	public static Message getStreamingMessage(@Nonnull Connection connection, @Nonnull String address, @Nonnull AddressType addressType) throws ClientException, IOException {
		StreamReceiverOptions streamOptions = new StreamReceiverOptions();
		streamOptions.sourceOptions().capabilities(addressType.getCapabilityName());
		try (StreamReceiver receiver = connection.openStreamReceiver(address)) {
			StreamDelivery delivery = receiver.receive(15, TimeUnit.SECONDS);
			if (delivery != null) {
				StreamReceiverMessage amqpMessage = delivery.message();
				Message ffMessage = convertAmqpMessageToFFMessage(amqpMessage);
				delivery.accept();
				return ffMessage;
			}
			log.error("Could not get streaming message from {} [{}]", addressType, address);
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

	public static @Nullable Message getMessage(@Nonnull AmqpConnectionFactoryFactory connectionFactory, @Nonnull String connectionName, @Nonnull String address, @Nonnull AddressType addressType) throws ClientException, IOException {
		try (Connection connection = connectionFactory.getConnectionFactory(connectionName).getConnection()) {
			return getMessage(connection, address, addressType);
		}
	}

	public static @Nullable Message getMessage(@Nonnull Connection connection, @Nonnull String address, @Nonnull AddressType addressType) throws ClientException, IOException {
		ReceiverOptions receiverOptions = new ReceiverOptions();
		receiverOptions.sourceOptions().capabilities(addressType.getCapabilityName());
		try (Receiver receiver = connection.openReceiver(address, receiverOptions)) {
			Delivery delivery = receiver.receive(15, TimeUnit.SECONDS);
			if (delivery != null) {
				Message ffMessage = convertAmqpMessageToFFMessage(delivery.message());
				delivery.accept();
				return ffMessage;
			}
		}
		log.error("Could not get message from {} [{}]", addressType, address);
		return null;
	}

	public static @Nullable String sendFFMessage(@Nonnull AmqpConnectionFactoryFactory connectionFactory, @Nonnull String connectionName, @Nonnull String address, @Nonnull AddressType addressType, @Nonnull Message message) throws ClientException, IOException {
		try (Connection connection = connectionFactory.getConnectionFactory(connectionName).getConnection()) {
			return sendFFMessage(connection, address, addressType, message);
		}
	}

	private static @Nullable String sendFFMessage(Connection connection, String address, @Nonnull AddressType addressType, Message message) throws ClientException, IOException {
		SenderOptions options = new SenderOptions();
		options.targetOptions().capabilities(addressType.getCapabilityName());

		try (Sender sender = connection.openSender(address, options)) {
			org.apache.qpid.protonj2.client.Message<?> amqpMessage;
			if (message.isBinary()) {
				amqpMessage = org.apache.qpid.protonj2.client.Message.create(message.asByteArray());
			} else {
				amqpMessage = org.apache.qpid.protonj2.client.Message.create(message.asString());
			}
			Tracker tracker = sender.send(amqpMessage);
			tracker.awaitSettlement();
			return Objects.toString(amqpMessage.messageId(), null);
		}
	}
}
