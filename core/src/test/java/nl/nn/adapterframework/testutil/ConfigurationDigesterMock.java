package nl.nn.adapterframework.testutil;

import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.configuration.ConfigurationException;

public class ConfigurationDigesterMock extends ConfigurationDigester {
	@Override
	public void digest() throws ConfigurationException {
		// Ignore digest
	}
}
