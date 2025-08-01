package org.frankframework.amqp;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import lombok.extern.log4j.Log4j2;

@Testcontainers(disabledWithoutDocker = true)
@Log4j2
class RabbitMQ4AmqpSenderTest extends AmqpSenderTest {

	private static final String RABBIT_MQ_DOCKER_TAG = "rabbitmq:4-alpine";

	@Container
	private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RABBIT_MQ_DOCKER_TAG)
//			.withAdminUser("admin")
//			.withAdminPassword("admin")
			;

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

	@AfterAll
	static void tearDownClass() {
//		await().atMost(1, TimeUnit.DAYS).until(() -> false);
	}

	@Override
	@Disabled("RabbitMQ does not support Dynamic Receivers now used for RR")
	void sendMessageRR() {
		//noinspection DataFlowIssue
		assumeFalse(true);
	}

}
