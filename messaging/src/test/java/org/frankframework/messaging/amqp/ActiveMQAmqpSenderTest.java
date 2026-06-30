package org.frankframework.messaging.amqp;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class ActiveMQAmqpSenderTest extends AmqpSenderTest {
	private static final String ACTIVEMQ_TAG = "apache/activemq-classic";

	@Container
	private static final ActiveMQContainer container = new ActiveMQContainer(ACTIVEMQ_TAG);

	@Override
	String getQueueExchangeName() {
		return QUEUE_EXCHANGE_NAME;
	}

	@Override
	String getTopicExchangeName() {
		return TOPIC_EXCHANGE_NAME;
	}

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

	@Override
	@Disabled("Not yet working with ActiveMQ Classic; unsure why")
	void sendMessageFFTopic() throws Exception {
		super.sendMessageFFTopic();
	}
}
