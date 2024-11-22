/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.scheduler.ConfiguredJob;
import org.frankframework.scheduler.IbisJobDetail;
import org.frankframework.scheduler.SchedulerHelper;
import org.frankframework.scheduler.job.DatabaseJob;
import org.frankframework.scheduler.job.IJob;
import org.frankframework.util.Locker;
import org.frankframework.util.MessageKeeperMessage;
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
import org.springframework.messaging.Message;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.SCHEDULER)
public class GetSchedules extends BusEndpointBase {

	private SchedulerHelper getSchedulerHelper() {
		return getBean("schedulerHelper", SchedulerHelper.class);
	}

	private Scheduler getScheduler() {
		return getSchedulerHelper().getScheduler();
	}

	@ActionSelector(BusAction.GET)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getSchedules(Message<?> message) {
		Scheduler scheduler = getScheduler();
		Map<String, Object> returnMap = new HashMap<>();

		try {
			returnMap.put("scheduler", getSchedulerMetaData(scheduler));
			returnMap.put("jobs", getJobGroupNamesWithJobs(scheduler));
		}
		catch(Exception e) {
			throw new BusException("Failed to parse schedules", e);
		}

		return new JsonMessage(returnMap);
	}

	@ActionSelector(BusAction.FIND)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getSchedule(Message<?> message) {
		String jobName = BusMessageUtils.getHeader(message, "job");
		String groupName = BusMessageUtils.getHeader(message, "group");

		Map<String, Object> returnMap = new HashMap<>();
		JobKey jobKey = JobKey.jobKey(jobName, groupName);

		try {
			returnMap = getJobData(jobKey, true);
		} catch (SchedulerException e) {
			throw new BusException("unable to retrieve jobdata", e);
		}

		return new JsonMessage(returnMap);
	}

	private Map<String, Object> getSchedulerMetaData(Scheduler scheduler) {
		Map<String, Object> schedulesMap = new HashMap<>();

		try {
			SchedulerMetaData schedulerMetaData = scheduler.getMetaData();

			schedulesMap.put("name", schedulerMetaData.getSchedulerName());
			schedulesMap.put("instanceId", schedulerMetaData.getSchedulerInstanceId());
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

			schedulesMap.put("runningSince", runningSince(schedulerMetaData));
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

	private long runningSince(SchedulerMetaData schedulerMetaData) {
		try {
			Date runningSince = schedulerMetaData.getRunningSince();
			return runningSince.getTime();
		} catch (Exception e) {
			log.debug("unable to determine running since", e);
		}
		return 0;
	}

	private Map<String,Object> getJobGroupNamesWithJobs(Scheduler scheduler) {
		Map<String, Object> jobGroups = new HashMap<>();
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
		Map<String, Object> jobData = new HashMap<>();
		Scheduler scheduler = getScheduler();
		String jobName = jobKey.getName();
		JobDetail job = scheduler.getJobDetail(jobKey);
		if(job == null) {
			throw new BusException("Job ["+jobKey+"] not found");
		}

		jobData.put("name", job.getKey().getName());
		jobData.put("group", job.getKey().getGroup());
		String description = "-";
		if (StringUtils.isNotEmpty(job.getDescription()))
			description = job.getDescription();
		jobData.put("description", description);
		jobData.put("stateful", job.isPersistJobDataAfterExecution() && job.isConcurrentExecutionDisallowed());
		jobData.put("durable",job.isDurable());
		jobData.put("jobClass", job.getJobClass().getSimpleName());

		if(job instanceof IbisJobDetail detail) {
			jobData.put("type", detail.getJobType());
		}

		TriggerState state = scheduler.getTriggerState(TriggerKey.triggerKey(jobName, jobKey.getGroup()));
		jobData.put("state", state.name());

		jobData.put("triggers", getJobTriggers(scheduler.getTriggersOfJob(jobKey)));
		jobData.put("messages", getJobMessages(job));

		JobDataMap jobMap = job.getJobDataMap();
		jobData.put("properties", getJobData(jobMap));

		if(expanded) {
			IJob jobDef = (IJob) jobMap.get(ConfiguredJob.JOBDEF_KEY);
			if(jobDef instanceof DatabaseJob dbJob) {
				jobData.put("adapter", dbJob.getAdapterName());
				jobData.put("listener", dbJob.getJavaListener());
				jobData.put("message", dbJob.getMessage());
				jobData.put("configuration", dbJob.getApplicationContext().getId());
			}

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

	private List<Map<String, Object>> getJobTriggers(List<? extends Trigger> triggers) {
		List<Map<String, Object>> jobTriggers = new ArrayList<>();

		for (Trigger trigger : triggers) {
			Map<String, Object> triggerDetails = new HashMap<>();

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

			if (trigger instanceof CronTrigger cronTrigger) {
				triggerDetails.put("triggerType", "cron");
				triggerDetails.put("cronExpression", cronTrigger.getCronExpression());
			} else if (trigger instanceof SimpleTrigger simpleTrigger) {
				triggerDetails.put("triggerType", "simple");
				triggerDetails.put("repeatInterval", simpleTrigger.getRepeatInterval());
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
			log.debug("error parsing date for property [{}]", propertyName, e);
		}
	}

	private List<Map<String, Object>> getJobData(JobDataMap jobData) {
		List<Map<String, Object>> jobDataMap = new ArrayList<>();
		String[] keys = jobData.getKeys();

		for (int z = 0; z < keys.length; z++) {
			Map<String, Object> property = new HashMap<>(3);

			String name = keys[z];
			Object value = jobData.get(name);

			if (value != null) {
				property.put("className", value.getClass().getName());
				property.put("value", value.toString());
			}
			property.put("key", name);

			jobDataMap.add(property);
		}

		return jobDataMap;
	}

	private List<Map<String, Object>> getJobMessages(JobDetail jobDetail) {
		List<Map<String, Object>> messages = new ArrayList<>();

		IJob jobdef = (IJob) jobDetail.getJobDataMap().get(ConfiguredJob.JOBDEF_KEY);
		for (int t=0; t < jobdef.getMessageKeeper().size(); t++) {
			Map<String, Object> message = new HashMap<>(3);
			MessageKeeperMessage job = jobdef.getMessageKeeper().getMessage(t);

			message.put("text", job.getMessageText());
			message.put("date", Date.from(job.getMessageDate()));
			message.put("level", job.getMessageLevel());

			messages.add(message);
		}

		return messages;
	}
}
