/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.scheduler.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.scheduler.IbisJobDetail;
import nl.nn.adapterframework.scheduler.IbisJobDetail.JobType;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * 1. This method first stores all database jobs that can are found in the Quartz Scheduler in a Map.
 * 2. It then loops through all records found in the database.
 * 3. If the job is found, remove it from the Map and compares it with the already existing scheduled job. 
 *    Only if they differ, it overwrites the current job.
 *    If it is not present it add the job to the scheduler.
 * 4. Once it's looped through all the database jobs, loop through the remaining jobs in the Map.
 *    Since they have been removed from the database, remove them from the Quartz Scheduler
 * 
 * @author Niels Meijer
 */
public class LoadDatabaseSchedulesJob extends JobDef {

	@Override
	public void execute() {
		Map<JobKey, IbisJobDetail> databaseJobDetails = new HashMap<>();
		Scheduler scheduler = null;
		SchedulerHelper sh = getApplicationContext().getBean(SchedulerHelper.class);
		try {
			scheduler = sh.getScheduler();

			// Fill the databaseJobDetails Map with all IbisJobDetails that have been stored in the database
			Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
			for(JobKey jobKey : jobKeys) {
				IbisJobDetail detail = (IbisJobDetail) scheduler.getJobDetail(jobKey);
				if(detail.getJobType() == JobType.DATABASE) {
					databaseJobDetails.put(detail.getKey(), detail);
				}
			}
		} catch (SchedulerException e) {
			getMessageKeeper().add("unable to retrieve jobkeys from scheduler", e);
		}

		// Get all IbisSchedules that have been stored in the database
		FixedQuerySender qs = SpringUtils.createBean(getApplicationContext(), FixedQuerySender.class);
		qs.setDatasourceName(JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
		qs.setQuery("SELECT COUNT(*) FROM IBISSCHEDULES");

		try {
			qs.configure();
			qs.open();
			try (Connection conn = qs.getConnection()) {
				try (PreparedStatement stmt = conn.prepareStatement("SELECT JOBNAME,JOBGROUP,ADAPTER,RECEIVER,CRON,EXECUTIONINTERVAL,MESSAGE,LOCKER,LOCK_KEY FROM IBISSCHEDULES")) {
					try (ResultSet rs = stmt.executeQuery()) {
						IbisManager ibisManager = getIbisManager();
						while(rs.next()) {
							String jobName = rs.getString("JOBNAME");
							String jobGroup = rs.getString("JOBGROUP");
							String adapterName = rs.getString("ADAPTER");
							String javaListener = rs.getString("RECEIVER");
							String cronExpression = rs.getString("CRON");
							int interval = rs.getInt("EXECUTIONINTERVAL");
							String message = rs.getString("MESSAGE");
							boolean hasLocker = rs.getBoolean("LOCKER");
							String lockKey = rs.getString("LOCK_KEY");

							JobKey key = JobKey.jobKey(jobName, jobGroup);

							Adapter adapter = ibisManager.getRegisteredAdapter(adapterName);
							if(adapter == null) {
								getMessageKeeper().add("unable to add schedule ["+key+"], adapter ["+adapterName+"] not found");
								continue;
							}

							//Create a new JobDefinition so we can compare it with existing jobs
							DatabaseJob jobdef = SpringUtils.createBean(adapter.getApplicationContext(), DatabaseJob.class);
							jobdef.setCronExpression(cronExpression);
							jobdef.setName(jobName);
							jobdef.setInterval(interval);
							jobdef.setJobGroup(jobGroup);
							jobdef.setAdapterName(adapterName);
							jobdef.setJavaListener(javaListener);
							jobdef.setMessage(message);
			
							if(hasLocker) {
								Locker locker = SpringUtils.createBean(getApplicationContext(), Locker.class);
			
								locker.setName(lockKey);
								locker.setObjectId(lockKey);
								locker.setDatasourceName(JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
								jobdef.setLocker(locker);
							}
			
							try {
								jobdef.configure();
							} catch (ConfigurationException e) {
								getMessageKeeper().add("unable to configure DatabaseJobDef ["+jobdef+"] with key ["+key+"]", e);
							}
			
							// If the job is found, find out if it is different from the existing one and update if necessarily
							if(databaseJobDetails.containsKey(key)) {
								IbisJobDetail oldJobDetails = databaseJobDetails.get(key);
								if(!oldJobDetails.compareWith(jobdef)) {
									log.debug("updating DatabaseSchedule ["+key+"]");
									try {
										sh.scheduleJob(jobdef);
									} catch (SchedulerException e) {
										getMessageKeeper().add("unable to update schedule ["+key+"]", e);
									}
								}
								// Remove the key that has been found from the databaseJobDetails Map
								databaseJobDetails.remove(key);
							} else {
								// The job was not found in the databaseJobDetails Map, which indicates it's new and has to be added
								log.debug("add DatabaseSchedule ["+key+"]");
								try {
									sh.scheduleJob(jobdef);
								} catch (SchedulerException e) {
									getMessageKeeper().add("unable to add schedule ["+key+"]", e);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) { // Only catch database related exceptions!
			getMessageKeeper().add("unable to retrieve schedules from database", e);
		} finally {
			qs.close();
		}

		// Loop through all remaining databaseJobDetails, which were not present in the database. Since they have been removed, unschedule them!
		for(JobKey key : databaseJobDetails.keySet()) {
			log.debug("delete DatabaseSchedule ["+key+"]");
			try {
				scheduler.deleteJob(key);
			} catch (SchedulerException e) {
				getMessageKeeper().add("unable to remove schedule ["+key+"]", e);
			}
		}
	}
}
