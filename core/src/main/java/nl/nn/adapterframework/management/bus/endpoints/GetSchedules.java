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
package nl.nn.adapterframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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

import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.scheduler.ConfiguredJob;
import nl.nn.adapterframework.scheduler.IbisJobDetail;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.scheduler.job.DatabaseJob;
import nl.nn.adapterframework.scheduler.job.IJob;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.MessageKeeperMessage;

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

		return ResponseMessage.ok(returnMap);
	}

	@ActionSelector(BusAction.FIND)
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

		return ResponseMessage.ok(returnMap);
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
			IJob jobDef = (IJob) jobMap.get(ConfiguredJob.JOBDEF_KEY);
			if(jobDef instanceof DatabaseJob) {
				DatabaseJob dbJob = (DatabaseJob) jobDef;
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
			message.put("date", job.getMessageDate());
			message.put("level", job.getMessageLevel());

			messages.add(message);
		}

		return messages;
	}
}
