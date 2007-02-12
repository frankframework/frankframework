/*
 * $Log: SchedulerSender.java,v $
 * Revision 1.2  2007-02-12 14:08:01  europe\L190409
 * Logger from LogUtil
 *
 * Revision 1.1  2005/11/01 08:51:13  John Dekker <john.dekker@ibissource.org>
 * Add support for dynamic scheduling, i.e. add a job to the scheduler using 
 * a sender
 *
 */
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;

/**
 * Registers a trigger in the scheduler so that the message is send to a javalistener
 * at a scheduled time.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.scheduler.SchedulerSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJavaListener(String) javaListener}</td><td>Java listener to be called when scheduler trigger fires</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCronExpressionPattern(String) cronExpressionPattern}</td><td>Expression that generates the cron trigger</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJobGroup(String) jobGroup}</td><td>Job group in which the new trigger is to be created (optional)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJobNamePattern(String) jobNamePattern}</td><td>Pattern that leads to the name of the registered trigger(optional)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author John Dekker
 */
public class SchedulerSender extends SenderWithParametersBase {
	protected Logger log = LogUtil.getLogger(this);
	
	public static final String JAVALISTENER = "javaListener";
	public static final String CORRELATIONID = "correlationId";
	public static final String MESSAGE = "message";
	
	private String javaListener;
	private String cronExpressionPattern;
	private String jobGroup;
	private String jobNamePattern;
	
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(javaListener)) {
			throw new ConfigurationException("Property [serviceName] is empty");
		}
		if (StringUtils.isEmpty(cronExpressionPattern)) {
			throw new ConfigurationException("Property [cronExpressionPattern] is empty");
		}
		
		Parameter p = new Parameter();
		p.setName("_cronexpression");
		p.setPattern(cronExpressionPattern);
		addParameter(p);

		if (StringUtils.isNotEmpty(jobNamePattern)) {
			p = new Parameter();
			p.setName("_jobname");
			p.setPattern(jobNamePattern);
			addParameter(p);
		}
		
		super.configure();
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		try {
			ParameterValueList values = prc.getValues(paramList);
			String jobName = getName() + correlationID;
			String cronExpression = values.getParameterValue("_cronexpression").getValue().toString();
			if (StringUtils.isNotEmpty(jobNamePattern)) {
				jobName = values.getParameterValue("_jobname").getValue().toString();;	
			}
			schedule(jobName, cronExpression, correlationID, message);
			return jobName;
		}
		catch(SenderException e) {
			throw e;
		}
		catch(Exception e) {
			throw new SenderException("Error during scheduling " + message, e);
		}
	}

	/*
	 * Schedule the job
	 */
	private void schedule(String jobName, String cronExpression, String correlationId, String message) throws Exception {
		JobDetail jobDetail = new JobDetail(jobName, Scheduler.DEFAULT_GROUP, ServiceJob.class);
		jobDetail.getJobDataMap().put(JAVALISTENER, javaListener);
		jobDetail.getJobDataMap().put(MESSAGE, message);
		jobDetail.getJobDataMap().put(CORRELATIONID, correlationId);

		if (StringUtils.isEmpty(jobGroup))
			SchedulerHelper.scheduleJob(jobName, jobDetail, cronExpression, false);
		else 
			SchedulerHelper.scheduleJob(jobName, jobGroup, jobDetail, cronExpression, false);

		if (log.isDebugEnabled()) {
			log.debug("SchedulerSender ["+ getName() +"] has send job [" + jobName + "] to the scheduler");
		}
	}
	public void setCronExpressionPattern(String string) {
		cronExpressionPattern = string;
	}

	public void setJobGroup(String string) {
		jobGroup = string;
	}

	public void setJobNamePattern(String string) {
		jobNamePattern = string;
	}

	public void setJavaListener(String string) {
		javaListener = string;
	}

}
