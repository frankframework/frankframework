package org.frankframework.messaging.amqp;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import lombok.extern.log4j.Log4j2;

@Testcontainers(disabledWithoutDocker = true)
@Log4j2
class RabbitMQ4AmqpListenerTest extends AmqpListenerTest {

	private static final String RABBIT_MQ_DOCKER_TAG = "rabbitmq:4-alpine";

	@Container
	private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RABBIT_MQ_DOCKER_TAG);

	private static final String QUEUE_NAME = "testQueueExchange";

	private static final String TOPIC_EXCHANGE = "testTopicExchange";

	private static final String DURABLE_TOPIC_EXCHANGE = "testDurableTopicExchange";

	@Override
	@NonNull
	String getQueueExchangeName() {
		return "/queues/" + QUEUE_NAME;
	}

	@Override
	@NonNull
	String getTopicExchangeName() {
		return "/queues/queue_" + TOPIC_EXCHANGE;
	}

	@Override
	@NonNull
	String getDurableTopicExchangeName() {
		return "/queues/queue_" +  DURABLE_TOPIC_EXCHANGE;
	}

	@NonNull
	@Override
	protected String getResourceName() {
		return "RabbitMQ";
	}

	@NonNull
	@Override
	protected Integer getAmqpPort() {
		return rabbitMQContainer.getAmqpPort();
	}

	@NonNull
	@Override
	protected String getHost() {
		return rabbitMQContainer.getHost();
	}

	@BeforeAll
	static void setupClass() throws IOException, TimeoutException, InterruptedException {
		// try creating the needed exchanges and queues here
		// v2 implies a lot, including that queues and exchanges aren't created by default any more, so we have to manually do that.
		// A few other issues arose. In a nutshell:
		// * Protonj is a v1 client, and we need v2 support (so create them with rabbitmq client)
		// * using the rabbitmq:4-management image doesn't work because that implicitly enables SASL authentication

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(rabbitMQContainer.getHost());
		factory.setPort(rabbitMQContainer.getAmqpPort());
		factory.setUsername(rabbitMQContainer.getAdminUsername());
		factory.setPassword(rabbitMQContainer.getAdminPassword());

		try (Connection connection = factory.newConnection();
		     Channel channel = connection.createChannel()) {
			// If you create a queue with <name> here, it'll become available at '/queues/<name>'
			channel.queueDeclare(QUEUE_NAME, true, false, false, null);
			channel.queueDeclare(QUEUE_NAME + "_1", true, false, false, null);
			channel.queueDeclare(QUEUE_NAME + "_2", true, false, false, null);
			channel.queueDeclare(QUEUE_NAME + "_RR", true, false, false, null);
			channel.queueDeclare(QUEUE_NAME + "_RR_replies", true, false, false, null);

			createExchangeAndAttachToQueue(channel, TOPIC_EXCHANGE, "topic");
			createExchangeAndAttachToQueue(channel, TOPIC_EXCHANGE + "_1", "topic");
			createExchangeAndAttachToQueue(channel, TOPIC_EXCHANGE + "_2", "topic");

			createExchangeAndAttachToQueue(channel, DURABLE_TOPIC_EXCHANGE, "direct");
			createExchangeAndAttachToQueue(channel, DURABLE_TOPIC_EXCHANGE + "_1", "direct");
			createExchangeAndAttachToQueue(channel, DURABLE_TOPIC_EXCHANGE + "_2", "direct");
		}

		ExecResult queues = rabbitMQContainer.execInContainer("rabbitmqctl", "list_queues", "name");
		System.out.println("Queues:\n" + queues.getStdout());

		ExecResult exchanges = rabbitMQContainer.execInContainer("rabbitmqctl", "list_exchanges", "name", "type");
		System.out.println("Exchanges:\n" + exchanges.getStdout());
	}

	/**
	 * Please note that the only way to listen/receive is to listen to a queue starting the new version of rabbitmq
	 * @param channel
	 * @param exchangeName
	 * @throws IOException
	 */
	private static void createExchangeAndAttachToQueue(Channel channel, String exchangeName, String type) throws IOException {
		channel.exchangeDeclare(exchangeName, type, true);
		channel.queueDeclare("queue_" + exchangeName, true, false, false, null);
		channel.queueBind("queue_" + exchangeName, exchangeName, "#");
	}
}
