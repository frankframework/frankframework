package org.frankframework.testutil.mock;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.lifecycle.ConfigurableLifecycle;

public class ConfigurationFlowGeneratorMock implements ConfigurableLifecycle {

	@Override
	public void start() {
		// NO OP
	}

	@Override
	public void stop() {
		// NO OP
	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public void configure() throws ConfigurationException {
		// NO OP
	}

}
