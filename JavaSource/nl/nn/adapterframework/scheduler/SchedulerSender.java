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
/*
 * $Log: SchedulerSender.java,v $
 * Revision 1.8  2011-11-30 13:51:42  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2010/03/10 14:30:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.4  2007/10/10 09:40:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * version using IbisManager
 *
 * Revision 1.3  2007/02/26 16:52:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * startScheduler on open()
 *
 * Revision 1.2  2007/02/12 14:08:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import org.apache.commons.lang.StringUtils;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

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
	
	public static final String JAVALISTENER = "javaListener";
	public static final String CORRELATIONID = "correlationId";
	public static final String MESSAGE = "message";
	
	private String javaListener;
	private String cronExpressionPattern;
	private String jobGroup;
	private String jobNamePattern;
	private SchedulerHelper schedulerHelper;
    
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

	public void open() throws SenderException {
		super.open();
		try {
			schedulerHelper.startScheduler();
		} catch (SchedulerException e) {
			throw new SenderException("Could not start Scheduler", e);
		}
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
			schedulerHelper.scheduleJob(jobName, jobDetail, cronExpression, false);
		else 
			schedulerHelper.scheduleJob(jobName, jobGroup, jobDetail, cronExpression, false);

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

    public SchedulerHelper getSchedulerHelper() {
        return schedulerHelper;
    }

    public void setSchedulerHelper(SchedulerHelper helper) {
        schedulerHelper = helper;
    }

}
