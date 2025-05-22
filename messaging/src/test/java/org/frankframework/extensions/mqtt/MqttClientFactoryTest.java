package org.frankframework.extensions.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.frankframework.credentialprovider.CredentialFactory;
import org.frankframework.jdbc.datasource.MqttClientSettings;
import org.frankframework.jdbc.datasource.ResourceObjectLocator;
import org.frankframework.util.AppConstants;

public class MqttClientFactoryTest {

	@BeforeAll
	static void setup() {
		AppConstants.getInstance().setProperty("mqtt.brokerURL", "tcp://localhost:1883");
	}

	@AfterAll
	static void teardown() {
		AppConstants.getInstance().remove("mqtt.brokerURL");
	}

	@Test
	public void findH2Database() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("mqttResources.yml");
		locator.afterPropertiesSet();

		Object obj = locator.lookup("jdbc/H2", null, Object.class);
		assertNotNull(obj);
	}

	@Test
	public void findMqttResource() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("mqttResources.yml");
		locator.afterPropertiesSet();

		MqttClientSettings obj = locator.lookup("mqtt/hivemq", null, MqttClientSettings.class);
		assertNotNull(obj);
		assertTrue(obj.isAutomaticReconnect());
		assertNull(obj.getClientId());

		assertEquals("username1", obj.getUser());
		assertEquals("password1", obj.getPassword());

		// Test without prefix
		assertThrows(Exception.class, () -> locator.lookup("hivemq", null, MqttClientSettings.class));
	}

	@Test
	public void testClientId() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("mqttResources.yml");
		locator.afterPropertiesSet();

		assertTrue(CredentialFactory.hasCredential("alias1")); // ensure the alias exists, else the remaining tests wont work

		MqttClientSettings obj = locator.lookup("mqtt/hivemq2", null, MqttClientSettings.class);
		assertNotNull(obj);
		assertFalse(obj.isAutomaticReconnect());
		assertEquals("test123", obj.getClientId());
		assertEquals("username1", obj.getUser());
		assertEquals("password1", obj.getPassword());
	}

	@Test
	public void tryWithoutPrefix() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("mqttResources.yml");
		locator.afterPropertiesSet();

		MqttClientFactory factory = new MqttClientFactory();
		factory.setObjectLocators(List.of(locator));
		factory.afterPropertiesSet();

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> factory.getClient("hivemq"));
		assertInstanceOf(MqttException.class, e.getCause());
		assertInstanceOf(ConnectException.class, e.getCause().getCause()); // We can create the MqttClient but it cannot connect to any server.
	}
}
