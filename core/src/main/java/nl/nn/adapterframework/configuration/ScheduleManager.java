/*
   Copyright 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecyleBase;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.scheduler.job.IJob;
import nl.nn.adapterframework.util.RunState;

/**
 * Container for jobs that are scheduled for periodic execution.
 *
 * @author Niels Meijer
 *
 */
/*
 * Configure/start/stop lifecycles are managed by Spring. See {@link ConfiguringLifecycleProcessor}
 */
public class ScheduleManager extends ConfigurableLifecyleBase implements ApplicationContextAware, AutoCloseable {

	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter SchedulerHelper schedulerHelper;
	private final Map<String, IJob> schedules = new LinkedHashMap<>();

	@Override
	public void configure() throws ConfigurationException {
		if(!inState(RunState.STOPPED)) {
			log.warn("unable to configure [{}] while in state [{}]", ()->this, this::getState);
			return;
		}
		updateState(RunState.STARTING);

		for (IJob jobdef : getSchedulesList()) {
			try {
				jobdef.configure();
				log.info("job scheduled with properties: {}", jobdef::toString);
			} catch (Exception e) {
				throw new ConfigurationException("could not schedule job [" + jobdef.getName() + "] cron [" + jobdef.getCronExpression() + "]", e);
			}
		}
	}

	/**
	 * Configure and start, managed through the Spring Lifecyle
	 */
	@Override
	public void start() {
		if(!inState(RunState.STARTING)) {
			log.warn("unable to start [{}] while in state [{}]", ()->this, this::getState);
			return;
		}

		for (IJob jobdef : getSchedulesList()) {
			if(jobdef.isConfigured()) {
				try {
					schedulerHelper.scheduleJob(jobdef);
					log.info("job scheduled with properties: {}", jobdef::toString);
				} catch (SchedulerException e) {
					log.error("could not schedule job [{}] cron [{}]", jobdef.getName(), jobdef.getCronExpression(), e);
				}
			} else {
				log.info("could not schedule job [{}] as it is not configured", jobdef::getName);
			}
		}

		try {
			schedulerHelper.startScheduler();
			log.info("scheduler started");
		} catch (SchedulerException e) {
			log.error("could not start scheduler", e);
		}

		updateState(RunState.STARTED);
	}

	/**
	 * remove all registered jobs
	 */
	@Override
	public void stop() {
		if(!inState(RunState.STARTED)) {
			log.warn("forcing [{}] to stop while in state [{}]", ()->this, this::getState);
		}
		updateState(RunState.STOPPING);

		log.info("stopping all jobs in ScheduleManager [{}]", ()->this);
		List<IJob> scheduledJobs = getSchedulesList();
		Collections.reverse(scheduledJobs);
		for (IJob jobDef : scheduledJobs) {
			log.info("removing trigger for JobDef [{}]", jobDef::getName);
			try {
				getSchedulerHelper().deleteTrigger(jobDef);
			}
			catch (SchedulerException se) {
				log.error("unable to remove scheduled job [{}]", jobDef, se);
			}
		}

		updateState(RunState.STOPPED);
	}

	@Override
	public void close() throws Exception {
		if(!inState(RunState.STOPPED)) {
			stop(); //Call this just in case...
		}

		while (!getSchedulesList().isEmpty()) {
			IJob job = getSchedulesList().get(0);
			unRegister(job);
		}
	}

	/**
	 * Job that is executed periodically. The time of execution can be configured within the job
	 * or from outside the configuration through the Frank!Console.
	 */
	public void registerScheduledJob(IJob job) {
		if(!inState(RunState.STOPPED)) {
			log.warn("cannot add JobDefinition, manager in state [{}]", this::getState);
		}

		log.debug("registering JobDef [{}] with ScheduleManager [{}]", ()->job, ()->this);
		if(job.getName() == null) {
			throw new IllegalStateException("JobDef has no name");
		}
		if(schedules.containsKey(job.getName())) {
			throw new IllegalStateException("JobDef [" + job.getName() + "] already registered.");
		}

		schedules.put(job.getName(), job);
	}

	public void unRegister(IJob job) {
		String name = job.getName();

		schedules.remove(name);
		log.debug("unregistered JobDef [{}] from ScheduleManager [{}]", ()->name, ()->this);
	}

	public final Map<String, IJob> getSchedules() {
		return Collections.unmodifiableMap(schedules);
	}

	public List<IJob> getSchedulesList() {
		return new ArrayList<>(getSchedules().values());
	}

	public IJob getSchedule(String name) {
		return getSchedules().get(name);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
		builder.append(" state ["+getState()+"]");
		builder.append(" schedules ["+schedules.size()+"]");
		if(applicationContext != null) {
			builder.append(" applicationContext ["+applicationContext.getDisplayName()+"]");
		}
		return builder.toString();
	}
}
