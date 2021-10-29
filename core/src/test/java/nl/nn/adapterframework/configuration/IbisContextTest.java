package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IbisContextTest {

	@Test
	public void startupShutdownNoConfigurations() {
		System.setProperty("configurations.names.application", "");

		IbisContext context = new IbisContext();
		context.init(false);

		assertEquals("TestConfiguration", context.getApplicationName());

		context.close();
	}

	@Test
	public void startupDummyConfiguration() {
		System.setProperty("configurations.names.application", "Dummy");
		System.setProperty("configurations.Dummy.classLoaderType", "DummyClassLoader");

		IbisContext context = new IbisContext();
		context.init(false);

		assertEquals("TestConfiguration", context.getApplicationName());

		context.close();
	}
}
