/*
   Copyright 2021-2025 WeAreFrank!

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.support.DefaultLifecycleProcessor;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.digester.ConfigurationDigester;
import org.frankframework.core.Adapter;
import org.frankframework.lifecycle.events.AdapterMessageEvent;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StringUtil;

/**
 * This class is a custom implementation of the Spring {@link DefaultLifecycleProcessor} that adds support for
 * {@link ConfigurableLifecycle} beans. It allows for the configuration of lifecycle beans before they are started.
 * 
 * Additionally it adds logging capabilities to track the context that's being lifecycled.
 * <p>
 * See {@link ConfigurableApplicationContext} for more information.
 */
@Log4j2
public class ConfiguringLifecycleProcessor extends DefaultLifecycleProcessor implements ConfigurableLifecycle {

	private final String className;
	private ApplicationContext applicationContext;

	public ConfiguringLifecycleProcessor(ApplicationContext context) {
		applicationContext = context;
		className = ClassUtils.classNameOf(context).toLowerCase();
	}

	/**
	 * The {@link ConfigurationDigester} may add new Lifecycle beans.
	 * Which is why it cannot be a {@link ConfigurableLifecycle} itself.
	 */
	@Override
	public void configure() throws ConfigurationException {
		long startTime = System.currentTimeMillis();
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(className, applicationContext.getId())) {
			if (log.isDebugEnabled()) log.debug("configuring all ConfigurableLifecycle beans: {}", this::getConfigurableLifecycleBeanNames);
			else log.info("configuring {}", () -> StringUtil.ucFirst(className));

			doConfigure();
			log.info("configured {} in {}", () -> StringUtil.ucFirst(className), () -> Misc.getDurationInMs(startTime));
		}
	}

	// This triggers an internal startBeans method, and does not call #start().
	// NOTE: the name/id has not been set yet (as this point), so it uses the hashcode instead. At the moment this is useful for the partent's context.
	@Override
	public void onRefresh() {
		long startTime = System.currentTimeMillis();
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(className, applicationContext.getId())) {
			if (log.isDebugEnabled()) log.debug("refresh (start) all 'autostart' LifeCycle beans: {}", this::getConfigurableLifecycleBeanNames);
			else log.info("refreshing {}", () -> StringUtil.ucFirst(className));

			super.onRefresh();
			log.info("refreshed {} in {}", () -> StringUtil.ucFirst(className), () -> Misc.getDurationInMs(startTime));
		}
	}

	@Override
	public void start() {
		long startTime = System.currentTimeMillis();
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(className, applicationContext.getId())) {
			if (log.isDebugEnabled()) log.debug("starting all LifeCycle beans: {}", this::getConfigurableLifecycleBeanNames);
			else log.info("starting {}", () -> StringUtil.ucFirst(className));

			super.start();
			log.info("started {} in {}", () -> StringUtil.ucFirst(className), () -> Misc.getDurationInMs(startTime));
		}
	}

	@Override
	public void stop() {
		long startTime = System.currentTimeMillis();
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(className, applicationContext.getId())) {
			if (log.isDebugEnabled()) log.debug("stopping all LifeCycle beans: {}", this::getConfigurableLifecycleBeanNames);
			else log.info("stopping {}", () -> StringUtil.ucFirst(className));

			super.stop();
			log.info("stopped {} in {}", () -> StringUtil.ucFirst(className), () -> Misc.getDurationInMs(startTime));
		}
	}

	/**
	 * Get a list of all bean names that implement ConfigurableLifecycle.
	 */
	private List<String> getConfigurableLifecycleBeanNames() {
		return getLifecycleBeans()
				.values()
				.stream()
				.filter(ConfigurableLifecycle.class::isInstance)
				.map(ClassUtils::nameOf)
				.toList();
	}

	private void doConfigure() throws ConfigurationException {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, LifecycleGroup> phases = new TreeMap<>();

		lifecycleBeans.forEach((beanName, bean) -> {
			if (bean instanceof ConfigurableLifecycle configurableBean) {
				int configurePhase = getPhase(configurableBean);
				phases.computeIfAbsent(configurePhase, LifecycleGroup::new).add(configurableBean);
			}
		});

		if (!phases.isEmpty()) {
			try {
				for (LifecycleGroup lifecycleGroup : phases.values()) {
					lifecycleGroup.configure();
				}
			} catch (ConfigurationException e) {
				stop(); // Stop all lifecycles
				throw e;
			}
		}
	}

	/**
	 * Simplified version of DefaultLifecycleProcessor.LifecycleGroup.
	 * Groups Lifecyle beans by Phase
	 */
	private class LifecycleGroup {

		private final int phase;
		private final List<ConfigurableLifecycle> members = new ArrayList<>();

		public LifecycleGroup(int phase) {
			this.phase = phase;
		}

		public void add(ConfigurableLifecycle bean) {
			this.members.add(bean);
		}

		public void configure() throws ConfigurationException {
			if (this.members.isEmpty()) {
				return;
			}
			log.debug("configuring beans in phase " + this.phase);
			ConfigurationException exceptionDuringPhase = null;

			for (ConfigurableLifecycle member : this.members) {
				if (member.isConfigured()) {
					log.debug("already configured [{}]", member);
					return;
				}
				log.trace("configuring [{}]", member);

				try {
					doConfigure(member);
				} catch (ConfigurationException e) {
					exceptionDuringPhase = addException(exceptionDuringPhase, e);
				}
			}
			if (exceptionDuringPhase != null) {
				throw exceptionDuringPhase;
			}
		}

		private void doConfigure(ConfigurableLifecycle member) throws ConfigurationException {
			try {
				member.configure();

				if (applicationContext instanceof Adapter adapter) adapter.publishEvent(new AdapterMessageEvent(adapter, member, "successfully configured"));
			}
			catch (ConfigurationException e) {
				if (applicationContext instanceof Adapter adapter) adapter.publishEvent(new AdapterMessageEvent(adapter, member, "unable to initialize", e));
				throw e;
			}

			log.debug("successfully configured bean [{}]", () -> ClassUtils.nameOf(member));
		}

		@Nonnull
		private static ConfigurationException addException(@Nullable final ConfigurationException originalEx, @Nonnull final ConfigurationException newEx) {
			if (originalEx == null) {
				return newEx;
			}

			originalEx.addSuppressed(newEx);
			return originalEx;
		}
	}

}
