/*
Copyright 2016 Integration Partners B.V.

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
package nl.nn.adapterframework.webcontrol.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;
import nl.nn.adapterframework.util.MessageKeeperMessage;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
* Retrieves the Scheduler metadata and the jobgroups with there jobs from the Scheduler.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowScheduler extends Base {
	@Context ServletConfig servletConfig;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedules() throws ApiException {
		initBase(servletConfig);

		Map<String, Object> returnMap = new HashMap<String, Object>();
		
		DefaultIbisManager manager = (DefaultIbisManager)ibisManager;
		SchedulerHelper sh = manager.getSchedulerHelper();
		Scheduler scheduler;

		try {
			scheduler = sh.getScheduler();
		}
		catch(Exception e) {
			throw new ApiException("Failed to parse destinations!");
		}
		
		try {
			returnMap.put("scheduler", getSchedulerMetaData(scheduler));
			returnMap.put("jobs", getJobGroupNamesWithJobs(scheduler));
		}
		catch(Exception e) {
			throw new ApiException("Failed to parse destinations!");
		}

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}
	
	private Map<String, Object> getSchedulerMetaData(Scheduler scheduler) throws ApiException {
		Map<String, Object> schedulesMap = new HashMap<String, Object>();

		try {
			SchedulerMetaData schedulerMetaData = scheduler.getMetaData();

			schedulesMap.put("name", schedulerMetaData.getSchedulerName());
			schedulesMap.put("instanceId", schedulerMetaData.getSchedulerInstanceId().toString());
			schedulesMap.put("version", schedulerMetaData.getVersion());
			schedulesMap.put("isSchedulerRemote", schedulerMetaData.isSchedulerRemote());

			String state = "unknown";
			if(schedulerMetaData.isStarted())
				state = "started";
			if(schedulerMetaData.isInStandbyMode())
				state = "paused";
			if(schedulerMetaData.isShutdown())
				state = "stopped";

			schedulesMap.put("state", state);
			schedulesMap.put("shutdown", schedulerMetaData.isShutdown());
			schedulesMap.put("started", schedulerMetaData.isStarted());
			schedulesMap.put("paused", schedulerMetaData.isInStandbyMode());

			schedulesMap.put("jobStoreSupportsPersistence", schedulerMetaData.jobStoreSupportsPersistence());
			schedulesMap.put("jobsExecuted", schedulerMetaData.getNumberOfJobsExecuted());
			long runningSinceInLong = 0;
			try {
				Date runningSince = schedulerMetaData.getRunningSince();
				runningSinceInLong = runningSince.getTime();
			} catch (Exception e) {
				log.debug(e);
			}
			schedulesMap.put("runningSince", runningSinceInLong);
			schedulesMap.put("jobStoreClass", schedulerMetaData.getJobStoreClass().getName());
			schedulesMap.put("schedulerClass", schedulerMetaData.getSchedulerClass().getName());
			schedulesMap.put("threadPoolClass", schedulerMetaData.getThreadPoolClass().getName());
			schedulesMap.put("threadPoolSize", schedulerMetaData.getThreadPoolSize());
		}
		catch (SchedulerException se) {
			log.error(se);
		}

		return schedulesMap;
	}

	private Map<String,Object> getJobGroupNamesWithJobs(Scheduler scheduler) throws ApiException {
		Map<String, Object> jobGroups = new HashMap<String, Object>();
		try {
			String[] jobGroupNames = scheduler.getJobGroupNames();

			for (int i = 0; i < jobGroupNames.length; i++) {
				Map<String, Object> jobsInGroup = new HashMap<String, Object>();

				String jobGroupName = jobGroupNames[i];
				String[] jobNames = scheduler.getJobNames(jobGroupName);

				for (int j = 0; j < jobNames.length; j++) {
					String jobName = jobNames[j];
					Map<String, Object> jobData = new HashMap<String, Object>();

					JobDef jobDef = null;
					for (Configuration configuration : ibisManager.getConfigurations()) {
						jobDef = configuration.getScheduledJob(jobName);
						if (jobDef != null) {
							break;
						}
					}

					JobDetail job = scheduler.getJobDetail(jobName, jobGroupName);

					jobData.put("fullName", job.getFullName());
					jobData.put("name", job.getName());
					String description = "-";
					if (StringUtils.isNotEmpty(job.getDescription()))
						description = job.getDescription();
					jobData.put("description", description);
					jobData.put("stateful", job.isStateful());
					jobData.put("durable",job.isDurable());
					jobData.put("volatile", job.isVolatile());
					jobData.put("jobClass", job.getJobClass().getName());

					jobData.put("triggers", getJobTriggers(scheduler, jobName, jobGroupName));
					jobData.put("messages", getJobMessages(jobDef));

					JobDataMap jobMap = job.getJobDataMap();
					jobData.put("containsTransientData", jobMap.containsTransientData());
					jobData.put("allowsTransientData", jobMap.getAllowsTransientData());
					jobData.put("properties", getJobData(jobMap));

					jobsInGroup.put(jobName, jobData);
				}
				jobGroups.put(jobGroupName, jobsInGroup);
			}
		} catch (Exception e){
			log.error(e);
		}

		return jobGroups;
	}

	private List<Map<String, Object>> getJobTriggers(Scheduler scheduler, String jobName, String groupName) throws ApiException {
		List<Map<String, Object>> jobTriggers = new ArrayList<Map<String, Object>>();

		try {
			String[] triggerGroupNames = scheduler.getTriggerGroupNames();
			for (int i = 0; i < triggerGroupNames.length; i++) {
				String triggerGroupName = triggerGroupNames[i];
				String[] triggerNames = scheduler.getTriggerNames(triggerGroupName);

				for (int s = 0; s < triggerNames.length; s++) {
					Trigger trigger = scheduler.getTrigger(triggerNames[s], triggerGroupName);
					if ((trigger.getJobName().equals(jobName)) && (trigger.getJobGroup().equals(groupName))) {
						Map<String, Object> triggerDetails = new HashMap<String, Object>();

						triggerDetails.put("fullName", trigger.getFullName());
						triggerDetails.put("name", trigger.getName());
						triggerDetails.put("calendarName", trigger.getCalendarName());
						Date date;
						try {
							date = trigger.getEndTime();
							triggerDetails.put("endTime", date.getTime());
						} catch (Exception e) { log.debug(e); };
						try {
							date = trigger.getFinalFireTime();
							triggerDetails.put("finalFireTime", date.getTime());
						} catch (Exception e) { log.debug(e); };
						try {
							date = trigger.getNextFireTime();
							triggerDetails.put("nextFireTime", date.getTime());
						} catch (Exception e) { log.debug(e); };
						try {
							date = trigger.getStartTime();
							triggerDetails.put("startTime", date.getTime());
						} catch (Exception e) { log.debug(e); };
						triggerDetails.put("misfireInstruction", trigger.getMisfireInstruction());

						if (trigger instanceof CronTrigger) {
							triggerDetails.put("triggerType", "cron");
							triggerDetails.put("cronExpression", ((CronTrigger) trigger).getCronExpression());
						} else if (trigger instanceof SimpleTrigger) {
							triggerDetails.put("triggerType", "simple");
							triggerDetails.put("repeatInterval", ((SimpleTrigger) trigger).getRepeatInterval());
						} else {
							triggerDetails.put("triggerType", "unknown");
						}

						triggerDetails.put("isVolatile", trigger.isVolatile());

						jobTriggers.add(triggerDetails);
					}
				}
			}
		}
		catch(Exception e) {
			throw new ApiException("Failed to get JobTriggers!");
		}
		return jobTriggers;
	}

	private List<Map<String, Object>> getJobData(JobDataMap jobData) throws ApiException {
		List<Map<String, Object>> jobDataMap = new ArrayList<Map<String, Object>>();
		String[] keys = jobData.getKeys();

		for (int z = 0; z < keys.length; z++) {
			Map<String, Object> property = new HashMap<String, Object>(3);

			String name = keys[z];
			Object value = jobData.get(name);

			if (value != null) {
				property.put("className", value.getClass().getName());
			}
			property.put("key", name);
			property.put("value", value.toString());

			jobDataMap.add(property);
		}

		return jobDataMap;
	}

	private List<Map<String, Object>> getJobMessages(JobDef jobdef) throws ApiException {
		List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();

		for (int t=0; t < jobdef.getMessageKeeper().size(); t++) {
			Map<String, Object> message = new HashMap<String, Object>(3);
			MessageKeeperMessage job = jobdef.getMessageKeeper().getMessage(t);

			message.put("text", job.getMessageText());
			message.put("date", job.getMessageDate());
			message.put("level", job.getMessageLevel());

			messages.add(message);
		}

		return messages;
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response putSchedules(LinkedHashMap<String, Object> json) throws ApiException {
		initBase(servletConfig);

		DefaultIbisManager manager = (DefaultIbisManager) ibisManager;
		SchedulerHelper sh = manager.getSchedulerHelper();

		Scheduler scheduler;
		try {
			scheduler = sh.getScheduler();
		}
		catch (SchedulerException e) {
			throw new ApiException("Cannot find scheduler"); 
		}

		String action = null;
		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("action")) {
				action = entry.getValue().toString();
			}
		}

		try {
			String commandIssuedBy = servletConfig.getInitParameter("remoteHost");
			commandIssuedBy += servletConfig.getInitParameter("remoteAddress");
			commandIssuedBy += servletConfig.getInitParameter("remoteUser");

			
			if (action.equalsIgnoreCase("start")) {
				if(scheduler.isInStandbyMode() || scheduler.isShutdown()) {
					scheduler.start();
					log.info("start scheduler:" + new Date() + commandIssuedBy);
				}
				else {
					throw new ApiException("Failed to start scheduler");
				}
			}
			else if (action.equalsIgnoreCase("pause")) {
				if(scheduler.isStarted()) {
					scheduler.standby();
					log.info("pause scheduler:" + new Date() + commandIssuedBy);
				}
				else {
					throw new ApiException("Failed to pause scheduler");
				}
			}
			else if (action.equalsIgnoreCase("stop")) {
				if(scheduler.isStarted() || scheduler.isInStandbyMode()) {
					scheduler.shutdown();
					log.info("shutdown scheduler:" + new Date() + commandIssuedBy);
				}
				else {
					throw new ApiException("Failed to stop scheduler");
				}
			} else {
				return Response.status(Response.Status.BAD_REQUEST).build();
			}
	
		} catch (Exception e) {
			log.error("",e);
		}
		return Response.status(Response.Status.OK).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response trigger(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName) throws ApiException {
		initBase(servletConfig);

		DefaultIbisManager manager = (DefaultIbisManager) ibisManager;
		SchedulerHelper sh = manager.getSchedulerHelper();

		Scheduler scheduler;
		try {
			scheduler = sh.getScheduler();
		}
		catch (SchedulerException e) {
			throw new ApiException("Cannot find scheduler"); 
		}

		try {
			String commandIssuedBy = servletConfig.getInitParameter("remoteHost");
			commandIssuedBy += servletConfig.getInitParameter("remoteAddress");
			commandIssuedBy += servletConfig.getInitParameter("remoteUser");

			log.info("trigger job jobName [" + jobName + "] groupName [" + groupName + "] " + commandIssuedBy);
			scheduler.triggerJob(jobName, groupName);

		} catch (Exception e) {
			throw new ApiException("Failed to trigger job"); 
		}

		return Response.status(Response.Status.OK).build();
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response PutSchedules(@PathParam("jobName") String jobName, @QueryParam("groupName") String groupName) throws ApiException {
		initBase(servletConfig);

		DefaultIbisManager manager = (DefaultIbisManager)ibisManager;
		SchedulerHelper sh = manager.getSchedulerHelper();

		Scheduler scheduler;
		try {
			scheduler = sh.getScheduler();
		}
		catch (SchedulerException e) {
			throw new ApiException("Cannot find scheduler"); 
		}

		try	{
			String commandIssuedBy = servletConfig.getInitParameter("remoteHost");
			commandIssuedBy += servletConfig.getInitParameter("remoteAddress");
			commandIssuedBy += servletConfig.getInitParameter("remoteUser");

			log.info("delete job jobName [" + jobName + "] groupName [" + groupName + "] " + commandIssuedBy);
			scheduler.deleteJob(jobName, groupName);
		} catch (Exception e) {
			throw new ApiException("Failed to delete job"); 
		}
		return Response.status(Response.Status.OK).build();
	}
}
