package org.frankframework.testutil.mock;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.digester.ConfigurationDigester;

public class ConfigurationDigesterMock extends ConfigurationDigester {
	@Override
	public void digest() throws ConfigurationException {
		// Ignore digest to speed up jUnit-tests
	}
}
