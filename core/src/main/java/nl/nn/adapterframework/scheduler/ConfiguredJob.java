/*
   Copyright 2013, 2019 Nationale-Nederlanden

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

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import nl.nn.adapterframework.configuration.IbisManager;



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
 * <p>Currently, the {@link nl.nn.adapterframework.configuration.Configuration configuration}
 * is stored in the job data map. As the configuration is not serializable, due to the nature of the
 * adapters, the quartz database support cannot be used.
 * </p>
 *
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.core.IAdapter
 * @see nl.nn.adapterframework.configuration.Configuration
  */
public class ConfiguredJob extends BaseJob {

	public static final String MANAGER_KEY = "manager";
	public static final String JOBDEF_KEY = "jobdef";

	public void execute(JobExecutionContext context) throws JobExecutionException {
		String ctName = Thread.currentThread().getName();
		try {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			IbisManager ibisManager = (IbisManager)dataMap.get(MANAGER_KEY);
			JobDef jobDef = (JobDef)dataMap.get(JOBDEF_KEY);
			Thread.currentThread().setName(jobDef.getName() + "["+ctName+"]");
			log.info(getLogPrefix(jobDef) + "executing");
			jobDef.executeJob(ibisManager);
			log.debug(getLogPrefix(jobDef) + "completed");
		}
		catch (Exception e) {
			log.error(e);
			throw new JobExecutionException(e, false);
		}
		finally {
			Thread.currentThread().setName(ctName);
		}
	}
	
}
