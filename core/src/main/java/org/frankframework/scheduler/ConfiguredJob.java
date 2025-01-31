/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2021 WeAreFrank!

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

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.scheduler.job.IJob;


/**
 * Job, specified in Configuration.xml, for executing things to do with an adapter, like starting or stopping it.
 * The <a href="http://quartz.sourceforge.net">Quartz scheduler</a> is used for scheduling.
 * <p>
 * Expects a JobDetail with a datamap with the following fields:
 * <ul>
 * <li>function: the function to do, possible values:  "startreceiver","stopadapter",  "stopreceiver" and "stopadapter"</li>
 * <li>config: the Configuration object</li>
 * <li>adapterName: the name of the adapter<li>
 * <li>receiverName: the name of the receiver<li>
 * </ul>
 *<p><b>Design consideration</b></p>
 * <p>Currently, the {@link Configuration} is stored in the job data map. As the configuration is not serializable, due to the nature of the
 * adapters, the quartz database support cannot be used.
 * </p>
 *
 * @author  Johan Verrips
 * @see Adapter
 * @see Configuration
  */
@Log4j2
public class ConfiguredJob implements Job {

	public static final String JOBDEF_KEY = "jobdef";

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String ctName = Thread.currentThread().getName();
		try {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			IJob jobDef = (IJob)dataMap.get(JOBDEF_KEY);
			Thread.currentThread().setName(jobDef.getName() + "["+ctName+"]");
			if (log.isTraceEnabled()) log.trace("{}executing", getLogPrefix(jobDef));
			jobDef.executeJob();
			if (log.isTraceEnabled()) log.trace("{}completed", getLogPrefix(jobDef));
		}
		catch (Exception e) {
			log.error("JobExecutionException while running {}", getLogPrefix(context), e);
			throw new JobExecutionException(e, false);
		}
		finally {
			Thread.currentThread().setName(ctName);
		}
	}

	private String getLogPrefix(JobExecutionContext context) {
		String instName = context.getJobDetail().getKey().getName();
		return "Job [" + instName + "] ";
	}

	private String getLogPrefix(IJob jobDef) {
		String instName = jobDef.getName();
		return "Job [" + instName + "] ";
	}
}
