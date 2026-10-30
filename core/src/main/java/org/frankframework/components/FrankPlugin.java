/*
   Copyright 2026 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.components;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationContext;

import lombok.extern.log4j.Log4j2;

import org.frankframework.components.plugins.PluginLoader;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.digester.ConfigurationDigester;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.Resource;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.PropertyLoader;
import org.frankframework.util.SpringUtils;

@Log4j2
public class FrankPlugin extends PipeLine {
	private PluginWrapper plugin;

	private String pluginName;
	private String configurationFile;
	private ConfigurationDigester configurationDigester;

	// Test method
	public PluginLoader getPluginLoader(ApplicationContext applicationContext) {
		return applicationContext.getBean(PluginLoader.class);
	}

	// Currently called in the Configure step of callee's.
	@Override
	public void afterPropertiesSet() throws Exception {
		if (pluginName == null) {
			throw new IllegalStateException("FrankPlugin needs to be initialized with a plugin");
		}

		plugin = findPlugin(getPluginLoader(getParent()), pluginName);

		setClassLoader(plugin.getPluginClassLoader());

		PluginDescriptor descriptor = plugin.getDescriptor();
		setDisplayName("Plugin [%s:%s]".formatted(descriptor.getPluginId(), descriptor.getVersion()));

		super.afterPropertiesSet();

		// Ensure the ConfigurationDigester bean exists.
		configurationDigester = SpringUtils.createBean(this, ConfigurationDigester.class);
		SpringUtils.registerSingleton(this, "configurationDigester", configurationDigester);

	}

	@Override
	public void configure() throws ConfigurationException {
		// Load all beans, and propagate them on their parent.
		Resource resource = getConfigurationFile();
		digestPluginInClassloader(resource);
		log.info("successfully loaded plugin [{}] with entrypoint [{}]", plugin::getDescriptor, resource::getName);

		// After loading all beans, configure them.
		super.configure();
		log.info("successfully configured plugin [{}] with entrypoint [{}]", plugin::getDescriptor, resource::getName);
	}

	private void digestPluginInClassloader(Resource resource) throws ConfigurationException {
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();

		try {
			// We must digest the entrypoint with the Plugin Classloader because the Thread's contextClassLoader is used.
			thread.setContextClassLoader(getClassLoader());

			// Digest the plugin's resource.
			PropertyLoader properties = new PropertyLoader(getClassLoader(), "plugin.properties");
			configurationDigester.digest(this, resource, properties);
		} finally {
			// Always revert to the original contextClassLoader, regardless if successful or not.
			thread.setContextClassLoader(originalClassLoader);
		}
	}

	@Override
	public final PipeLineResult process(@Nullable Receiver<?> receiver, @NonNull String messageId, @NonNull Message message, @NonNull PipeLineSession pipeLineSession) throws PipeRunException {
		return process(message, pipeLineSession);
	}

	public final PipeLineResult process(@NonNull Message message, @NonNull PipeLineSession pipeLineSession) throws PipeRunException {
		String messageId = pipeLineSession.getMessageId();
		if (StringUtils.isBlank(messageId)) {
			throw new PipeRunException(null, "unable to process message without messageId");
		}

		return super.process(null, messageId, message, pipeLineSession);
	}

	/**
	 * Allow uses to inject session variables through {@link Parameter parameters}, and set the CID when present.
	 */
	public static PipeLineSession createChildSession(ParameterValueList pvl, PipeLineSession parentSession) {
		PipeLineSession childSession = new PipeLineSession();
		String correlationId = parentSession.getCorrelationId();
		if (correlationId != null) {
			childSession.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
		}
		if (pvl != null) {
			childSession.putAll(pvl.getValueMap());
		}

		// Optionally set the messageId if the ParameterValueList has none.
		// This is useful for plugins that need to maintain the same messageId, but only when explicitly set.
		if (StringUtils.isBlank(childSession.getMessageId())) {
			String messageId = MessageUtils.generateDefaultMessageId();
			childSession.put(PipeLineSession.MESSAGE_ID_KEY, messageId);
		}

		return childSession;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public void setConfigurationFile(String configurationFile) {
		this.configurationFile = configurationFile;
	}

	/**
	 * Try and see if the plugin exists in the applications PluginLoader.
	 */
	private static @NonNull PluginWrapper findPlugin(PluginLoader pluginLoader, String nameOfPlugin) throws ConfigurationException {
		PluginWrapper pluginWrapper = pluginLoader.findPlugin(nameOfPlugin);

		if (pluginWrapper == null) {
			throw new ConfigurationException("plugin ["+nameOfPlugin+"] not found");
		}
		if (pluginWrapper.getPluginState() != PluginState.STARTED) {
			throw new ConfigurationException("plugin ["+nameOfPlugin+"] is in state ["+pluginWrapper.getPluginState()+"]");
		}

		return pluginWrapper;
	}

	/**
	 * The configurationFile is the plugin's entrypoint. Typically Configuration.xml but could be any XML file.
	 */
	private @NonNull Resource getConfigurationFile() throws ConfigurationException {
		if (StringUtils.isBlank(configurationFile)) {
			throw new ConfigurationException("no reference provided for plugin ["+plugin.getPluginId()+"]");
		}

		String resourceToUse = configurationFile.startsWith("/") ? configurationFile.substring(1) : configurationFile;
		Resource resource = Resource.getResource(this, resourceToUse);
		if (resource == null) {
			throw new ConfigurationException("reference ["+resourceToUse+"] not found in plugin ["+plugin.getPluginId()+"]");
		}
		return resource;
	}

	@Override
	protected void additionalToString(StringBuilder result) {
		result.append("[plugin=").append(pluginName).append("]");
	}
}
