package nl.nn.adapterframework.core;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.IbisContext;

@Ignore //As long as no PropertyConfigurer or <context:property-placeholder /> is present in the SpringCommon.xml, property resolution will not work.
public class SpringConfigurationXmlPropertyResolver {

	@After
	public void teardown() {
		System.setProperty("SPRING.CONFIG.LOCATIONS", "springContext.xml");
	}

	@Test
	public void testSpringXmlPropertyResolver() {
		System.setProperty("SPRING.CONFIG.LOCATIONS", "springContext.xml,springIBISTEST.xml");

		IbisContext context = new IbisContext();
		context.init(false);

		//Make sure property in the Spring XML is resolved
		assertEquals("nl.nn.adapterframework.jms.IbisMessageListenerContainer", context.getBean("messageListenerClassName", String.class));
	}
}
