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
package org.frankframework.components.plugins;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.InitializingBean;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.digester.ConfigurationDigester;
import org.frankframework.configuration.util.ConfigurationUtils;
import org.frankframework.core.Adapter;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.Resource;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.PropertyLoader;
import org.frankframework.util.SpringUtils;

/**
 * @author Niels Meijer
 */
public class CompositePipe extends FixedForwardPipe implements InitializingBean {

	private String pluginName;
	private String partReference = ConfigurationUtils.DEFAULT_CONFIGURATION_FILE;

	private @Getter @Setter Adapter adapter;
	private PluginLoader pluginLoader;
	private PipeLine pipeline;

	public CompositePipe() throws SecurityException, ReflectiveOperationException {
		// NOOP for Spring to initialize
	}

	// For testing purposes only!
	protected CompositePipe(PluginLoader pluginLoader, PipeLine pipeline) {
		this.pluginLoader = pluginLoader;
		this.pipeline = pipeline;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (pluginLoader == null) {
			pluginLoader = getApplicationContext().getBean(PluginLoader.class);
		}
		if (pipeline == null) {
			pipeline = ClassUtils.newInstance(PipeLine.class);
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("plugin", pluginName)) {
			parameterNamesMustBeUnique = true;

			PluginWrapper plugin = findPlugin(pluginName);
			Resource resource = getResource(plugin, partReference);

			// Assuming we can find the plugin and entrypoint. Configure the parameters.
			super.configure();

			pipeline.setClassLoader(plugin.getPluginClassLoader());
			SpringUtils.autowireByName(adapter, pipeline);

			ConfigurationDigester configurationDigester = adapter.getBean(ConfigurationDigester.class);
			PropertyLoader properties = new PropertyLoader(plugin.getPluginClassLoader(), "plugin.properties");

			// We must digest the entrypoint with the Plugin Classloader because the Thread's contextClassLoader is used.
			Thread.currentThread().setContextClassLoader(plugin.getPluginClassLoader());
			configurationDigester.digest(pipeline, resource, properties);
			log.info("succesfully loaded plugin [{}] with entrypoint [{}]", plugin::getDescriptor, resource::getName);

			// After loading all bean, configure them.
			pipeline.configure();
			log.info("succesfully configured plugin [{}] with entrypoint [{}]", plugin::getDescriptor, resource::getName);
		} finally {
			// Always revert to the original contextClassLoader, regardless if successful or not.
			Thread.currentThread().setContextClassLoader(getConfigurationClassLoader());
		}
	}

	@Override
	public void start() {
		super.start();

		if (pipeline != null) {
			pipeline.start();
		}
	}

	@Override
	public void stop() {
		if (pipeline != null) {
			pipeline.stop();
		}

		super.stop();
	}

	@Override
	public @NonNull PipeRunResult doPipe(Message message, PipeLineSession parentSession) throws PipeRunException {
		ParameterValueList pvl;
		try {
			pvl = getParameterList().getValues(message, parentSession);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "cannot determine parameter values", e);
		}

		try (PipeLineSession childSession = createChildSession(pvl, parentSession)) {
			PipeLineResult pipelineResult = pipeline.process(MessageUtils.generateMessageId(), message, childSession);

			PipeForward forward = getExitCodeForward(childSession);
			return new PipeRunResult(forward, pipelineResult.getResult());
		}
	}

	/**
	 * Allow uses to inject session variables through {@link Parameter parameters}, and set the CID when present.
	 */
	private static PipeLineSession createChildSession(ParameterValueList pvl, PipeLineSession parentSession) throws PipeRunException {
		PipeLineSession childSession = new PipeLineSession();
		String correlationId = parentSession.getCorrelationId();
		if (correlationId != null) {
			childSession.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
		}
		if (pvl != null) {
			childSession.putAll(pvl.getValueMap());
		}

		return childSession;
	}

	/**
	 * Returns the exit code forward, if present. Else the SUCCESS forward.
	 */
	private @Nullable PipeForward getExitCodeForward(PipeLineSession childSession) {
		Object exitCode = childSession.remove(PipeLineSession.EXIT_CODE_CONTEXT_KEY);
		String forwardName = Objects.toString(exitCode, null); // ToString the value
		return forwardName != null ? findForward(forwardName) : getSuccessForward();
	}

	/**
	 * Try and see if the plugin exists in the applications PluginLoader.
	 */
	private @NonNull PluginWrapper findPlugin(String nameOfPlugin) throws ConfigurationException {
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
	 * The resource is the plugin's entrypoint. Typically Configuration.xml but could be any XML file.
	 */
	private static @NonNull Resource getResource(PluginWrapper pluginWrapper, String entryPoint) throws ConfigurationException {
		if (StringUtils.isBlank(entryPoint)) {
			throw new ConfigurationException("no reference provided for plugin ["+pluginWrapper.getPluginId()+"]");
		}

		String resourceToUse = entryPoint.startsWith("/") ? entryPoint.substring(1) : entryPoint;
		Resource resource = Resource.getResource(pluginWrapper::getPluginClassLoader, resourceToUse);
		if (resource == null) {
			throw new ConfigurationException("reference ["+resourceToUse+"] not found in plugin ["+pluginWrapper.getPluginId()+"]");
		}
		return resource;
	}

	public void setPlugin(String pluginName) {
		this.pluginName = pluginName;
	}

	public void setRef(String partReference) {
		this.partReference = partReference;
	}
}
