/*
   Copyright 2021-2024 WeAreFrank!

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

import org.frankframework.configuration.ConfigurationException;
import org.springframework.context.Lifecycle;
import org.springframework.context.support.DefaultLifecycleProcessor;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ConfiguringLifecycleProcessor extends DefaultLifecycleProcessor implements ConfigurableLifecycle {

	/**
	 * The {@link ConfigurationDigester} may add new Lifecycle beans.
	 * Which is why it cannot be a {@link ConfigurableLifecycle} itself.
	 */
	@Override
	public void configure() throws ConfigurationException {
		log.trace("configuring all LifeCycle beans");
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

	// This triggers an internal startBeans method, and does not call #start().
	@Override
	public void onRefresh() {
		log.trace("refresh, starting all LifeCycle beans");
		super.onRefresh();
	}

	@Override
	public void start() {
		log.trace("starting all LifeCycle beans");
		super.start();
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
			for (ConfigurableLifecycle member : this.members) {
				log.trace("configuring [{}]", member);
				member.configure();
			}
		}
	}

}
