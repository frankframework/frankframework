package nl.nn.adapterframework.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.IbisContext;

public class SpringConfigurationXmlPropertyResolver {

	@Test
	public void testSpringXmlPropertyResolver() {
		System.setProperty("SPRING.CONFIG.LOCATIONS", "SpringApplicationContext.xml");

		IbisContext context = new IbisContext();
		context.init(false);

		//Make sure property in the Spring XML is resolved
		assertEquals("nl.nn.adapterframework.jms.IbisMessageListenerContainer", context.getBean("testSpringXmlPropertyResolver", String.class));
	}
}
