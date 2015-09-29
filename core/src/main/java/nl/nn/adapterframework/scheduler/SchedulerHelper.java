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
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

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
		if ((sched.getTrigger(jobDetail.getName(), jobDetail.getGroup())) != null) {
			if (overwrite)
				sched.unscheduleJob(jobDetail.getName(), jobDetail.getGroup());
			else
				throw new SchedulerException("Job with name [" + jobDetail.getName() + "] already exists");
		}
 
		if (StringUtils.isNotEmpty(cronExpression)) {
			CronTrigger cronTrigger = new CronTrigger(jobDetail.getName(), jobDetail.getGroup());
			cronTrigger.setCronExpression(cronExpression);
			sched.scheduleJob(jobDetail, cronTrigger);
		} else if (interval > -1) {
			SimpleTrigger simpleTrigger = new SimpleTrigger(jobDetail.getName(), jobDetail.getGroup());
			if (interval == 0) {
				// Keep trigger active to keep it available in GUI
				simpleTrigger.setRepeatCount(1);
				simpleTrigger.setRepeatInterval(1000L * 60L * 60L * 24L * 365L * 100L);
			} else {
				// New Quartz version seems to have repeatForever(), for now set
				// repeat count to Integer.MAX_VALUE.
				simpleTrigger.setRepeatCount(Integer.MAX_VALUE);
				simpleTrigger.setRepeatInterval(interval);
			}
			sched.scheduleJob(jobDetail, simpleTrigger);
		} else {
			log.warn("no cronexpression or interval for job [" + jobDetail.getName() + "], cannot schedule");
		}
	}

	public Trigger getTrigger(String jobName) throws SchedulerException {
		return getTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}

	public Trigger getTrigger(String jobName, String jobGroup) throws SchedulerException {
		return getScheduler().getTrigger(jobName, jobGroup);
	}

	public JobDetail getJobForTrigger(String jobName) throws SchedulerException {
		return getJobForTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}

	public JobDetail getJobForTrigger(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = getScheduler();

		Trigger t = sched.getTrigger(jobName, jobGroup);
		String name = t.getJobName();
		String group = t.getJobGroup();

		return sched.getJobDetail(name, group);
	}

	public void deleteTrigger(String jobName) throws SchedulerException {
		deleteTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}

	public void deleteTrigger(String jobName, String jobGroup) throws SchedulerException {
		getScheduler().unscheduleJob(jobName, jobGroup);
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
