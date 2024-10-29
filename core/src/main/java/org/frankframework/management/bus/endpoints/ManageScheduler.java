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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.messaging.Message;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.scheduler.IbisJobDetail;
import org.frankframework.scheduler.IbisJobDetail.JobType;
import org.frankframework.scheduler.SchedulerHelper;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.SCHEDULER)
public class ManageScheduler extends BusEndpointBase {
	public enum ScheduleAction {
		START, STOP, PAUSE;
	}
	public enum JobAction {
		PAUSE, RESUME, TRIGGER;
	}

	private SchedulerHelper getSchedulerHelper() {
		return getBean("schedulerHelper", SchedulerHelper.class);
	}

	private Scheduler getScheduler() {
		return getSchedulerHelper().getScheduler();
	}

	private JobKey getJobKey(String jobName, String groupName) {
		JobKey jobKey = JobKey.jobKey(jobName, groupName);
		if(jobKey == null) {
			throw new BusException("JobKey not found, unable to remove schedule");
		}
		return jobKey;
	}

	@ActionSelector(BusAction.MANAGE)
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> manageScheduler(Message<?> message) {
		String issuedBy = BusMessageUtils.getHeader(message, "issuedBy");
		String jobName = BusMessageUtils.getHeader(message, "job");
		String groupName = BusMessageUtils.getHeader(message, "group");

		if(StringUtils.isNotEmpty(jobName) || StringUtils.isNotEmpty(groupName)) {
			JobAction action = BusMessageUtils.getEnumHeader(message, "operation", JobAction.class);
			if(action == null) {
				throw new BusException("no action specified");
			}
			JobKey jobKey = getJobKey(jobName, groupName);
			return handleJob(action, jobKey, issuedBy);
		}

		ScheduleAction action = BusMessageUtils.getEnumHeader(message, "operation", ScheduleAction.class);
		if(action == null) {
			throw new BusException("no action specified");
		}

		return handleScheduler(action, issuedBy);
	}

	public Message<String> handleScheduler(ScheduleAction action, String issuedBy) {
		Scheduler scheduler = getScheduler();
		try {
			switch (action) {
			case START:
				if(scheduler.isInStandbyMode() || scheduler.isShutdown()) {
					scheduler.start();
					log2SecurityLog("starting scheduler", issuedBy);
					break;
				}
				throw new BusException("scheduler already started");
			case PAUSE:
				if(scheduler.isStarted()) {
					scheduler.standby();
					log2SecurityLog("pausing scheduler", issuedBy);
					break;
				}
				throw new BusException("can only pause a started scheduler");
			case STOP:
				if(scheduler.isStarted() || scheduler.isInStandbyMode()) {
					scheduler.shutdown();
					log2SecurityLog("stopping scheduler", issuedBy);
					break;
				}
				throw new BusException("scheduler already stopped");
			default:
				throw new BusException("action not implemented");
			}
		} catch (SchedulerException e) {
			throw new BusException("unable to run action ["+action+"]", e);
		}

		return EmptyMessage.accepted();
	}

	public Message<String> handleJob(JobAction action, JobKey jobKey, String issuedBy) {
		Scheduler scheduler = getScheduler();
		try {
			switch (action) {
			case PAUSE:
				log2SecurityLog("pausing job ["+jobKey+"]", issuedBy);
				scheduler.pauseJob(jobKey);
				break;
			case RESUME:
				log2SecurityLog("resuming job ["+jobKey+"]", issuedBy);
				SchedulerHelper sh = getSchedulerHelper();
				JobDetail jobDetail = getJobDetail(scheduler, jobKey);
				// TODO this part can be more generic in case multiple triggers can be configurable
				List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
				if(triggers != null) {
					for (Trigger trigger : triggers) {
						if(trigger instanceof CronTrigger cronTrigger) {
							sh.scheduleJob(jobDetail, cronTrigger.getCronExpression(), -1, true);
						} else if(trigger instanceof SimpleTrigger simpleTrigger) {
							sh.scheduleJob(jobDetail, null, simpleTrigger.getRepeatInterval(), true);
						}
					}
				}
				scheduler.resumeJob(jobKey);
				break;
			case TRIGGER:
				log2SecurityLog("trigger job ["+jobKey+"]", issuedBy);
				scheduler.triggerJob(jobKey);
				break;
			default:
				throw new BusException("action not implemented");
			}
		} catch (SchedulerException e) {
			throw new BusException("unable to run action ["+action+"]", e);
		}

		return EmptyMessage.accepted();
	}

	private IbisJobDetail getJobDetail(Scheduler scheduler, JobKey jobKey) {
		try {
			IbisJobDetail jobDetail = (IbisJobDetail) scheduler.getJobDetail(jobKey);
			if(jobDetail == null) {
				throw new BusException("Job ["+jobKey+"] not found");
			}
			return jobDetail;
		} catch (SchedulerException e) {
			throw new BusException("failed to find job", e);
		}
	}

	@ActionSelector(BusAction.DELETE)
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> deleteJob(Message<?> message) {
		String issuedBy = BusMessageUtils.getHeader(message, "issuedBy");
		String jobName = BusMessageUtils.getHeader(message, "job");
		String groupName = BusMessageUtils.getHeader(message, "group");

		JobKey jobKey = getJobKey(jobName, groupName);
		Scheduler scheduler = getScheduler();
		IbisJobDetail jobDetail = getJobDetail(scheduler, jobKey);

		log2SecurityLog("deleting job ["+jobKey+"]", issuedBy);

		try {
			if(jobDetail.getJobType() == JobType.DATABASE) {
				boolean success = false;
				// remove from database
				FixedQuerySender qs = createBean(FixedQuerySender.class);
				qs.setDatasourceName(IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
				qs.setQuery("SELECT COUNT(*) FROM IBISSCHEDULES");
				qs.configure();

				try {
					qs.start();
					try (Connection conn = qs.getConnection()) {

						String query = "DELETE FROM IBISSCHEDULES WHERE JOBNAME=? AND JOBGROUP=?";
						try (PreparedStatement stmt = conn.prepareStatement(query)) {
							stmt.setString(1, jobKey.getName());
							stmt.setString(2, jobKey.getGroup());

							success = stmt.executeUpdate() > 0;
						}
					}
				} catch (LifecycleException | SQLException | JdbcException e) {
					throw new BusException("error removing job from database", e);
				} finally {
					qs.stop();
				}
				if(!success) {
					throw new BusException("failed to remove job from database");
				}
			}

			// remove from memory
			scheduler.deleteJob(jobKey);
		} catch (ConfigurationException e) {
			throw new BusException("error configuring FixedQuerySender to remove job from database", e);
		} catch (SchedulerException e) {
			throw new BusException("failed to delete job", e);
		}

		return EmptyMessage.accepted();
	}
}
