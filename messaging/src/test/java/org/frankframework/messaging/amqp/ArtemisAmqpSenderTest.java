package org.frankframework.messaging.amqp;

import org.jspecify.annotations.NonNull;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class ArtemisAmqpSenderTest extends AmqpSenderTest {
	private static final String ARTEMIS_TAG = "apache/activemq-artemis";

	@Container
	private static final ArtemisContainer container = new ArtemisContainer(ARTEMIS_TAG)
			.withEnv("ANONYMOUS_LOGIN", "true");

	@NonNull
	@Override
	protected String getResourceName() {
		return "Artemis";
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
