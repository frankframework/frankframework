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
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.InitializingBean;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.components.PipelinePart;
import org.frankframework.configuration.AdapterAware;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.util.ConfigurationUtils;
import org.frankframework.core.Adapter;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.Resource;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.doc.Default;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.SpringUtils;

/**
 * Pipe that allows you to call a Frank! Plugin. Just like a FrankSender this pipe calls a sub-process, or sub-adapter.
 * As not all session variables are copied over, if you wish to propagate a value, you can do so by using Parameters.
 * For example:
 *
 * <pre>{@code
 * <CompositePipe name="name-of-the-pipe" plugin="name-of-the-plugin">
 *     <Param name="inject-me" value="im a value" />
 *     <Param name="inject-me-too" sessionKey="originalMessage" />
 * </CompositePipe>
 * }</pre>
 *
 *
 * @ff.note The sub-process called by this pipe will function as it's own 'pipeline' call, similar to calling a sub-adapter.
 * @ff.tip  A plugin may have multiple entrypoints (or ref's), each one can be called independently.
 *
 * @see <a href="https://github.com/frankframework/plugin-template">https://github.com/frankframework/plugin-template</a>
 *
 * @author Niels Meijer
 */
public class CompositeSender extends AbstractSenderWithParameters implements InitializingBean, AdapterAware {

	private String pluginName;
	private String partReference = ConfigurationUtils.DEFAULT_CONFIGURATION_FILE;

	private @Getter @Setter Adapter adapter;
	private PluginLoader pluginLoader;
	private PipelinePart pipeline;

	public CompositeSender() throws SecurityException, ReflectiveOperationException {
		// NOOP for Spring to initialize
	}

	// For testing purposes only!
	protected CompositeSender(PluginLoader pluginLoader, PipelinePart pipeline) {
		this.pluginLoader = pluginLoader;
		this.pipeline = pipeline;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (pluginLoader == null) {
			pluginLoader = getApplicationContext().getBean(PluginLoader.class);
		}
		if (pipeline == null) {
			pipeline = ClassUtils.newInstance(PipelinePart.class);
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;

		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("plugin", pluginName)) {
			PluginWrapper plugin = findPlugin(pluginName);
			Resource resource = getResource(plugin, partReference);

			// Assuming we can find the plugin and entrypoint. Configure the parameters.
			super.configure();

			pipeline.setPlugin(plugin);
			SpringUtils.autowireByName(adapter, pipeline);

			pipeline.digest(resource);
			log.info("successfully loaded plugin [{}] with entrypoint [{}]", plugin::getDescriptor, resource::getName);

			// After loading all beans, configure them.
			pipeline.configure();
			log.info("successfully configured plugin [{}] with entrypoint [{}]", plugin::getDescriptor, resource::getName);
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
	public @NonNull SenderResult sendMessage(@NonNull Message message, @NonNull PipeLineSession parentSession) throws SenderException {
		ParameterValueList pvl;
		try {
			pvl = getParameterList().getValues(message, parentSession);
		} catch (ParameterException e) {
			throw new SenderException("cannot determine parameter values", e);
		}

		try (PipeLineSession childSession = createChildSession(pvl, parentSession)) {
			PipeLineResult pipelineResult = pipeline.process(MessageUtils.generateMessageId(), message, childSession);

			Object exitCode = childSession.remove(PipeLineSession.EXIT_CODE_CONTEXT_KEY);
			String forwardName = Objects.toString(exitCode, null); // ToString the value
			return new SenderResult(pipelineResult.isSuccessful(), pipelineResult.getResult(), null, forwardName);
		} catch (PipeRunException e) {
			throw new SenderException("error while processing request in plugin", e);
		}
	}

	/**
	 * Allow uses to inject session variables through {@link Parameter parameters}, and set the CID when present.
	 */
	private static PipeLineSession createChildSession(ParameterValueList pvl, PipeLineSession parentSession) {
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

	/**
	 * Functional name of the plugin to load.
	 */
	public void setPlugin(String pluginName) {
		this.pluginName = pluginName;
	}

	/**
	 * File in the Plugin which contains the {@code <PipelinePart />} to call.
	 * Defaults to {@value ConfigurationUtils#DEFAULT_CONFIGURATION_FILE} but can be any XML file.
	 */
	@Default(ConfigurationUtils.DEFAULT_CONFIGURATION_FILE)
	public void setRef(String partReference) {
		this.partReference = partReference;
	}
}
