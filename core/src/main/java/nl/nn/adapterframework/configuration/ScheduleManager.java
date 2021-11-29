/*
   Copyright 2021 WeAreFrank!

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
import nl.nn.adapterframework.lifecycle.ConfiguringLifecycleProcessor;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.scheduler.job.IJob;

/**
 * Configure/start/stop lifecycles are managed by Spring. See {@link ConfiguringLifecycleProcessor}
 *
 * @author Niels Meijer
 */
public class ScheduleManager extends ConfigurableLifecyleBase implements ApplicationContextAware, AutoCloseable {

	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter SchedulerHelper schedulerHelper;
	private final Map<String, IJob> schedules = new LinkedHashMap<>();

	@Override
	public void configure() {
		if(!inState(BootState.STOPPED)) {
			log.warn("unable to configure ["+this+"] while in state ["+getState()+"]");
			return;
		}
		updateState(BootState.STARTING);

		for (IJob jobdef : getSchedulesList()) {
			try {
				jobdef.configure();
				log.info("job scheduled with properties :" + jobdef.toString());
			} catch (Exception e) {
				log.error("Could not schedule job [" + jobdef.getName() + "] cron [" + jobdef.getCronExpression() + "]", e);
			}
		}
	}

	/**
	 * Configure and start, managed through the Spring Lifecyle
	 */
	@Override
	public void start() {
		if(!inState(BootState.STARTING)) {
			log.warn("unable to start ["+this+"] while in state ["+getState()+"]");
			return;
		}

		for (IJob jobdef : getSchedulesList()) {
			if(jobdef.isConfigured()) {
				try {
					schedulerHelper.scheduleJob(jobdef);
					log.info("job scheduled with properties :" + jobdef.toString());
				} catch (SchedulerException e) {
					log.error("Could not schedule job [" + jobdef.getName() + "] cron [" + jobdef.getCronExpression() + "]", e);
				}
			} else {
				log.info("Could not schedule job [" + jobdef.getName() + "] as it is not configured");
			}
		}

		try {
			schedulerHelper.startScheduler();
			log.info("Scheduler started");
		} catch (SchedulerException e) {
			log.error("Could not start scheduler", e);
		}

		updateState(BootState.STARTED);
	}

	/**
	 * remove all registered jobs
	 */
	@Override
	public void stop() {
		if(!inState(BootState.STARTED)) {
			log.warn("forcing ["+this+"] to stop while in state ["+getState()+"]");
		}
		updateState(BootState.STOPPING);

		log.info("stopping all adapters in AdapterManager ["+this+"]");
		List<IJob> scheduledJobs = getSchedulesList();
		Collections.reverse(scheduledJobs);
		for (IJob jobDef : scheduledJobs) {
			log.info("removing trigger for JobDef [" + jobDef.getName() + "]");
			try {
				getSchedulerHelper().deleteTrigger(jobDef);
			}
			catch (SchedulerException se) {
				log.error("unable to remove scheduled job ["+jobDef+"]", se);
			}
		}

		updateState(BootState.STOPPED);
	}

	@Override
	public void close() throws Exception {
		if(!inState(BootState.STOPPED)) {
			stop(); //Call this just in case...
		}

		while (!getSchedulesList().isEmpty()) {
			IJob job = getSchedulesList().get(0);
			unRegister(job);
		}
	}

	public void register(IJob job) {
		if(!inState(BootState.STOPPED)) {
			log.warn("cannot add JobDefinition, manager in state ["+getState()+"]");
		}

		if(log.isDebugEnabled()) log.debug("registering JobDef ["+job+"] with ScheduleManager ["+this+"]");
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
		if(log.isDebugEnabled()) log.debug("unregistered JobDef ["+name+"] from ScheduleManager ["+this+"]");
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
