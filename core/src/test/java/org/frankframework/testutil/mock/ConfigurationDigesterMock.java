package org.frankframework.testutil.mock;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.digester.ConfigurationDigester;
import org.frankframework.core.Resource;
import org.frankframework.util.PropertyLoader;

public class ConfigurationDigesterMock extends ConfigurationDigester {

	private @Getter @Setter String originalConfiguration;
	private @Getter @Setter String loadedConfiguration;

	@Override
	public void digest() throws ConfigurationException {
		// Ignore digest to speed up jUnit-tests
		setLoadedConfiguration("<loaded authAlias=\"test\" />");
		setOriginalConfiguration("<original authAlias=\"test\" />");
	}

	@Override
	public void digest(ApplicationContext applicationContext, Resource configurationResource, PropertyLoader properties) throws ConfigurationException {
		// Ignore digest to speed up jUnit-tests
	}
}
