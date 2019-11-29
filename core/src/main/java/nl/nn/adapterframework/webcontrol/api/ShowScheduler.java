/*
Copyright 2016-2017, 2019 Integration Partners B.V.

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

import java.io.StringReader;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.scheduler.ConfiguredJob;
import nl.nn.adapterframework.scheduler.DatabaseJobDef;
import nl.nn.adapterframework.scheduler.IbisJobDetail;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.scheduler.IbisJobDetail.JobType;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.MessageKeeperMessage;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * Retrieves the Scheduler metadata and the jobgroups with there jobs from the Scheduler.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowScheduler extends Base {
	private @Context ServletConfig servletConfig;
	private @Context SecurityContext securityContext;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedules() throws ApiException {
		initBase(servletConfig);
		Scheduler scheduler = getScheduler();

		Map<String, Object> returnMap = new HashMap<String, Object>();

		try {
			returnMap.put("scheduler", getSchedulerMetaData(scheduler));
			returnMap.put("jobs", getJobGroupNamesWithJobs(scheduler));
		}
		catch(Exception e) {
			throw new ApiException("Failed to parse destinations", e);
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

			schedulesMap.put("jobStoreSupportsPersistence", schedulerMetaData.isJobStoreSupportsPersistence());
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
			List<String> jobGroupNames = scheduler.getJobGroupNames();

			for(String jobGroupName : jobGroupNames) {
				Map<String, Object> jobsInGroup = new HashMap<String, Object>();

				Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroupName));

				for (JobKey jobKey : jobKeys) {
					String jobName = jobKey.getName();
					Map<String, Object> jobData = new HashMap<String, Object>();

					JobDetail job = scheduler.getJobDetail(jobKey);

					jobData.put("fullName", job.getKey().getName() + "." + job.getKey().getGroup());
					jobData.put("name", job.getKey().getName());
					String description = "-";
					if (StringUtils.isNotEmpty(job.getDescription()))
						description = job.getDescription();
					jobData.put("description", description);
					jobData.put("stateful", job.isPersistJobDataAfterExecution() && job.isConcurrentExectionDisallowed());
					jobData.put("durable",job.isDurable());
					jobData.put("jobClass", job.getJobClass().getSimpleName());

					if(job instanceof IbisJobDetail) {
						jobData.put("type", ((IbisJobDetail) job).getJobType());
					}

					TriggerState state = scheduler.getTriggerState(TriggerKey.triggerKey(jobName, jobGroupName));
					jobData.put("state", state.name());

					jobData.put("triggers", getJobTriggers(scheduler.getTriggersOfJob(jobKey)));
					jobData.put("messages", getJobMessages(job));

					JobDataMap jobMap = job.getJobDataMap();
					jobData.put("properties", getJobData(jobMap));

					jobsInGroup.put(jobName, jobData);
				}
				jobGroups.put(jobGroupName, jobsInGroup);
			}
		} catch (Exception e) {
			log.error("error retrieving job from jobgroup", e);
		}

		return jobGroups;
	}

	private List<Map<String, Object>> getJobTriggers(List<? extends Trigger> triggers) throws ApiException {
		List<Map<String, Object>> jobTriggers = new ArrayList<Map<String, Object>>();

		for (Trigger trigger : triggers) {
			Map<String, Object> triggerDetails = new HashMap<String, Object>();

			TriggerKey triggerKey = trigger.getKey();
			triggerDetails.put("fullName", triggerKey.getGroup() + "." + triggerKey.getName());
			triggerDetails.put("name", triggerKey.getName());
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
				date = trigger.getPreviousFireTime();
				triggerDetails.put("previousFireTime", date.getTime());
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

			jobTriggers.add(triggerDetails);
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

	private List<Map<String, Object>> getJobMessages(JobDetail jobDetail) throws ApiException {
		List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();

		JobDef jobdef = (JobDef) jobDetail.getJobDataMap().get(ConfiguredJob.JOBDEF_KEY);
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
	public Response updateScheduler(LinkedHashMap<String, Object> json) throws ApiException {
		initBase(servletConfig);
		Scheduler scheduler = getScheduler();

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

	private Scheduler getScheduler() {
		DefaultIbisManager manager = (DefaultIbisManager) ibisManager;
		SchedulerHelper sh = manager.getSchedulerHelper();

		try {
			return sh.getScheduler();
		}
		catch (SchedulerException e) {
			throw new ApiException("Cannot find scheduler", e); 
		}
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response trigger(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName, LinkedHashMap<String, Object> json) throws ApiException {
		initBase(servletConfig);
		Scheduler scheduler = getScheduler();

		String commandIssuedBy = servletConfig.getInitParameter("remoteHost");
		commandIssuedBy += servletConfig.getInitParameter("remoteAddress");
		commandIssuedBy += servletConfig.getInitParameter("remoteUser");

		log.info("trigger job jobName [" + jobName + "] groupName [" + groupName + "] " + commandIssuedBy);
		JobKey jobKey = JobKey.jobKey(jobName, groupName);

		String action = ""; //PAUSE,RESUME,TRIGGER

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				action = (String) entry.getValue();
			}
		}

		try {
			if("pause".equals(action)) {
				scheduler.pauseJob(jobKey);
			}
			else if("resume".equals(action)) {
				scheduler.resumeJob(jobKey);
			}
			else if("trigger".equals(action)) {
				scheduler.triggerJob(jobKey);
			}
			else {
				throw new ApiException("no (valid) action provided! Expected one of PAUSE,RESUME,TRIGGER");
			}
		} catch (SchedulerException e) {
			throw new ApiException("Failed to "+action+" job", e); 
		}

		return Response.status(Response.Status.OK).build();
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createSchedule(MultipartFormDataInput input) throws ApiException {
		return createSchedule(null, input);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createScheduleInJobGroup(@PathParam("groupName") String groupName, MultipartFormDataInput input) throws ApiException {
		return createSchedule(groupName, input);
	}

	public Response createSchedule(String groupName, MultipartFormDataInput input) {
		initBase(servletConfig);

		Map<String, List<InputPart>> inputDataMap = input.getFormDataMap();
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		String name = resolveStringFromMap(inputDataMap, "name");
		String cronExpression = resolveStringFromMap(inputDataMap, "cron");
		int interval = resolveTypeFromMap(inputDataMap, "interval", Integer.class, -1);

		String adapterName = resolveStringFromMap(inputDataMap, "adapter");
		//Make sure the adapter exists!
		DefaultIbisManager manager = (DefaultIbisManager) ibisManager;
		IAdapter adapter = manager.getRegisteredAdapter(adapterName);
		if(adapter == null) {
			throw new ApiException("Adapter ["+adapterName+"] not found");
		}

		//Make sure the receiver exists!
		String receiverName = resolveStringFromMap(inputDataMap, "receiver");
		if(adapter.getReceiverByName(receiverName) == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found");
		}

		String jobGroup = groupName;
		if(StringUtils.isEmpty(jobGroup)) {
			jobGroup = adapter.getConfiguration().getName();
		}

		boolean persistent = resolveTypeFromMap(inputDataMap, "persistent", boolean.class, false);
		boolean hasLocker = resolveTypeFromMap(inputDataMap, "locker", boolean.class, false);
		String lockKey = resolveTypeFromMap(inputDataMap, "lockkey", String.class, "lock4["+name+"]");

		String message = resolveStringFromMap(inputDataMap, "message");

		SchedulerHelper sh = manager.getSchedulerHelper();

		//First try to create the schedule and run it on the local ibis before storing it in the database
		DatabaseJobDef jobdef = new DatabaseJobDef();
		jobdef.setCronExpression(cronExpression);
		jobdef.setName(name);
		jobdef.setAdapterName(adapterName);
		jobdef.setReceiverName(receiverName);
		jobdef.setJobGroup(jobGroup);
		jobdef.setMessage(message);
		jobdef.setInterval(interval);

		if(hasLocker) {
			Locker locker = new Locker();
			locker.setName(lockKey);
			locker.setObjectId(lockKey);
			jobdef.setLocker(locker);
		}

		try {
			jobdef.configure();
			sh.scheduleJob(manager, jobdef);
		} catch (Exception e) {
			throw new ApiException("Failed to add schedule", e);
		}

		//Save the job in the database
		if(persistent && AppConstants.getInstance().getBoolean("loadDatabaseSchedules.active", false)) {
			String jmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(jmsRealm)) {
				throw new ApiException("no JmsRealm found!");
			}

			boolean success = false;
			FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
			qs.setJmsRealm(jmsRealm);
			qs.setQuery("SELECT COUNT(*) FROM IBISSCHEDULES");
			try {
				qs.configure();
			} catch (ConfigurationException e) {
				throw new ApiException("Error creating FixedQuerySender bean to store job in database", e);
			}

			String user = null;
			Principal principal = securityContext.getUserPrincipal();
			if(principal != null)
				user = principal.getName();

			Connection conn = null;
			try {
				//TODO: Remove old schedule if exists...

				qs.open();
				conn = qs.getConnection();

				String query = "INSERT INTO IBISSCHEDULES (JOBNAME,JOBGROUP,ADAPTER,RECEIVER,CRON,MESSAGE,LOCKER,LOCK_KEY,CREATED_ON,BY_USER) "
						+ "VALUES (?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?)";
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				stmt.setString(2, jobGroup);
				stmt.setString(3, adapterName);
				stmt.setString(4, receiverName);
				stmt.setString(5, cronExpression);
				stmt.setClob(6, new StringReader(message));
				stmt.setBoolean(7, hasLocker);
				stmt.setString(8, lockKey);
				stmt.setString(9, user);

				success = stmt.executeUpdate() > 0;
			} catch (SenderException | SQLException | JdbcException e) {
				throw new ApiException("error saving job in database", e);
			} finally {
				qs.close();
				JdbcUtil.close(conn);
			}

			if(!success)
				throw new ApiException("An error occurred while storing the job in the database");
		}

		return Response.status(Response.Status.CREATED).build();
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteSchedules(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName) throws ApiException {
		initBase(servletConfig);
		Scheduler scheduler = getScheduler();

		try {
			log.info("delete job jobName [" + jobName + "] groupName [" + groupName + "] " + commandIssuedBy());
			JobKey jobKey = JobKey.jobKey(jobName, groupName);
			if(jobKey == null) {
				throw new ApiException("JobKey not found, unable to remove schedule");
			}

			IbisJobDetail jobDetail = (IbisJobDetail) scheduler.getJobDetail(jobKey);
			if(jobDetail.getJobType() == JobType.DATABASE) {
				boolean success = false;
				String jmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
				if (StringUtils.isEmpty(jmsRealm)) {
					throw new ApiException("no JmsRealm found!");
				}

				// remove from database
				FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
				qs.setJmsRealm(jmsRealm);
				qs.setQuery("SELECT COUNT(*) FROM IBISSCHEDULES");
				try {
					qs.configure();
				} catch (ConfigurationException e) {
					throw new ApiException("Error creating FixedQuerySender bean to remove job from database", e);
				}

				Connection conn = null;
				try {
					qs.open();
					conn = qs.getConnection();

					String query = "DELETE FROM IBISSCHEDULES WHERE JOBNAME=? AND JOBGROUP=?";
					PreparedStatement stmt = conn.prepareStatement(query);
					stmt.setString(1, jobKey.getName());
					stmt.setString(2, jobKey.getGroup());

					success = stmt.executeUpdate() > 0;
				} catch (SenderException | SQLException | JdbcException e) {
					throw new ApiException("error removing job from database", e);
				} finally {
					qs.close();
					JdbcUtil.close(conn);
				}
				if(!success) {
					throw new ApiException("failed to remove job from database");
				}
			}

			// remove from memory
			scheduler.deleteJob(jobKey);
		} catch (SchedulerException e) {
			throw new ApiException("Failed to delete job", e); 
		}
		return Response.status(Response.Status.OK).build();
	}

	private String commandIssuedBy() {
		String commandIssuedBy = servletConfig.getInitParameter("remoteHost");
		commandIssuedBy += servletConfig.getInitParameter("remoteAddress");
		commandIssuedBy += servletConfig.getInitParameter("remoteUser");
		return commandIssuedBy;
	}
}
