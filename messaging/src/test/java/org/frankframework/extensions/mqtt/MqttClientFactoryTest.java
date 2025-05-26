package org.frankframework.extensions.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.frankframework.jdbc.datasource.FrankResource;
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

		Object obj = locator.lookup("mqtt/hivemq", null, null);
		assertNotNull(obj);
		FrankResource resource = assertInstanceOf(FrankResource.class, obj);
		assertEquals("true", resource.getProperties().getProperty("automaticReconnect"));
		assertNull(resource.getProperties().getProperty("clientId"));

		assertEquals("username1", resource.getCredentials().getUsername());
		assertEquals("password1", resource.getCredentials().getPassword());

		// Test without prefix
		assertThrows(Exception.class, () -> locator.lookup("hivemq", null, null));
	}

	@Test
	public void testClientId() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("mqttResources.yml");
		locator.afterPropertiesSet();

		assertTrue(CredentialFactory.hasCredential("alias1")); // ensure the alias exists, else the remaining tests wont work

		Object obj = locator.lookup("mqtt/hivemq2", null, null);
		assertNotNull(obj);
		FrankResource resource = assertInstanceOf(FrankResource.class, obj);

		assertNull(resource.getProperties().getProperty("automaticReconnect"));
		assertEquals("test123", resource.getProperties().getProperty("clientId"));

		assertEquals("username1", resource.getCredentials().getUsername());
		assertEquals("password1", resource.getCredentials().getPassword());
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
