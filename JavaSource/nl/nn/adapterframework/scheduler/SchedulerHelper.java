/*
 * $Log: SchedulerHelper.java,v $
 * Revision 1.7  2010-09-10 11:37:18  L190409
 * added warning for empty cronExpression
 *
 * Revision 1.6  2008/09/04 13:27:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restructured job scheduling
 *
 * Revision 1.5  2007/12/12 09:09:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow for query-type jobs
 *
 * Revision 1.4  2007/10/10 09:40:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * version using IbisManager
 *
 * Revision 1.3  2007/02/26 16:50:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add method startScheduler()
 *
 * Revision 1.2  2007/02/21 16:07:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.1  2005/11/01 08:51:14  John Dekker <john.dekker@ibissource.org>
 * Add support for dynamic scheduling, i.e. add a job to the scheduler using 
 * a sender
 *
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
