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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

import org.apache.commons.lang.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import static org.quartz.JobBuilder.*;


/** 
 * @author John Dekker
 */
@IbisDescription(
	"Registers a trigger in the scheduler so that the message is send to a javalistener \n" + 
	"at a scheduled time. \n" 
)
public class SchedulerSender extends SenderWithParametersBase {
	
	public static final String JAVALISTENER = "javaListener";
	public static final String CORRELATIONID = "correlationId";
	public static final String MESSAGE = "message";
	
	private String javaListener;
	private String cronExpressionPattern;
	private String jobGroup = Scheduler.DEFAULT_GROUP;
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
	void schedule(String jobName, String cronExpression, String correlationId, String message) throws Exception {
		
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put(JAVALISTENER, javaListener);
		jobDataMap.put(MESSAGE, message);
		jobDataMap.put(CORRELATIONID, correlationId);
		
		JobDetail jobDetail = newJob(ServiceJob.class)
				.withIdentity(jobName, jobGroup)
				.usingJobData(jobDataMap) 
				.build();
		
		schedulerHelper.scheduleJob(jobDetail, cronExpression, -1, false);
		if (log.isDebugEnabled()) {
			log.debug("SchedulerSender ["+ getName() +"] has send job [" + jobName + "] to the scheduler");
		}
	}

	@IbisDoc({"expression that generates the cron trigger", ""})
	public void setCronExpressionPattern(String string) {
		cronExpressionPattern = string;
	}

	@IbisDoc({"job group in which the new trigger is to be created (optional)", ""})
	public void setJobGroup(String string) {
		jobGroup = string;
	}

	@IbisDoc({"pattern that leads to the name of the registered trigger(optional)", ""})
	public void setJobNamePattern(String string) {
		jobNamePattern = string;
	}

	@IbisDoc({"java listener to be called when scheduler trigger fires", ""})
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
