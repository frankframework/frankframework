package nl.nn.adapterframework.testutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.nn.adapterframework.configuration.Configuration;

/**
 * Test Configuration utility
 */
public class TestConfiguration extends Configuration {
	public final static String TEST_CONFIGURATION_NAME = "TestConfiguration";

	//Configures a standalone configuration.
	public TestConfiguration() {
		super();
		setConfigLocation("testConfigurationContext.xml");
		setName(TEST_CONFIGURATION_NAME);
		refresh();
		configure();
		start();
	}

	@Test
	public void testTestConfiguration() {
		Configuration config = new TestConfiguration(); //Validate it can create/init
		assertTrue(config.isActive());
		assertEquals(TEST_CONFIGURATION_NAME, config.getId());
		config.close();
		assertTrue(!config.isActive());
	}
}
