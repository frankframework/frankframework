/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.lifecycle;

import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.support.GenericApplicationContext;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.util.RunState;

@Log4j2
public class ConfigurableApplicationContext extends GenericApplicationContext implements ConfigurableLifecycle, InitializingBean, ApplicationContextAware {

	private final String className = this.getClass().getSimpleName();
	private @Getter RunState state = RunState.STOPPED;
	private @Getter boolean isConfigured = false;

	protected final boolean inState(RunState state) {
		return getState() == state;
	}

	@Override
	protected final void initLifecycleProcessor() {
		ConfiguringLifecycleProcessor defaultProcessor = new ConfiguringLifecycleProcessor(this);
		defaultProcessor.setBeanFactory(getBeanFactory());
		getBeanFactory().registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, defaultProcessor);
		super.initLifecycleProcessor();
	}

	@Override
	public final void setApplicationContext(@Nonnull ApplicationContext applicationContext) {
		setParent(applicationContext);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isActive()) {
			throw new LifecycleException("unable to refresh, " + className + " is already active");
		}

		refresh();
	}

	@Override
	public void configure() throws ConfigurationException {
		if (!isActive()) {
			throw new LifecycleException(className + " is not active");
		}
		log.info("configuring {} [{}]", () -> className, this::getId);

		state = RunState.STARTING;

		LifecycleProcessor lifecycle = getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
		if (!(lifecycle instanceof ConfigurableLifecycle configurableLifecycle)) {
			throw new ConfigurationException("wrong lifecycle processor found, unable to configure beans");
		}

		// Trigger a configure on all (Configurable) Lifecycle beans
		configurableLifecycle.configure();
		isConfigured = true;
	}

	/**
	 * Configure and start, managed through the Spring Lifecyle
	 */
	@Override
	public void start() {
		log.info("starting {} [{}]", () -> className, this::getDisplayName);
		if (!isConfigured()) {
			throw new IllegalStateException("cannot start " + className + " that's not configured");
		}

		super.start();
		state = RunState.STARTED;
	}

	/**
	 * Opposed to close, this stops all beans with a {@link ConfigurableLifecycle ConfigurableLifecycles}.
	 * After calling stop you do not need to reconfigure this ApplicationContext.
	 * Allows you to stop and start all {@link ConfigurableLifecycle ConfigurableLifecycles}.
	 */
	@Override
	public void stop() {
		log.info("stopping {} [{}]", () -> className, this::getDisplayName);
		state = RunState.STOPPING;
		try {
			super.stop();
		} finally {
			state = RunState.STOPPED;
		}
	}

	@Override
	public boolean isRunning() {
		return inState(RunState.STARTED) && super.isRunning();
	}

	@Override
	public void close() {
		log.info("closing {} [{}]", () -> className, this::getId);
		try {
			state = RunState.STOPPING;
			super.close();
		} finally {
			isConfigured = false;
			state = RunState.STOPPED;
		}
	}
}
