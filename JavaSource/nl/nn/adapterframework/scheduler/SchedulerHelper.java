/*
   Copyright 2013 Nationale-Nederlanden

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

		scheduleJob(jobdef.getName(), jobDetail, jobdef.getCronExpression(), true);
	}
	
	public void scheduleJob(String jobName, JobDetail jobDetail, String cronExpression, boolean overwrite) throws SchedulerException, ParseException {
		scheduleJob(jobName, Scheduler.DEFAULT_GROUP, jobDetail, cronExpression, overwrite);
	}
	
	public void scheduleJob(String jobName, String jobGroup, JobDetail jobDetail, String cronExpression, boolean overwrite) throws SchedulerException, ParseException {
		Scheduler sched = getScheduler();

		// if the job already exists, remove it.
		if ((sched.getTrigger(jobName, jobGroup)) != null) {
			if (overwrite)
				sched.unscheduleJob(jobName, jobGroup);
			else
				throw new SchedulerException("Job with name [" + jobName + "] already exists");
		}

		if (StringUtils.isNotEmpty(cronExpression)) {
			CronTrigger cronTrigger = new CronTrigger(jobName, jobGroup);
			cronTrigger.setCronExpression(cronExpression);
			sched.scheduleJob(jobDetail, cronTrigger);
		} else {
			log.warn("no cronexpression for job [" + jobName + "], cannot schedule");
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
		if (!scheduler.isStarted()) {
			log.info("Starting Scheduler");
			scheduler.start();
		}
	}
	
	
}
