package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.MessageKeeper;

public class IbisContextMessageLogTest {

	@Test
	public void testConfigurationMessage() {
		IbisContext ibisContext = new IbisContext();
		ibisContext.log(TestConfiguration.TEST_CONFIGURATION_NAME, null, "test 123");

		MessageKeeper globalKeeper = ibisContext.getMessageKeeper();
		assertEquals(1, globalKeeper.size());
		assertEquals("INFO", globalKeeper.getMessage(0).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] test 123", globalKeeper.getMessage(0).getMessageText());

		MessageKeeper configKeeper = ibisContext.getMessageKeeper(TestConfiguration.TEST_CONFIGURATION_NAME);
		assertEquals(1, configKeeper.size());
		assertEquals("INFO", configKeeper.getMessage(0).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] test 123", configKeeper.getMessage(0).getMessageText());
	}

	@Test
	public void testConfigurationMessageWithVersion() {
		IbisContext ibisContext = new IbisContext();
		ibisContext.log(TestConfiguration.TEST_CONFIGURATION_NAME, "13-2342", "test 123");

		MessageKeeper globalKeeper = ibisContext.getMessageKeeper();
		assertEquals(1, globalKeeper.size());
		assertEquals("INFO", globalKeeper.getMessage(0).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] [13-2342] test 123", globalKeeper.getMessage(0).getMessageText());

		MessageKeeper configKeeper = ibisContext.getMessageKeeper(TestConfiguration.TEST_CONFIGURATION_NAME);
		assertEquals(1, configKeeper.size());
		assertEquals("INFO", configKeeper.getMessage(0).getMessageLevel());
		assertEquals("Configuration [TestConfiguration] [13-2342] test 123", configKeeper.getMessage(0).getMessageText());
	}

	@Test
	public void testGlobalMessage() {
		IbisContext ibisContext = new IbisContext();
		ibisContext.log(null, "13-2342", "test 123");

		MessageKeeper globalKeeper = ibisContext.getMessageKeeper();
		assertEquals(2, globalKeeper.size()); //Why 2?
		assertEquals("INFO", globalKeeper.getMessage(0).getMessageLevel());
		assertEquals("Application [TestConfiguration] test 123", globalKeeper.getMessage(0).getMessageText());

		MessageKeeper configKeeper = ibisContext.getMessageKeeper(TestConfiguration.TEST_CONFIGURATION_NAME);
		assertNull(configKeeper);
	}
}
