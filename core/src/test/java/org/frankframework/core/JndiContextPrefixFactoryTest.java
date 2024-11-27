package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.util.AppConstants;

public class JndiContextPrefixFactoryTest {
	private enum ApplicationServer {
		TOMCAT,JBOSS;
	}

	@Test
	public void contextPrefixTomcat() {
		JndiContextPrefixFactory factory = createFactory(ApplicationServer.TOMCAT);
		assertEquals("java:comp/env/", factory.getContextPrefix());
	}

	@Test
	public void contextPrefixJboss() {
		JndiContextPrefixFactory factory = createFactory(ApplicationServer.JBOSS);
		assertEquals("java:/", factory.getContextPrefix());
	}

	private JndiContextPrefixFactory createFactory(ApplicationServer appl) {
		AppConstants.getInstance().setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, appl.name());
		JndiContextPrefixFactory factory = new JndiContextPrefixFactory();
		factory.afterPropertiesSet();
		return factory;
	}
}
