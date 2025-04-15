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
package org.frankframework.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.support.GenericApplicationContext;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.lifecycle.ConfiguringLifecycleProcessor;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.scheduler.job.IJob;
import org.frankframework.util.RunState;
import org.frankframework.util.SpringUtils;

/**
 * Container for jobs that are scheduled for periodic execution.
 * <p>
 * Configure/start/stop lifecycles are managed by Spring.
 * @see ConfiguringLifecycleProcessor
 *
 * @author Niels Meijer
 *
 */
@Log4j2
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class ScheduleManager extends GenericApplicationContext implements ConfigurableLifecycle, InitializingBean, ApplicationContextAware {

	private @Getter boolean isConfigured = false;
	private @Getter RunState state = RunState.STOPPED;

	@Override
	protected void initLifecycleProcessor() {
		ConfiguringLifecycleProcessor defaultProcessor = new ConfiguringLifecycleProcessor();
		defaultProcessor.setBeanFactory(getBeanFactory());
		getBeanFactory().registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, defaultProcessor);
		super.initLifecycleProcessor();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		setParent(applicationContext);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isActive()) {
			throw new LifecycleException("unable to refresh, ScheduleManager is already active");
		}

		refresh();
	}

	@Override
	public void configure() throws ConfigurationException {
		log.info("configuring ScheduleManager [{}]", this::getId);
		state = RunState.STARTING;

		LifecycleProcessor lifecycle = getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
		if (!(lifecycle instanceof ConfigurableLifecycle configurableLifecycle)) {
			throw new ConfigurationException("wrong lifecycle processor found, unable to configure beans");
		}

		log.debug("configuring ScheduleManager [{}]", getId());

		// Trigger a configure on all (Configurable) Lifecycle beans
		configurableLifecycle.configure();
		isConfigured = true;
	}

	@Override
	public int getPhase() {
		return 200;
	}

	@Override
	public boolean isRunning() {
		return state == RunState.STARTED && super.isRunning();
	}

	/**
	 * Configure and start, managed through the Spring Lifecyle
	 */
	@Override
	public void start() {
		log.info("starting ScheduleManager [{}]", this::getId);
		if (!isConfigured()) {
			throw new IllegalStateException("cannot start ScheduleManager that's not configured");
		}

		super.start();
		state = RunState.STARTED;
	}

	/*
	 * Opposed to close you do not need to reconfigure the manager. Allows you to stop and start Schedules.
	 */
	@Override
	public void stop() {
		log.info("stopping ScheduleManager [{}]", this::getId);
		state = RunState.STOPPING;

		try {
			super.stop();
		} finally {
			state = RunState.STOPPED;
		}
	}


	/**
	 * Close this context and remove all registered jobs
	 */
	@Override
	public void close() {
		log.info("closing ScheduleManager [{}]", this::getId);
		try {
			state = RunState.STOPPING;
			super.close();
		} finally {
			isConfigured = false;
			state = RunState.STOPPED;
		}
	}

	/**
	 * Job that is executed periodically. The time of execution can be configured within the job
	 * or from outside the configuration through the Frank!Console.
	 */
	public void addScheduledJob(IJob job) {
		log.debug("registering job [{}] with ScheduleManager [{}]", ()->job, ()->this);
		if(job.getName() == null) {
			throw new IllegalStateException("JobDef has no name");
		}

		SpringUtils.registerSingleton(this, job.getName(), job);
		log.debug("ScheduleManager [{}] registered job [{}]", this::getId, job::toString);
	}

	public void unRegister(IJob job) {
		DefaultListableBeanFactory cbf = (DefaultListableBeanFactory) getAutowireCapableBeanFactory();
		String name = job.getName();
		getSchedules()
				.keySet()
				.stream()
				.filter(name::equals)
				.forEach(cbf::destroySingleton);
		log.debug("unregistered JobDef [{}] from ScheduleManager [{}]", ()->name, ()->this);
	}

	public final Map<String, IJob> getSchedules() {
		Map<String, IJob> jobs = getBeansOfType(IJob.class);
		return Collections.unmodifiableMap(jobs);
	}

	public List<IJob> getSchedulesList() {
		if (!isActive()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(getSchedules().values());
	}

	public IJob getSchedule(String name) {
		return getSchedules().get(name);
	}
}
