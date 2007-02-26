/*
 * $Log: SchedulerHelper.java,v $
 * Revision 1.3  2007-02-26 16:50:09  europe\L190409
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

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

/**
 * The SchedulerHelper encapsulates the quarz scheduler.
 * 
 * @author John Dekker
 */
public class SchedulerHelper {
	protected static Logger log = LogUtil.getLogger(SchedulerHelper.class);
	
	public static void scheduleJob(Configuration config, JobDef jobdef) throws Exception {
		JobDetail jobDetail = new JobDetail(jobdef.getName(), // job name
			Scheduler.DEFAULT_GROUP, // job group
			AdapterJob.class); // the java class to execute

		jobDetail.getJobDataMap().put("config", config); // reference to configuration.
		jobDetail.getJobDataMap().put("adapterName", jobdef.getAdapterName());
		jobDetail.getJobDataMap().put("receiverName", jobdef.getReceiverName());
		jobDetail.getJobDataMap().put("function", jobdef.getFunction());
		
		if (StringUtils.isNotEmpty(jobdef.getDescription()))
			jobDetail.setDescription(jobdef.getDescription());

		scheduleJob(jobdef.getName(), jobDetail, jobdef.getCronExpression(), true);
	}
	
	public static void scheduleJob(String jobName, JobDetail jobDetail, String cronExpression, boolean overwrite) throws SchedulerException, ParseException {
		scheduleJob(jobName, Scheduler.DEFAULT_GROUP, jobDetail, cronExpression, overwrite);
	}
	
	public static void scheduleJob(String jobName, String jobGroup, JobDetail jobDetail, String cronExpression, boolean overwrite) throws SchedulerException, ParseException {
		Scheduler sched = getScheduler();

		// if the job already exists, remove it.
		if ((sched.getTrigger(jobName, jobGroup)) != null) {
			if (overwrite)
				sched.unscheduleJob(jobName, jobGroup);
			else
				throw new SchedulerException("Job with name [" + jobName + "] already exists");
		}

		CronTrigger cronTrigger = new CronTrigger(jobName, jobGroup);
		cronTrigger.setCronExpression(cronExpression);
		sched.scheduleJob(jobDetail, cronTrigger);
	}
	
	public static Trigger getTrigger(String jobName) throws SchedulerException {
		return getTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}

	public static Trigger getTrigger(String jobName, String jobGroup) throws SchedulerException {
		return getScheduler().getTrigger(jobName, jobGroup);
	}

	public static JobDetail getJobForTrigger(String jobName) throws SchedulerException {
		return getJobForTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}

	public static JobDetail getJobForTrigger(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = getScheduler();
		
		Trigger t = sched.getTrigger(jobName, jobGroup);
		String name = t.getJobName();
		String group = t.getJobGroup();
		
		return sched.getJobDetail(name, group);
	}
	
	public static void deleteTrigger(String jobName) throws SchedulerException {
		deleteTrigger(jobName, Scheduler.DEFAULT_GROUP);
	}
	
	public static void deleteTrigger(String jobName, String jobGroup) throws SchedulerException {
		getScheduler().unscheduleJob(jobName, jobGroup);
	}
	
	public static Scheduler getScheduler() throws SchedulerException {
		SchedulerFactory schedFact = new StdSchedulerFactory();
		Scheduler sched = schedFact.getScheduler();
		return sched;		
	}

	public static void startScheduler() throws SchedulerException {
		Scheduler scheduler = SchedulerHelper.getScheduler();
		if (scheduler.isPaused()) {
			log.info("Starting Scheduler");
			scheduler.start();
		}
	}
	
	
}
