package org.frankframework.messaging.amqp;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Disabled;
import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;

@Testcontainers(disabledWithoutDocker = true)
public class ActiveMQAmqpSenderTest extends AmqpSenderTest {
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

	@Override
	@Disabled("Not yet working with ActiveMQ Classic; unsure why")
	void sendMessageFFTopic() throws ConfigurationException, SenderException, TimeoutException {
		super.sendMessageFFTopic();
	}
}
