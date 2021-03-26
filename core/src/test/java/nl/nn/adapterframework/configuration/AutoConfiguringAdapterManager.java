package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.Adapter;

/**
 * Minimum Viable AdapterManager that also calls the configure method upon registering
 */
public class AutoConfiguringAdapterManager extends AdapterManager {

	@Override
	public void registerAdapter(Adapter adapter) {
		super.registerAdapter(adapter);
		try {
			adapter.configure();
		} catch (ConfigurationException e) {
			log.error("error configuring adapter ["+adapter.getName()+"]", e);
		}
	};
}
