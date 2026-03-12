package org.frankframework.components;

import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.digester.ConfigurationDigester;
import org.frankframework.core.PipeLine;
import org.frankframework.core.Resource;
import org.frankframework.util.PropertyLoader;

public class PipelinePart extends PipeLine {
	private PluginWrapper plugin;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (plugin == null) {
			throw new IllegalStateException("PipelinePart needs to be initialized with a plugin");
		}

		setClassLoader(plugin.getPluginClassLoader());

		PluginDescriptor descriptor = plugin.getDescriptor();
		setDisplayName("Plugin [%s:%s]".formatted(descriptor.getPluginId(), descriptor.getVersion()));

		super.afterPropertiesSet();

		// Ensure the ConfigurationDigester bean exists.
		getBean(ConfigurationDigester.class);
	}

	public void digest(Resource resource) throws ConfigurationException {
		Thread thread = Thread.currentThread();

		try {
			// We must digest the entrypoint with the Plugin Classloader because the Thread's contextClassLoader is used.
			thread.setContextClassLoader(getClassLoader());

			PropertyLoader properties = new PropertyLoader(getClassLoader(), "plugin.properties");
			ConfigurationDigester configurationDigester = getBean(ConfigurationDigester.class);

			// Digest the plugin's resource.
			configurationDigester.digest(this, resource, properties);
		} finally {
			// Always revert to the original contextClassLoader, regardless if successful or not.
			thread.setContextClassLoader(getConfigurationClassLoader());
		}

	}

	public void setPlugin(PluginWrapper plugin) {
		this.plugin = plugin;
	}
}
