package org.frankframework.extensions.mqtt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.jdbc.datasource.ResourceObjectLocator;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.SenderTestBase;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
public class MqttSenderTest extends SenderTestBase<MqttSender> {

	@Container
	private static final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.9"));

	private static final String RESOURCE_NAME = "mqtt/hivemq";

	@Override
	public MqttSender createSender() throws Exception {
		MqttSender sender = new MqttSender();
		sender.setName("senderName");

		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("mqttResources.yml");
		locator.afterPropertiesSet();

		MqttClientFactoryFactory factory = new MqttClientFactoryFactory();
		factory.setObjectLocators(List.of(locator));
		factory.afterPropertiesSet();

		sender.setMqttClientFactoryFactory(factory);
		sender.setResourceName(RESOURCE_NAME);

		return sender;
	}

	@BeforeAll
	static void setUrlProperty() {
		System.setProperty("mqtt.brokerURL", String.format("tcp://%s:%s", hivemqCe.getHost(), hivemqCe.getMqttPort()));
	}

	@AfterAll
	static void clearUrlProperty() {
		System.clearProperty("mqtt.brokerURL");
	}

	@Test
	public void createAndDestroyFactory() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("mqttResources.yml");
		locator.afterPropertiesSet();

		MqttClientFactoryFactory factory = new MqttClientFactoryFactory();
		factory.setObjectLocators(List.of(locator));
		factory.afterPropertiesSet();

		MqttClient client = factory.getClientFactory(RESOURCE_NAME).createMqttClient();
		assertTrue(client.isConnected());

		factory.destroy();

		assertTrue(client.isConnected()); // Closing factory does not close all connected clients anymore, for now
		assertTrue(factory.getObjectInfo().isEmpty());
	}

	@Test
	public void senderCanBeClosedAndReOpened() {
		sender.setTopic("dummyTopic");

		assertDoesNotThrow(() -> sender.configure());
		sender.start();

		// Test that sending succeeds first time
		SenderResult result = assertDoesNotThrow(() -> sender.sendMessage(new Message("dummy"), session));
		CloseUtils.closeSilently(result.getResult());

		// Stop and Restart the sender
		sender.stop();
		sender.start();

		// Test that sending succeeds first time
		result = assertDoesNotThrow(() -> sender.sendMessage(new Message("dummy"), session));
		CloseUtils.closeSilently(result.getResult());
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
	public void testMultipleSenders() throws Exception {
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
