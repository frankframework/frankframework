/*
   Copyright 2013, 2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.scheduler;

import java.text.ParseException;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

/**
 * The SchedulerHelper encapsulates the quarz scheduler.
 *
 * @author John Dekker
 */
public class SchedulerHelper {
	protected static Logger log = LogUtil.getLogger(SchedulerHelper.class);

    private Scheduler scheduler;

	public void scheduleJob(IbisManager ibisManager, JobDef jobdef) throws Exception {
		JobDetail jobDetail = jobdef.getJobDetail(ibisManager);
		scheduleJob(jobDetail, jobdef.getCronExpression(), jobdef.getInterval(), true);
	}

	public void scheduleJob(JobDetail jobDetail, String cronExpression, long interval, boolean overwrite) throws SchedulerException, ParseException {
		Scheduler sched = getScheduler();

		// if the job already exists, remove it.
		if ((sched.getTrigger(TriggerKey.triggerKey(jobDetail.getKey().getName(), jobDetail.getKey().getGroup()))) != null) {
			if (overwrite)
				sched.unscheduleJob(TriggerKey.triggerKey(jobDetail.getKey().getName(), jobDetail.getKey().getGroup()));
			else
				throw new SchedulerException("Job with name [" + jobDetail.getKey().getName() + "] already exists");
		}

		if (StringUtils.isNotEmpty(cronExpression)) {
			CronTrigger cronTrigger = newTrigger()
					.withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
					.withSchedule(cronSchedule(cronExpression))
					.build();
			
			sched.scheduleJob(jobDetail, cronTrigger);
		} else if (interval > -1) {
			SimpleTrigger simpleTrigger;
			
			if(interval == 0) {
				simpleTrigger = newTrigger()
						.withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
						.forJob(jobDetail)
						.withSchedule(simpleSchedule()
								.withIntervalInSeconds(60 * 60 * 24 * 365 * 100)
								.withRepeatCount(1))
						.build();
			} else {
				simpleTrigger = newTrigger()
						.withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
						.forJob(jobDetail)
						.withSchedule(simpleSchedule()
								.withIntervalInSeconds((int)interval)
								.repeatForever())
						.build();
			}
			
			sched.scheduleJob(jobDetail, simpleTrigger);
		} else {
			log.warn("no cronexpression or interval for job [" + jobDetail.getKey().getName() + "], cannot schedule");
		}
	}

	public Trigger getTrigger(String jobName) throws SchedulerException {
		return getTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}

	public Trigger getTrigger(String jobName, String jobGroup) throws SchedulerException {
		return getScheduler().getTrigger(TriggerKey.triggerKey(jobName, jobGroup));
	}

	public JobDetail getJobForTrigger(String jobName) throws SchedulerException {
		return getJobForTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}

	public JobDetail getJobForTrigger(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = getScheduler();

		Trigger t = sched.getTrigger(TriggerKey.triggerKey(jobName, jobGroup));
		String name = t.getJobKey().getName();
		String group = t.getJobKey().getGroup();

		return sched.getJobDetail(JobKey.jobKey(name, group));
	}

	public void deleteTrigger(String jobName) throws SchedulerException {
		deleteTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}

	public void deleteTrigger(String jobName, String jobGroup) throws SchedulerException {
		getScheduler().unscheduleJob(TriggerKey.triggerKey(jobName, jobGroup));
	}

	public Scheduler getScheduler() throws SchedulerException {
		return scheduler;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void startScheduler() throws SchedulerException {
		Scheduler scheduler = getScheduler();
		if (scheduler != null && !scheduler.isStarted()) {
			log.info("Starting Scheduler");
			scheduler.start();
		}
	}


}
