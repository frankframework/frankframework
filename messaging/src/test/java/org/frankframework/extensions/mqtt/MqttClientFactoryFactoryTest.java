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

import lombok.extern.log4j.Log4j2;

import org.frankframework.credentialprovider.CredentialFactory;
import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.jdbc.datasource.ResourceObjectLocator;

@Log4j2
public class MqttClientFactoryFactoryTest {

	@BeforeAll
	static void setup() {
		System.setProperty("mqtt.brokerURL", "tcp://localhost:1883");
	}

	@AfterAll
	static void teardown() {
		System.clearProperty("mqtt.brokerURL");
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

		MqttClientFactoryFactory factory = new MqttClientFactoryFactory();
		factory.setObjectLocators(List.of(locator));
		factory.afterPropertiesSet();

		MqttClientFactory clientFactory = factory.getClientFactory("hivemq");
		MqttException e = assertThrows(MqttException.class, clientFactory::createMqttClient);
		log.debug("expected an exception, logging the trace as this seems to be a bit flaky", e);
		assertInstanceOf(ConnectException.class, e.getCause()); // We can create the MqttClientFactory but it cannot connect to any server.
	}
}
