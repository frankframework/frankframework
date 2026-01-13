package org.frankframework.messaging.amqp;

import org.jspecify.annotations.NonNull;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import lombok.extern.log4j.Log4j2;

@Testcontainers(disabledWithoutDocker = true)
@Log4j2
class RabbitMQ4AmqpListenerTest extends AmqpListenerTest {

	private static final String RABBIT_MQ_DOCKER_TAG = "rabbitmq:4-alpine";

	@Container
	private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RABBIT_MQ_DOCKER_TAG)
//			.withAdminUser("admin")
//			.withAdminPassword("admin")
			;

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
}
