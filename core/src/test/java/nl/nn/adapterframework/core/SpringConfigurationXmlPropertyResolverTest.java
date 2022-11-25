package nl.nn.adapterframework.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.IbisContext;

public class SpringConfigurationXmlPropertyResolverTest {

	@Test
	public void testSpringXmlPropertyResolver() {
		try (IbisContext context = new IbisContext() {
			@Override
			protected String[] getSpringConfigurationFiles(ClassLoader classLoader) {
				String[] files = new String[1];
				files[0] = "SpringXmlPropertyResolverTest.xml";
				return files;
			}
		}) {
			context.init(false);

			//Make sure property in the Spring XML is resolved
			assertEquals("nl.nn.adapterframework.jms.IbisMessageListenerContainer", context.getBean("testSpringXmlPropertyResolver", String.class));
		}
	}
}
