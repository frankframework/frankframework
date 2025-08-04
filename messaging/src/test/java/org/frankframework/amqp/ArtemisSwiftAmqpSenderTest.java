package org.frankframework.amqp;

import jakarta.annotation.Nonnull;

import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class ArtemisSwiftAmqpSenderTest extends SwiftAmqpSenderTest {
	private static final String ARTEMIS_TAG = "apache/activemq-artemis";

	@Container
	private static final ArtemisContainer container = new ArtemisContainer(ARTEMIS_TAG)
			.withEnv("ANONYMOUS_LOGIN", "true");

	@Nonnull
	@Override
	protected String getResourceName() {
		return "Artemis";
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
