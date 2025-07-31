package org.frankframework.amqp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nullable;

import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.StreamDelivery;
import org.apache.qpid.protonj2.client.StreamReceiver;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderResult;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

@Log4j2
public abstract class AmqpSenderTest {
	private static final String EXCHANGE_NAME = "testExchange";
	private AmqpSender sender;
	private PipeLineSession session;

	@BeforeEach
	void setUp() throws Exception {
		sender = new AmqpSender();
		sender.setQueueName(AmqpSenderTest.EXCHANGE_NAME);
		sender.setHost(getHost());
		sender.setPort(getAmqpPort());

		sender.configure();
		sender.start();

		session = new PipeLineSession();
	}

	protected abstract Integer getAmqpPort();

	protected abstract String getHost();

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session);
		sender.stop();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void sendMessageNoReply(boolean sendStreaming) throws Exception {
		// Arrange
		sender.setSendStreaming(sendStreaming);
		Message message = new Message("test");

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check message on queue
		org.apache.qpid.protonj2.client.Message<byte[]> result = getMessage(AmqpSenderTest.EXCHANGE_NAME);

		assertNotNull(result);

		AmqpSenderTest.log.error(result);

		String r = new String(result.body());

		assertEquals("test", r);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void sendMessageNoReplyReceiveStreaming(boolean sendStreaming) throws Exception {
		// Arrange
		sender.setSendStreaming(sendStreaming);
		Message message = new Message("test");

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check message on queue
		Message result = getStreamingMessage(AmqpSenderTest.EXCHANGE_NAME);

		assertNotNull(result);

		AmqpSenderTest.log.error(result);

		String r = result.asString();

		assertEquals("test", r);
	}

	protected @Nullable org.apache.qpid.protonj2.client.Message<byte[]> getMessage(String queueName) throws ClientException {
		try (Client client = Client.create();
			 Connection connection = client.connect(getHost(), getAmqpPort());
			 Receiver receiver = connection.openReceiver(queueName)) {
			Delivery delivery = receiver.receive(5, TimeUnit.SECONDS);
			if (delivery != null) {
				return delivery.message();
			}
			AmqpSenderTest.log.error("Could not get message from queue [{}]", queueName);
		}
		return null;
	}

	protected @Nullable Message getStreamingMessage(String queueName) throws ClientException, IOException {
		try (Client client = Client.create();
			 Connection connection = client.connect(getHost(), getAmqpPort());
			 StreamReceiver receiver = connection.openStreamReceiver(queueName)) {
			StreamDelivery delivery = receiver.receive(5, TimeUnit.SECONDS);
			if (delivery != null) {
				return new Message(delivery.message().body());
			}
			AmqpSenderTest.log.error("Could not get streaming message from queue [{}]", queueName);
		}
		return null;
	}
}
