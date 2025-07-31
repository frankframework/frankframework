package org.frankframework.amqp;

import org.junit.jupiter.api.Disabled;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Disabled("I don't yet know why but with ActiveMQ Artemis the test cannot receive the messages sent without error from the sender")
@Testcontainers(disabledWithoutDocker = true)
public class ArtemisAmqpSenderTest extends AmqpSenderTest {
	private static final String ARTEMIS_TAG = "apache/activemq-artemis";

	@Container
	private static final ArtemisContainer container = new ArtemisContainer(ARTEMIS_TAG)
			.withEnv("ANONYMOUS_LOGIN", "true");

	@Override
	protected Integer getAmqpPort() {
		return container.getMappedPort(5672);
	}

	@Override
	protected String getHost() {
		return "localhost";
	}
}
