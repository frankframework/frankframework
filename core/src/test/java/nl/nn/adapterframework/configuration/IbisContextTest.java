package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IbisContextTest {

	@Test
	public void startupShutdown() {
		System.setProperty("configurations.names.application", "");
		IbisContext context = new IbisContext();
		context.init(false);

		assertEquals("TestConfiguration", context.getApplicationName());

		context.close();
	}
}
