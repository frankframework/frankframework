package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ClassLoaderManager;

public interface IScopeProvider {

	/**
	 * This ClassLoader is set upon creation of the object, used to retrieve resources configured by the Ibis application.
	 * @return returns the ClassLoader created by the {@link ClassLoaderManager ClassLoaderManager}.
	 */
	public ClassLoader getConfigurationClassLoader();
}
