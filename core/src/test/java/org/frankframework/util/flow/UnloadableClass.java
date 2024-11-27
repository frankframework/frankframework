package org.frankframework.util.flow;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;

public class UnloadableClass implements IConfigurable {
	static String fail = null;
	static {
		// Force an exception so the class won't load
		System.err.println(fail.length());
	}

	@Override
	public void setName(String name) {
		// No-op
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return null;
	}

	@Override
	public void configure() throws ConfigurationException {
		// No-op
	}

	@Override
	public ClassLoader getConfigurationClassLoader() {
		return null;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// No-op
	}
}
