package org.frankframework.extensions.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
public class KafkaIntegrationTest {

	@Container
	private static final KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka");

	final KafkaSender sender = new KafkaSender();

	final KafkaListener listener = new KafkaListener();

	@BeforeEach
	void setUp() throws Exception {
		sender.setTopic("test.test2");
		sender.setClientId("test");
		sender.setBootstrapServers(kafkaContainer.getBootstrapServers());
		sender.configure();
		sender.start();

		listener.setTopics("test.test2.*, anothertopic");
		listener.setClientId("test");
		listener.setBootstrapServers(kafkaContainer.getBootstrapServers());
		listener.setGroupId("testGroupId");
		listener.configure();
		listener.start();
	}

	/**
	 * Test sending a message to Kafka and receiving it back through the listener.
	 */
	@Test
	public void test() throws Exception {
		final String messageContent = "Hello World";

		// Send a message using the sender
		Message message = new Message(messageContent);
		PipeLineSession session = new PipeLineSession();
		sender.sendMessage(message, session);

		// Wait a bit
		RawMessageWrapper<ConsumerRecord<String, byte[]>> messageWrapper = Awaitility.await()
				.atMost(10, TimeUnit.SECONDS)
				.until(() -> listener.getRawMessage(new HashMap<>()), Objects::nonNull);

		// Assert that the message was received by the listener
		Assertions.assertNotNull(messageWrapper);
		assertEquals(messageContent, new String(messageWrapper.getRawMessage().value()));
	}
}
