package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.frankframework.lifecycle.events.ApplicationMessageEvent;
import org.frankframework.lifecycle.events.ConfigurationMessageEvent;
import org.frankframework.lifecycle.events.MessageEventListener;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.MessageKeeper;

public class ApplicationMessageEventTest {

	@Test
	public void testConfigurationMessage() {
		TestConfiguration configuration = new TestConfiguration();
		MessageEventListener listener = new MessageEventListener();
		configuration.addApplicationListener(listener);
		configuration.publishEvent(new ConfigurationMessageEvent(configuration, "test 123"));

		MessageKeeper globalKeeper = listener.getMessageKeeper();
		assertEquals(1, globalKeeper.size());
		assertEquals("INFO", globalKeeper.getMessage(0).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] test 123", globalKeeper.getMessage(0).getMessageText());

		MessageKeeper configKeeper = listener.getMessageKeeper(configuration.getName());
		assertEquals(1, configKeeper.size());
		assertEquals("INFO", configKeeper.getMessage(0).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] test 123", configKeeper.getMessage(0).getMessageText());
	}

	@Test
	public void testConfigurationMessageWithVersion() {
		TestConfiguration configuration = new TestConfiguration();
		MessageEventListener listener = new MessageEventListener();
		configuration.addApplicationListener(listener);
		configuration.setVersion("13-2342");
		configuration.publishEvent(new ConfigurationMessageEvent(configuration, "test 123"));

		MessageKeeper globalKeeper = listener.getMessageKeeper();
		assertEquals(1, globalKeeper.size());
		assertEquals("INFO", globalKeeper.getMessage(0).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] [13-2342] test 123", globalKeeper.getMessage(0).getMessageText());

		MessageKeeper configKeeper = listener.getMessageKeeper(configuration.getName());
		assertEquals(1, configKeeper.size());
		assertEquals("INFO", configKeeper.getMessage(0).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] [13-2342] test 123", configKeeper.getMessage(0).getMessageText());
	}

	@Test
	public void testGlobalMessage() {
		TestConfiguration configuration = new TestConfiguration();
		MessageEventListener listener = new MessageEventListener();
		configuration.addApplicationListener(listener);
		configuration.setVersion("13-2342");
		configuration.publishEvent(new ApplicationMessageEvent(configuration, "test 123"));

		MessageKeeper globalKeeper = listener.getMessageKeeper();
		assertEquals(1, globalKeeper.size());
		assertEquals("INFO", globalKeeper.getMessage(0).getMessageLevel());
		assertEquals("Application [TestConfiguration] test 123", globalKeeper.getMessage(0).getMessageText());

		MessageKeeper configKeeper = listener.getMessageKeeper(configuration.getName());
		assertNull(configKeeper, "did not expect a MessageKeeper as there should not be any messages");
	}
}
