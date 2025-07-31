package org.frankframework.amqp;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderResult;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

@Testcontainers(disabledWithoutDocker =true)
@Log4j2
class RabbitMQAmqpSenderTest {

	private static final String EXCHANGE_NAME = "testExchange";

	private static final String RABBIT_MQ_DOCKER_TAG = "rabbitmq:4-alpine";

	@Container
	private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RABBIT_MQ_DOCKER_TAG)
//			.withAdminUser("admin")
//			.withAdminPassword("admin")
			;

	private AmqpSender sender;
	private PipeLineSession session;

	@BeforeEach
	void setUp() throws Exception {
		sender = new AmqpSender();
		sender.setQueueName(EXCHANGE_NAME);
		sender.setHost(rabbitMQContainer.getHost());
		sender.setPort(rabbitMQContainer.getAmqpPort());

		sender.configure();
		sender.start();

		session = new PipeLineSession();

		log.error(()->"RabbitMQ HTTP Connection URL: %s%n".formatted(rabbitMQContainer.getHttpUrl()));
		log.error(()->"RabbitMQ HTTPs Connection URL: %s%n".formatted(rabbitMQContainer.getHttpsUrl()));
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session);
	}

	@AfterAll
	static void tearDownClass() {
		await().atMost(1, TimeUnit.DAYS).until(() -> false);
	}

	@Test
	void sendMessageNoReply() throws Exception {
		// Arrange
		Message message = new Message("test");

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check message on queue
		org.apache.qpid.protonj2.client.Message<byte[]> result = getMessage(EXCHANGE_NAME);

		assertNotNull(result);

		log.error(result);

		String r = new String(result.body());

		assertEquals("test", r);
	}

	protected org.apache.qpid.protonj2.client.Message<byte[]> getMessage(String queueName) throws ClientException {
		try (Client client = Client.create();
			 Connection connection = client.connect(rabbitMQContainer.getHost(), rabbitMQContainer.getAmqpPort());
			 Receiver receiver = connection.openReceiver(queueName)) {
			Delivery delivery = receiver.receive(5, TimeUnit.SECONDS);
			if (delivery != null) {
				return delivery.message();
			}
			log.error("Could not get message from queue [{}]", queueName);
		}
		return null;
	}
}
