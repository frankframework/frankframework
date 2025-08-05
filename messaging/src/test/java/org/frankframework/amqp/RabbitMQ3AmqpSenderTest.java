package org.frankframework.amqp;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import lombok.extern.log4j.Log4j2;

@Testcontainers(disabledWithoutDocker = true)
@Log4j2
class RabbitMQ3AmqpSenderTest extends AmqpSenderTest {

	private static final String RABBIT_MQ_DOCKER_TAG = "rabbitmq:3.13-alpine";

	@Container
	private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RABBIT_MQ_DOCKER_TAG);

	@Nonnull
	@Override
	protected String getResourceName() {
		return "RabbitMQ";
	}

	@Nonnull
	@Override
	protected Integer getAmqpPort() {
		return rabbitMQContainer.getAmqpPort();
	}

	@Nonnull
	@Override
	protected String getHost() {
		return rabbitMQContainer.getHost();
	}

	@BeforeAll
	static void setupClass() throws IOException, InterruptedException {
		log.info("Try enabling AMQP 1.0 in container");
		ExecResult enableResult = rabbitMQContainer.execInContainer("/opt/rabbitmq/sbin/rabbitmq-plugins", "enable", "rabbitmq_amqp1_0");
		log.info("Result of enabling AMQP plugin: {}", enableResult);
	}


}
