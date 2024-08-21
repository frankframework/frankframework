package org.frankframework.testutil.mock;

import org.frankframework.configuration.ConfigurationDigester;
import org.frankframework.configuration.ConfigurationException;

public class ConfigurationDigesterMock extends ConfigurationDigester {
	@Override
	public void configure() throws ConfigurationException {
		// Ignore digest to speed up jUnit-tests
	}
}
