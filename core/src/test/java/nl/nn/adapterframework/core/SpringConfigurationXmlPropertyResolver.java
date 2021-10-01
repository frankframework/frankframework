package nl.nn.adapterframework.core;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import nl.nn.adapterframework.configuration.IbisContext;

public class SpringConfigurationXmlPropertyResolver {

	@After
	public void teardown() {
		System.setProperty("SPRING.CONFIG.LOCATIONS", "SpringApplicationContext.xml");
	}

	@Test
	public void testSpringXmlPropertyResolver() {
		System.setProperty("SPRING.CONFIG.LOCATIONS", "SpringApplicationContext.xml,springIBISTEST.xml");

		IbisContext context = new IbisContext();
		context.init(false);

		//Make sure property in the Spring XML is resolved
		assertEquals("nl.nn.adapterframework.jms.IbisMessageListenerContainer", context.getBean("messageListenerClassName", String.class));
	}
}
