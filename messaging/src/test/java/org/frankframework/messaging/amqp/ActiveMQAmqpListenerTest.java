package org.frankframework.messaging.amqp;

import org.jspecify.annotations.NonNull;
import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class ActiveMQAmqpListenerTest extends AmqpListenerTest {
	private static final String ACTIVEMQ_TAG = "apache/activemq-classic";

	@Container
	private static final ActiveMQContainer container = new ActiveMQContainer(ACTIVEMQ_TAG);

	@NonNull
	@Override
	protected String getResourceName() {
		return "ActiveMQ";
	}

	@NonNull
	@Override
	protected Integer getAmqpPort() {
		return container.getMappedPort(5672);
	}

	@NonNull
	@Override
	protected String getHost() {
		return "localhost";
	}
}
