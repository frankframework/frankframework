/*
   Copyright 2013, 2015, 2019 Nationale-Nederlanden, 2022 WeAreFrank!

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

import static org.quartz.JobBuilder.newJob;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.senders.SenderWithParametersBase;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;

/**
 * Registers a trigger in the scheduler so that the message is sent to a {@link org.frankframework.receivers.JavaListener}
 * at a scheduled time.
 * 
 * NOTE: This element has been removed. Please use the database schedules to manage dynamic schedules.
 *
 * @author John Dekker
 */
@Deprecated(forRemoval = true, since = "8.0.5")
public class SchedulerSender extends SenderWithParametersBase {

	private String javaListener;
	private String cronExpressionPattern;
	private String jobGroup = null;
	private String jobNamePattern;
	private SchedulerHelper schedulerHelper;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(javaListener)) {
			throw new ConfigurationException("Property [serviceName] is empty");
		}
		if (StringUtils.isEmpty(cronExpressionPattern)) {
			throw new ConfigurationException("Property [cronExpressionPattern] is empty");
		}

		Parameter p = SpringUtils.createBean(getApplicationContext(), Parameter.class);
		p.setName("_cronexpression");
		p.setPattern(cronExpressionPattern);
		addParameter(p);

		if (StringUtils.isNotEmpty(jobNamePattern)) {
			p = SpringUtils.createBean(getApplicationContext(), Parameter.class);
			p.setName("_jobname");
			p.setPattern(jobNamePattern);
			addParameter(p);
		}
		super.configure();
	}

	@Override
	public void open() throws SenderException {
		super.open();
		try {
			schedulerHelper.startScheduler();
		} catch (SchedulerException e) {
			throw new SenderException("Could not start Scheduler", e);
		}
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException {
		try {
			String correlationID = session==null ? "" : session.getCorrelationId();
			ParameterValueList values = paramList.getValues(message, session);
			String jobName = getName() + correlationID;
			String cronExpression = values.get("_cronexpression").asStringValue();
			if (StringUtils.isNotEmpty(jobNamePattern)) {
				jobName = values.get("_jobname").asStringValue();
			}
			schedule(jobName, cronExpression, correlationID, message.asString());
			return new SenderResult(jobName);
		} catch(SenderException e) {
			throw e;
		} catch(Exception e) {
			throw new SenderException("Error during scheduling " + message, e);
		}
	}

	private void schedule(String jobName, String cronExpression, String correlationId, String message) throws Exception {

		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put(ServiceJob.JAVALISTENER_KEY, javaListener);
		jobDataMap.put(ServiceJob.MESSAGE_KEY, message);
		jobDataMap.put(ServiceJob.CORRELATIONID_KEY, correlationId);

		JobDetail jobDetail = newJob(ServiceJob.class)
				.withIdentity(jobName, jobGroup)
				.usingJobData(jobDataMap)
				.build();

		schedulerHelper.scheduleJob(jobDetail, cronExpression);
		if (log.isDebugEnabled()) {
			log.debug("SchedulerSender ["+ getName() +"] has send job [" + jobName + "] to the scheduler");
		}
	}

	/** expression that generates the cron trigger */
	public void setCronExpressionPattern(String string) {
		cronExpressionPattern = string;
	}

	/** job group in which the new trigger is to be created (optional) */
	public void setJobGroup(String string) {
		if(StringUtils.isNotEmpty(string))
			jobGroup = string;
		else
			jobGroup = null;
	}

	/** pattern that leads to the name of the registered trigger(optional) */
	public void setJobNamePattern(String string) {
		jobNamePattern = string;
	}

	/** java listener to be called when scheduler trigger fires */
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
