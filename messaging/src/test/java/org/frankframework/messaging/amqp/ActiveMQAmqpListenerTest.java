package org.frankframework.messaging.amqp;

import jakarta.annotation.Nonnull;

import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class ActiveMQAmqpListenerTest extends AmqpListenerTest {
	private static final String ACTIVEMQ_TAG = "apache/activemq-classic";

	@Container
	private static final ActiveMQContainer container = new ActiveMQContainer(ACTIVEMQ_TAG);

	@Nonnull
	@Override
	protected String getResourceName() {
		return "ActiveMQ";
	}

	@Nonnull
	@Override
	protected Integer getAmqpPort() {
		return container.getMappedPort(5672);
	}

	@Nonnull
	@Override
	protected String getHost() {
		return "localhost";
	}
}
