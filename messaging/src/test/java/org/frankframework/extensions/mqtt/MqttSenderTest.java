package org.frankframework.extensions.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;

import org.frankframework.parameters.Parameter;
import org.frankframework.senders.SenderTestBase;

import org.frankframework.stream.Message;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
public class MqttSenderTest extends SenderTestBase<MqttSender> {

	@Container
	private static final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.9"));

	private static final String RESOURCE_NAME = "mqtt/hivemq";

	private static MqttClientFactory mqttClientFactory;

	@Override
	public MqttSender createSender() {
		MqttSender sender = new MqttSender();
		sender.setName("senderName");
		sender.setResourceName(RESOURCE_NAME);
		sender.setMqttClientFactory(mqttClientFactory);

		return sender;
	}

	@BeforeAll
	public static void beforeAll() throws Exception {
		MqttClient client = new MqttClient(String.format("tcp://%s:%s", hivemqCe.getHost(), hivemqCe.getMqttPort()), "clientId", new MemoryPersistence());
		client.connect();

		mqttClientFactory = new MqttClientFactory();
		mqttClientFactory.add(client, RESOURCE_NAME);
	}

	@Test
	public void testSendMessageToTopicFromAttribute() {
		sender.setTopic("dummyTopic");

		assertDoesNotThrow(() -> sender.configure());
		sender.start();

		SenderResult result = assertDoesNotThrow(() -> sender.sendMessage(new Message("dummy"), session));
		result.close();
	}

	@Test
	public void testSendMessageToTopicFromParam() {
		Parameter topicParameter = new Parameter();
		topicParameter.setName("topic");
		topicParameter.setSessionKey("topicKey");
		sender.addParameter(topicParameter);

		assertDoesNotThrow(() -> sender.configure());
		sender.start();

		session.put("topicKey", "dummyTopic");

		SenderResult result = assertDoesNotThrow(() -> sender.sendMessage(new Message("dummy"), session));
		result.close();
	}

	@Test
	public void testNullTopicFromAttribute() {
		assertThrows(ConfigurationException.class, () -> sender.configure());
	}

	@Test
	public void testNullTopicFromParam() {
		Parameter topicParameter = new Parameter();
		topicParameter.setName("topic");
		topicParameter.setSessionKey("topicKey");
		sender.addParameter(topicParameter);

		assertDoesNotThrow(() -> sender.configure());
		sender.start();

		assertThrows(SenderException.class, () -> sender.sendMessage(new Message("dummy"), session));
	}

	@Test
	public void testMultipleSenders() {
		MqttSender sender1 = createSender();
		sender1.setName("sender1");
		sender1.setTopic("topic1");

		assertDoesNotThrow(sender1::configure);
		sender1.start();

		MqttSender sender2 = createSender();
		sender2.setName("sender2");
		sender2.setTopic("topic2");

		assertDoesNotThrow(sender2::configure);
		sender2.start();

		SenderResult result1 = assertDoesNotThrow(() -> sender1.sendMessage(new Message("dummy"), session));
		result1.close();

		SenderResult result2 = assertDoesNotThrow(() -> sender2.sendMessage(new Message("dummy"), session));
		result2.close();
	}

}
