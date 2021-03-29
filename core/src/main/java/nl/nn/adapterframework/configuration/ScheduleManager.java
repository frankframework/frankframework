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

import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.util.LogUtil;

public class ScheduleManager implements ApplicationContextAware, AutoCloseable, Lifecycle {
	protected final Logger log = LogUtil.getLogger(this);

	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter SchedulerHelper schedulerHelper;
	private final Map<String, JobDef> schedules = new LinkedHashMap<>();

	private enum BootState {
		STARTING, STARTED, STOPPING, STOPPED;
	}
	private BootState state = BootState.STARTING;

	/**
	 * Configure and start, managed through the Spring Lifecyle
	 */
	@Override
	public void start() {
		if(state != BootState.STARTING) {
			return;
		}

		for (JobDef jobdef : getSchedulesList()) {
			try {
				jobdef.configure();
				schedulerHelper.scheduleJob(jobdef);
				log.info("job scheduled with properties :" + jobdef.toString());
			} catch (Exception e) {
				log.error("Could not schedule job [" + jobdef.getName() + "] cron [" + jobdef.getCronExpression() + "]", e);
			}
		}

		try {
			schedulerHelper.startScheduler();
			log.info("Scheduler started");
		} catch (SchedulerException e) {
			log.error("Could not start scheduler", e);
		}

		state = BootState.STARTED;
	}

	/**
	 * remove all registered jobs
	 */
	@Override
	public void stop() {
		if(state != BootState.STARTED) {
			return;
		}

		state = BootState.STOPPING;
		log.info("stopping all adapters in AdapterManager ["+this+"]");
		List<JobDef> schedules = getSchedulesList();
		Collections.reverse(schedules);
		for (JobDef jobdef : schedules) {
			log.info("removing trigger for JobDef [" + jobdef.getName() + "]");
			try {
				getSchedulerHelper().deleteTrigger(jobdef);
			}
			catch (SchedulerException se) {
				log.error("unable to remove scheduled job ["+jobdef+"]", se);
			}
		}
		state = BootState.STOPPED;
	}

	@Override
	public void close() throws Exception {
		stop(); //Call this just in case...

		while (getSchedulesList().size() > 0) {
			JobDef job = getSchedulesList().get(0);
			unRegister(job);
		}
	}

	public void register(JobDef job) {
		if(state != BootState.STARTING) {
			log.warn("cannot add JobDefinition, manager in state ["+state.name()+"]");
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

	public void unRegister(JobDef job) {
		String name = job.getName();

		schedules.remove(name);
		if(log.isDebugEnabled()) log.debug("unregistered JobDef ["+name+"] from ScheduleManager ["+this+"]");
	}

	public final Map<String, JobDef> getSchedules() {
		return Collections.unmodifiableMap(schedules);
	}

	public List<JobDef> getSchedulesList() {
		return new ArrayList<>(getSchedules().values());
	}

	public JobDef getSchedule(String name) {
		return getSchedules().get(name);
	}

	@Override
	public boolean isRunning() {
		return state == BootState.STARTED;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
		builder.append(" state ["+state+"]");
		builder.append(" schedules ["+schedules.size()+"]");
		if(applicationContext != null) {
			builder.append(" applicationContext ["+applicationContext.getDisplayName()+"]");
		}
		return builder.toString();
	}
}
