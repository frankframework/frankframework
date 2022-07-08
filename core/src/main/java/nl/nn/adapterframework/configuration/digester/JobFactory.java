/*
   Copyright 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.configuration.digester;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.JobDefFunctions;
import nl.nn.adapterframework.scheduler.job.IbisActionJob;
import nl.nn.adapterframework.scheduler.job.Job;
import nl.nn.adapterframework.scheduler.job.SendMessageJob;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * Factory for instantiating Schedules Jobs from the Digester framework.
 * Instantiates the job based on the function specified.
 *
 * @author Niels Meijer
 */
public class JobFactory extends GenericFactory {

	@Override
	public Object createObject(Map<String, String> attrs) throws ClassNotFoundException {
		String className = attrs.get("className");
		if(StringUtils.isEmpty(className) || className.equals(Job.class.getCanonicalName())) { //Default empty, filled when using new pre-parsing
			String function = attrs.get("function");
			if(StringUtils.isEmpty(function)) {
				throw new IllegalArgumentException("function may not be empty");
			}

			className = determineClassNameFromFunction(function);
			attrs.put("className", className);
		}
		return super.createObject(attrs);
	}

	private String determineClassNameFromFunction(String functionName) {
		JobDefFunctions function = EnumUtils.parse(JobDefFunctions.class, functionName);
		Class<?> clazz = function.getJobClass();

		return clazz.getCanonicalName();
	}

	public static JobDef createJob(IAdapter adapter, String receiverName, String message, String functionName) {
		JobDefFunctions function = StringUtils.isNotEmpty(functionName) ? EnumUtils.parse(JobDefFunctions.class, functionName) : JobDefFunctions.SEND_MESSAGE;

		switch(function) {
		case SEND_MESSAGE:
			SendMessageJob sendMessageJob = SpringUtils.createBean(adapter.getApplicationContext(), SendMessageJob.class);
			sendMessageJob.setAdapterName(adapter.getName());
			sendMessageJob.setJavaListener(receiverName);
			sendMessageJob.setMessage(message);
			return sendMessageJob;
		case START_ADAPTER:
		case STOP_ADAPTER:
		case START_RECEIVER:
		case STOP_RECEIVER:
			IbisActionJob ibisActionJob = SpringUtils.createBean(adapter.getApplicationContext(), IbisActionJob.class);
			ibisActionJob.setAdapterName(adapter.getName());
			ibisActionJob.setReceiverName(receiverName);
			ibisActionJob.setAction(EnumUtils.parse(IbisActionJob.Action.class, "function", function.name()));
			return ibisActionJob;
		default:
			throw new IllegalArgumentException("Job function ["+function+"] is not supported as Database job");
		}
	}

	public static void mapFields(JobDef jobDef, Map<String, Object> jobData) {
		if (jobDef instanceof SendMessageJob) {
			SendMessageJob job = (SendMessageJob) jobDef;
			jobData.put("adapter", job.getAdapterName());
			jobData.put("listener", job.getJavaListener());
			jobData.put("message", job.getMessage());
			jobData.put("action", JobDefFunctions.SEND_MESSAGE);
			return;
		}
		if (jobDef instanceof IbisActionJob) {
			IbisActionJob job = (IbisActionJob) jobDef;
			jobData.put("adapter", job.getAdapterName());
			jobData.put("listener", job.getReceiverName());
			jobData.put("action", job.getIbisAction());
			return;
		}
	}

}
