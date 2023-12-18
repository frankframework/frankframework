package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.IbisContext;

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
			assertEquals("iaf/gui/", context.getBean("testSpringXmlPropertyResolver", String.class));
		}
	}
}
