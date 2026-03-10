package org.frankframework.testutil.mock;

import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.digester.ConfigurationDigester;
import org.frankframework.core.Resource;
import org.frankframework.util.PropertyLoader;

public class ConfigurationDigesterMock extends ConfigurationDigester {
	@Override
	public void digest() throws ConfigurationException {
		// Ignore digest to speed up jUnit-tests
	}

	@Override
	public void digest(ApplicationContext applicationContext, Resource configurationResource, PropertyLoader properties) throws ConfigurationException {
		// Ignore digest to speed up jUnit-tests
	}
}
