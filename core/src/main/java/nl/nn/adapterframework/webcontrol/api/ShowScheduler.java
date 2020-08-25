/*
Copyright 2016-2020 WeAreFrank!

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
import javax.ws.rs.Consumes;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.scheduler.ConfiguredJob;
import nl.nn.adapterframework.scheduler.DatabaseJobDef;
import nl.nn.adapterframework.scheduler.IbisJobDetail;
import nl.nn.adapterframework.scheduler.IbisJobDetail.JobType;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.MessageKeeperMessage;

/**
 * Retrieves the Scheduler metadata and the jobgroups with there jobs from the Scheduler.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowScheduler extends Base {
	private @Context SecurityContext securityContext;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedules() throws ApiException {

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

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedule(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName) throws ApiException {
		Map<String, Object> returnMap = new HashMap<String, Object>();
		JobKey jobKey = JobKey.jobKey(jobName, groupName);

		try {
			returnMap = getJobData(jobKey, true);
		} catch (SchedulerException e) {
			throw new ApiException(e);
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
				log.debug("unable to determine running since", e);
			}
			schedulesMap.put("runningSince", runningSinceInLong);
			schedulesMap.put("jobStoreClass", schedulerMetaData.getJobStoreClass().getName());
			schedulesMap.put("schedulerClass", schedulerMetaData.getSchedulerClass().getName());
			schedulesMap.put("threadPoolClass", schedulerMetaData.getThreadPoolClass().getName());
			schedulesMap.put("threadPoolSize", schedulerMetaData.getThreadPoolSize());
		}
		catch (SchedulerException se) {
			log.error("unable to retrieve SchedulerMetaData", se);
		}

		return schedulesMap;
	}

	private Map<String,Object> getJobGroupNamesWithJobs(Scheduler scheduler) throws ApiException {
		Map<String, Object> jobGroups = new HashMap<String, Object>();
		try {
			List<String> jobGroupNames = scheduler.getJobGroupNames();

			for(String jobGroupName : jobGroupNames) {
				List<Map<String, Object>> jobsInGroupList = new ArrayList<>();

				Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroupName));

				for (JobKey jobKey : jobKeys) {
					jobsInGroupList.add(getJobData(jobKey, false));
				}
				jobGroups.put(jobGroupName, jobsInGroupList);
			}
		} catch (Exception e) {
			log.error("error retrieving job from jobgroup", e);
		}

		return jobGroups;
	}

	private Map<String, Object> getJobData(JobKey jobKey, boolean expanded) throws SchedulerException {
		Map<String, Object> jobData = new HashMap<String, Object>();
		Scheduler scheduler = getScheduler();
		String jobName = jobKey.getName();
		JobDetail job = scheduler.getJobDetail(jobKey);

		jobData.put("fullName", job.getKey().getGroup() + "." + job.getKey().getName());
		jobData.put("name", job.getKey().getName());
		jobData.put("group", job.getKey().getGroup());
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

		TriggerState state = scheduler.getTriggerState(TriggerKey.triggerKey(jobName, jobKey.getGroup()));
		jobData.put("state", state.name());

		jobData.put("triggers", getJobTriggers(scheduler.getTriggersOfJob(jobKey)));
		jobData.put("messages", getJobMessages(job));

		JobDataMap jobMap = job.getJobDataMap();
		jobData.put("properties", getJobData(jobMap));

		if(expanded) {
			JobDef jobDef = (JobDef) jobMap.get(ConfiguredJob.JOBDEF_KEY);
			jobData.put("adapter", jobDef.getAdapterName());
			jobData.put("receiver", jobDef.getReceiverName());
			jobData.put("message", jobDef.getMessage());
			
			Locker locker = jobDef.getLocker();
			if(locker != null) {
				jobData.put("locker", true);
				jobData.put("lockkey", locker.getObjectId());
			} else {
				jobData.put("locker", false);
			}
		}

		return jobData;
	}

	private List<Map<String, Object>> getJobTriggers(List<? extends Trigger> triggers) throws ApiException {
		List<Map<String, Object>> jobTriggers = new ArrayList<Map<String, Object>>();

		for (Trigger trigger : triggers) {
			Map<String, Object> triggerDetails = new HashMap<String, Object>();

			TriggerKey triggerKey = trigger.getKey();
			triggerDetails.put("fullName", triggerKey.getGroup() + "." + triggerKey.getName());
			triggerDetails.put("name", triggerKey.getName());
			triggerDetails.put("calendarName", trigger.getCalendarName());

			putDateProperty(triggerDetails, "endTime", trigger.getEndTime());
			putDateProperty(triggerDetails, "finalFireTime", trigger.getFinalFireTime());
			putDateProperty(triggerDetails, "nextFireTime", trigger.getNextFireTime());
			putDateProperty(triggerDetails, "previousFireTime", trigger.getPreviousFireTime());
			putDateProperty(triggerDetails, "startTime", trigger.getStartTime());

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

	private void putDateProperty(Map<String, Object> map, String propertyName, Date date) {
		try {
			if(date != null) {
				map.put(propertyName, date.getTime());
			}
		} catch (Exception e) {
			log.debug("error parsing date for property ["+propertyName+"]", e);
		}
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
			log.error("unable to run action ["+action+"]",e);
		}
		return Response.status(Response.Status.OK).build();
	}

	private Scheduler getScheduler() {
		DefaultIbisManager manager = (DefaultIbisManager) getIbisManager();
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
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response trigger(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName, LinkedHashMap<String, Object> json) throws ApiException {
		Scheduler scheduler = getScheduler();

		if(log.isInfoEnabled()) {
			String commandIssuedBy = request.getRemoteHost();
			commandIssuedBy += "-"+request.getRemoteAddr();
			commandIssuedBy += "-"+request.getRemoteUser();
	
			log.info("trigger job jobName [" + jobName + "] groupName [" + groupName + "] " + commandIssuedBy);
		}
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
	public Response createSchedule(MultipartBody input) throws ApiException {
		String jobGroupName = resolveStringFromMap(input, "group");
		return createSchedule(jobGroupName, input);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job/{jobName}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateSchedule(@PathParam("groupName") String groupName, @PathParam("jobName") String jobName, MultipartBody input) throws ApiException {
		return createSchedule(groupName, jobName, input, true);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createScheduleInJobGroup(@PathParam("groupName") String groupName, MultipartBody input) throws ApiException {
		return createSchedule(groupName, input);
	}


	private Response createSchedule(String groupName, MultipartBody input) {
		return createSchedule(groupName, null, input, false);
	}

	private Response createSchedule(String groupName, String jobName, MultipartBody inputDataMap, boolean overwrite) {

		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		String name = jobName;
		if(name == null) //If name is not explicitly set, try to deduct it from inputmap
			name = resolveStringFromMap(inputDataMap, "name");

		String cronExpression = resolveTypeFromMap(inputDataMap, "cron", String.class, "");
		int interval = resolveTypeFromMap(inputDataMap, "interval", Integer.class, -1);
		//Either one of the two has to be set
		if(interval == -1 && StringUtils.isEmpty(cronExpression)) {
			throw new ApiException("Either 'cron' or 'interval' has to be set");
		}

		String adapterName = resolveStringFromMap(inputDataMap, "adapter");
		//Make sure the adapter exists!
		DefaultIbisManager manager = (DefaultIbisManager) getIbisManager();
		IAdapter adapter = manager.getRegisteredAdapter(adapterName);
		if(adapter == null) {
			throw new ApiException("Adapter ["+adapterName+"] not found");
		}

		//Make sure the receiver exists!
		String receiverName = resolveStringFromMap(inputDataMap, "receiver");
		IReceiver receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found");
		}
		String listenerName = null;
		if (receiver instanceof ReceiverBase) {
			ReceiverBase rb = (ReceiverBase) receiver;
			IListener<?> listener = rb.getListener();
			if(listener != null) {
				listenerName = listener.getName();
			}
		}
		if(StringUtils.isEmpty(listenerName)) {
			throw new ApiException("unable to determine listener for receiver ["+receiverName+"]");
		}

		String jobGroup = groupName;
		if(StringUtils.isEmpty(jobGroup)) {
			jobGroup = adapter.getConfiguration().getName();
		}

		boolean persistent = resolveTypeFromMap(inputDataMap, "persistent", boolean.class, false);
		boolean hasLocker = resolveTypeFromMap(inputDataMap, "locker", boolean.class, false);
		String lockKey = resolveTypeFromMap(inputDataMap, "lockkey", String.class, "lock4["+name+"]");

		String message = resolveStringFromMap(inputDataMap, "message");

		String description = resolveStringFromMap(inputDataMap, "description");
		
		SchedulerHelper sh = manager.getSchedulerHelper();

		//First try to create the schedule and run it on the local ibis before storing it in the database
		DatabaseJobDef jobdef = new DatabaseJobDef();
		jobdef.setCronExpression(cronExpression);
		jobdef.setName(name);
		jobdef.setAdapterName(adapterName);
		jobdef.setReceiverName(listenerName);
		jobdef.setJobGroup(jobGroup);
		jobdef.setMessage(message);
		jobdef.setDescription(description);
		jobdef.setInterval(interval);

		String jmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
		if(hasLocker) {
			Locker locker = (Locker) getIbisContext().createBeanAutowireByName(Locker.class);
			locker.setName(lockKey);
			locker.setObjectId(lockKey);
			locker.setJmsRealm(jmsRealm);
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
			if (StringUtils.isEmpty(jmsRealm)) {
				throw new ApiException("no JmsRealm found!");
			}

			boolean success = false;
			FixedQuerySender qs = (FixedQuerySender) getIbisContext().createBeanAutowireByName(FixedQuerySender.class);
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

			try {
				qs.open();
				try (Connection conn = qs.getConnection()) {

					if(overwrite) {
						String deleteQuery = "DELETE FROM IBISSCHEDULES WHERE JOBNAME=? AND JOBGROUP=?";
						try (PreparedStatement deleteStatement = conn.prepareStatement(deleteQuery)) {
							deleteStatement.setString(1, name);
							deleteStatement.setString(2, jobGroup);
							deleteStatement.executeUpdate();
						}
					}
	
					String insertQuery = "INSERT INTO IBISSCHEDULES (JOBNAME, JOBGROUP, ADAPTER, RECEIVER, CRON, EXECUTIONINTERVAL, MESSAGE, DESCRIPTION, LOCKER, LOCK_KEY, CREATED_ON, BY_USER) "
							+ "VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?)";
					try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
						stmt.setString(1, name);
						stmt.setString(2, jobGroup);
						stmt.setString(3, adapterName);
						stmt.setString(4, listenerName);
						stmt.setString(5, cronExpression);
						stmt.setInt(6, interval);
						stmt.setClob(7, new StringReader(message));
						stmt.setString(8, description);
						stmt.setBoolean(9, hasLocker);
						stmt.setString(10, lockKey);
						stmt.setString(11, user);
		
						success = stmt.executeUpdate() > 0;
					}
				}
			} catch (SenderException | SQLException | JdbcException e) {
				throw new ApiException("error saving job in database", e);
			} finally {
				qs.close();
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
		Scheduler scheduler = getScheduler();

		try {
			if(log.isInfoEnabled()) log.info("delete job jobName [" + jobName + "] groupName [" + groupName + "] " + commandIssuedBy());
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
				FixedQuerySender qs = (FixedQuerySender) getIbisContext().createBeanAutowireByName(FixedQuerySender.class);
				qs.setJmsRealm(jmsRealm);
				qs.setQuery("SELECT COUNT(*) FROM IBISSCHEDULES");
				try {
					qs.configure();
				} catch (ConfigurationException e) {
					throw new ApiException("Error creating FixedQuerySender bean to remove job from database", e);
				}

				try {
					qs.open();
					try (Connection conn = qs.getConnection()) {

						String query = "DELETE FROM IBISSCHEDULES WHERE JOBNAME=? AND JOBGROUP=?";
						try (PreparedStatement stmt = conn.prepareStatement(query)) {
							stmt.setString(1, jobKey.getName());
							stmt.setString(2, jobKey.getGroup());
		
							success = stmt.executeUpdate() > 0;
						}
					}
				} catch (SenderException | SQLException | JdbcException e) {
					throw new ApiException("error removing job from database", e);
				} finally {
					qs.close();
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
