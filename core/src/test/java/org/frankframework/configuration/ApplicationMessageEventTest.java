package org.frankframework.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.lifecycle.ApplicationMessageEvent;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.MessageKeeper;

public class ApplicationMessageEventTest {

	@Test
	public void testConfigurationMessage() {
		TestConfiguration configuration = new TestConfiguration();
		configuration.publishEvent(new ConfigurationMessageEvent(configuration, "test 123"));

		MessageKeeper globalKeeper = configuration.getGlobalMessageKeeper();
		assertEquals(2, globalKeeper.size());
		assertEquals("INFO", globalKeeper.getMessage(1).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] test 123", globalKeeper.getMessage(1).getMessageText());

		MessageKeeper configKeeper = configuration.getMessageKeeper();
		assertEquals(2, configKeeper.size());
		assertEquals("INFO", configKeeper.getMessage(1).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] test 123", configKeeper.getMessage(1).getMessageText());
	}

	@Test
	public void testConfigurationMessageWithVersion() {
		TestConfiguration configuration = new TestConfiguration();
		configuration.setVersion("13-2342");
		configuration.publishEvent(new ConfigurationMessageEvent(configuration, "test 123"));

		MessageKeeper globalKeeper = configuration.getGlobalMessageKeeper();
		assertEquals(2, globalKeeper.size());
		assertEquals("INFO", globalKeeper.getMessage(1).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] [13-2342] test 123", globalKeeper.getMessage(1).getMessageText());

		MessageKeeper configKeeper = configuration.getMessageKeeper();
		assertEquals(2, configKeeper.size());
		assertEquals("INFO", configKeeper.getMessage(1).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] [13-2342] test 123", configKeeper.getMessage(1).getMessageText());
	}

	@Test
	public void testGlobalMessage() {
		TestConfiguration configuration = new TestConfiguration();
		configuration.setVersion("13-2342");
		configuration.publishEvent(new ApplicationMessageEvent(configuration, "test 123"));

		MessageKeeper globalKeeper = configuration.getGlobalMessageKeeper();
		assertEquals(2, globalKeeper.size());
		assertEquals("INFO", globalKeeper.getMessage(1).getMessageLevel());
		assertEquals("Application [TestConfiguration] test 123", globalKeeper.getMessage(1).getMessageText());

		MessageKeeper configKeeper = configuration.getMessageKeeper();
		assertEquals(1, configKeeper.size());
		assertEquals("INFO", configKeeper.getMessage(0).getMessageLevel());
		assertThat(configKeeper.getMessage(0).getMessageText(), Matchers.containsString("Configuration [TestConfiguration] configured in "));
	}
}
