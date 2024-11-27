package org.frankframework.configuration;

import org.frankframework.core.Adapter;

/**
 * Minimum Viable AdapterManager that also calls the configure method upon registering
 */
public class AutoConfiguringAdapterManager extends AdapterManager {

	@Override
	public void addAdapter(Adapter adapter) {
		super.addAdapter(adapter);
		try {
			adapter.configure();
		} catch (ConfigurationException e) {
			log.error("error configuring adapter ["+adapter.getName()+"]", e);
		}
	};
}
