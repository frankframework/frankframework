package org.frankframework.amqp;

import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class ActiveMQAmqpSenderTest extends AmqpSenderTest {
	private static final String ACTIVEMQ_TAG = "apache/activemq-classic";

	@Container
	private static final ActiveMQContainer container = new ActiveMQContainer(ACTIVEMQ_TAG);

	@Override
	protected Integer getAmqpPort() {
		return container.getMappedPort(5672);
	}

	@Override
	protected String getHost() {
		return "localhost";
	}
}
