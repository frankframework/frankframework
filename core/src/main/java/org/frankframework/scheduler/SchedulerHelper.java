/*
   Copyright 2013, 2015, 2019 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.scheduler.job.IJob;

/**
 * The SchedulerHelper encapsulates the quarz scheduler.
 *
 * @author John Dekker
 */
@Log4j2
public class SchedulerHelper {

	public static final String DEFAULT_GROUP = Scheduler.DEFAULT_GROUP;

	private @Getter @Setter Scheduler scheduler;

	public static void validateJob(JobDetail jobDetail, String cronExpression) throws ConfigurationException {
		TriggerKey triggerKey = TriggerKey.triggerKey(jobDetail.getKey().getName(), jobDetail.getKey().getGroup());
		if (StringUtils.isNotEmpty(cronExpression)) {
			try {
				createCronTrigger(triggerKey, cronExpression);
			} catch (Exception e) {
				throw new ConfigurationException("invalid job", e);
			}
		}
	}

	public void scheduleJob(IJob jobdef) throws SchedulerException {
		JobDetail jobDetail = jobdef.getJobDetail();
		scheduleJob(jobDetail, jobdef.getCronExpression(), jobdef.getInterval(), true);
	}

	/**
	 * Schedule a new job
	 * @param jobDetail
	 * @param cronExpression null or cron expression in quartz format
	 */
	public void scheduleJob(JobDetail jobDetail, String cronExpression) throws SchedulerException {
		scheduleJob(jobDetail, cronExpression, -1, false);
	}

	/**
	 * Schedule a new job
	 * @param jobDetail
	 * @param interval 0 or interval when to trigger
	 */
	public void scheduleJob(JobDetail jobDetail, long interval) throws SchedulerException {
		scheduleJob(jobDetail, null, interval, false);
	}

	/**
	 * Schedule a new job
	 * @param jobDetail
	 * @param cronExpression null or cron expression in quartz format
	 * @param interval 0 (trigger once) or interval (in ms) when to trigger
	 * @param overwrite overwrite existing {@link ConfiguredJob jobs}.
	 */
	public void scheduleJob(JobDetail jobDetail, String cronExpression, long interval, boolean overwrite) throws SchedulerException {

		// if the job already exists, remove it.
		if (scheduler.checkExists(jobDetail.getKey())) {
			if (overwrite)
				scheduler.unscheduleJob(TriggerKey.triggerKey(jobDetail.getKey().getName(), jobDetail.getKey().getGroup()));
			else
				throw new SchedulerException("Job with name [" + jobDetail.getKey().getName() + "] already exists");
		}

		TriggerKey triggerKey = TriggerKey.triggerKey(jobDetail.getKey().getName(), jobDetail.getKey().getGroup());
		if (StringUtils.isNotEmpty(cronExpression)) {
			scheduler.scheduleJob(jobDetail, createCronTrigger(triggerKey, cronExpression));
		} else if (interval > -1) {
			SimpleScheduleBuilder schedule = simpleSchedule();
			if(interval == 0) {
				// Keep trigger active to keep it available in GUI
				schedule.withIntervalInHours(876000);
				schedule.withRepeatCount(1);
			} else {
				schedule.withIntervalInMilliseconds(interval).repeatForever();
			}

			SimpleTrigger simpleTrigger = newTrigger()
					.withIdentity(triggerKey)
					.forJob(jobDetail).withSchedule(schedule).build();
			scheduler.scheduleJob(jobDetail, simpleTrigger);
		} else {
			log.error("no cronexpression or interval for job [{}], cannot schedule", jobDetail::getKey);
		}
	}

	protected static CronTrigger createCronTrigger(TriggerKey triggerKey, String cronExpression) {
		return newTrigger()
				.withIdentity(triggerKey)
				.withSchedule(cronSchedule(cronExpression))
				.build();
	}

	public boolean contains(String name) throws SchedulerException {
		return contains(name, null);
	}

	public boolean contains(String name, String group) throws SchedulerException {
		JobKey key = null;

		if(StringUtils.isEmpty(group))
			key = JobKey.jobKey(name, DEFAULT_GROUP);
		else
			key = JobKey.jobKey(name, group);

		return scheduler.checkExists(key);
	}

	public Trigger getTrigger(String name) throws SchedulerException {
		return getTrigger(name, null);
	}

	public Trigger getTrigger(String name, String group) throws SchedulerException {
		TriggerKey key = null;
		if(StringUtils.isEmpty(group))
			key = TriggerKey.triggerKey(name, DEFAULT_GROUP);
		else
			key = TriggerKey.triggerKey(name, group);

		return scheduler.getTrigger(key);
	}

	public JobDetail getJobDetail(String jobName) throws SchedulerException {
		return getJobDetail(jobName, DEFAULT_GROUP);
	}

	public JobDetail getJobDetail(String jobName, String jobGroup) throws SchedulerException {
		return scheduler.getJobDetail(JobKey.jobKey(jobName, jobGroup));
	}

	public void deleteTrigger(IJob jobDef) throws SchedulerException {
		deleteTrigger(jobDef.getName(), jobDef.getJobGroup());
	}

	public void deleteTrigger(String jobName) throws SchedulerException {
		deleteTrigger(jobName, DEFAULT_GROUP);
	}

	public void deleteTrigger(String name, String group) throws SchedulerException {
		TriggerKey key = null;
		if(StringUtils.isEmpty(group))
			key = TriggerKey.triggerKey(name, DEFAULT_GROUP);
		else
			key = TriggerKey.triggerKey(name, group);

		getScheduler().unscheduleJob(key);
	}
}
