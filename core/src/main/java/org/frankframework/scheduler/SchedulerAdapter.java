/*
   Copyright 2013, 2015, 2016, 2019 Nationale-Nederlanden

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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisManager;
import org.frankframework.scheduler.job.IJob;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper;
import org.frankframework.util.XmlBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
/**
 * The SchedulerAdapter is an adapter for the <a href="http://quartz.sourceforge.net">Quartz scheduler</a> <br/>
 * It transforms the information from the scheduler to XML.
 * @author  Johan Verrips
 * @since 4.0
 * @deprecated An external time series database should be used.
  */
@Deprecated(forRemoval = true, since = "8.1")
public class SchedulerAdapter {
	protected Logger log=LogUtil.getLogger(this);

	/**
	 * Get all jobgroups, jobs within this group, the jobdetail and the
	 * associated triggers in XML format.
	 */
	public XmlBuilder getJobGroupNamesWithJobsToXml(Scheduler theScheduler, IbisManager ibisManager) {
		XmlBuilder xbRoot = new XmlBuilder("jobGroups");

		try {
			// process groups
			List<String> jgnames = theScheduler.getJobGroupNames();

			for (int i = 0; i < jgnames.size(); i++) {
				XmlBuilder el = new XmlBuilder("jobGroup");
				String jobGroupName = jgnames.get(i);
				el.addAttribute("name", jobGroupName);

				// process jobs within group
				XmlBuilder jb = new XmlBuilder("jobs");
				Set<JobKey> jobKeys = theScheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroupName));

				for (JobKey jobKey : jobKeys) {
					XmlBuilder jn = new XmlBuilder("job");
					String jobName = jobKey.getName();
					jn.addAttribute("name", jobName);

					// details for job
					JobDetail jobDetail = theScheduler.getJobDetail(jobKey);
					XmlBuilder jd = jobDetailToXmlBuilder(jobDetail);
					jn.addSubElement(jd);

					// get the triggers for this job
					List<? extends Trigger> triggers = theScheduler.getTriggersOfJob(jobKey);
					XmlBuilder tr = getJobTriggers(triggers);
					jn.addSubElement(tr);


					JobDataMap jobDataMap = jobDetail.getJobDataMap();
					XmlBuilder datamap = jobDataMapToXmlBuilder(jobDataMap);
					jn.addSubElement(datamap);
					jb.addSubElement(jn);

					IJob jobDef = null;
					if(ibisManager != null) {
						for (Configuration configuration : ibisManager.getConfigurations()) {
							jobDef = configuration.getScheduledJob(jobName);
							if (jobDef != null) {
								break;
							}
						}
					}
					XmlBuilder ms = getJobMessages(jobDef);
					jn.addSubElement(ms);
				}
				el.addSubElement(jb);
				xbRoot.addSubElement(el);
			}
		} catch (SchedulerException se) {
			log.error(se);
		}

		return xbRoot;
	}

	public XmlBuilder getJobTriggers(List<? extends Trigger> triggers) {
		XmlBuilder xbRoot = new XmlBuilder("triggers");

		for (Trigger trigger : triggers) {
			XmlBuilder tr = triggerToXmlBuilder(trigger);
			xbRoot.addSubElement(tr);
		}

		return xbRoot;
	}

	public XmlBuilder getJobMessages(IJob jobdef) {
		XmlBuilder jobMessages = new XmlBuilder("jobMessages");
		if (jobdef!=null) {
			MessageKeeper jobMessageKeeper = jobdef.getMessageKeeper();
			if (jobMessageKeeper!=null) {
				for (int t=0; t<jobMessageKeeper.size(); t++) {
					XmlBuilder jobMessage=new XmlBuilder("jobMessage");
					jobMessage.setValue(jobMessageKeeper.getMessage(t).getMessageText(),true);
					jobMessage.addAttribute("date", DateFormatUtils.format(jobMessageKeeper.getMessage(t).getMessageDate(), DateFormatUtils.FULL_GENERIC_FORMATTER));
					jobMessage.addAttribute("level", jobMessageKeeper.getMessage(t).getMessageLevel());
					jobMessages.addSubElement(jobMessage);
				}
			}
		}
		return jobMessages;
	}

	public XmlBuilder getSchedulerCalendarNamesToXml(Scheduler theScheduler) {
		XmlBuilder xbRoot = new XmlBuilder("schedulerCalendars");

		try {
			List<String> names = theScheduler.getCalendarNames();

			for(int i = 0; i < names.size(); i++) {
				XmlBuilder el = new XmlBuilder("calendar");

				el.setValue(names.get(i));
				xbRoot.addSubElement(el);
			}
		} catch (SchedulerException se) {
			log.error(se.toString());
		}
		return xbRoot;
	}

	public XmlBuilder getSchedulerMetaDataToXml(Scheduler theScheduler) {
		XmlBuilder xbRoot = new XmlBuilder("schedulerMetaData");

		try {
			SchedulerMetaData smd = theScheduler.getMetaData();

			xbRoot.addAttribute("schedulerName", smd.getSchedulerName());
			xbRoot.addAttribute("schedulerInstanceId", smd.getSchedulerInstanceId());
			xbRoot.addAttribute("version", smd.getVersion());
			xbRoot.addAttribute("isPaused", smd.isInStandbyMode());
			xbRoot.addAttribute("isSchedulerRemote", smd.isSchedulerRemote());
			xbRoot.addAttribute("isShutdown", smd.isShutdown());
			xbRoot.addAttribute("isStarted", smd.isStarted());
			xbRoot.addAttribute("jobStoreSupportsPersistence", smd.isJobStoreSupportsPersistence());
			xbRoot.addAttribute("numJobsExecuted", Integer.toString(smd.getNumberOfJobsExecuted()));
			try {
				Date runningSince = smd.getRunningSince();

				xbRoot.addAttribute("runningSince", (null == runningSince ? "unknown" : DateFormatUtils.format(runningSince, DateFormatUtils.GENERIC_DATETIME_FORMATTER)));
			} catch (Exception e) {
				log.debug(e);
			}

			xbRoot.addAttribute("jobStoreClass", smd.getJobStoreClass().getName());
			xbRoot.addAttribute("schedulerClass", smd.getSchedulerClass().getName());
			xbRoot.addAttribute("threadPoolClass", smd.getThreadPoolClass().getName());
			xbRoot.addAttribute("threadPoolSize", Integer.toString(smd.getThreadPoolSize()));
		} catch (SchedulerException se) {
			log.error(se);
		}

		return xbRoot;
	}

	public XmlBuilder jobDataMapToXmlBuilder(JobDataMap jd) {

		XmlBuilder xbRoot = new XmlBuilder("jobDataMap");
		String[] keys = jd.getKeys();

		for (int i = 0; i < keys.length; i++) {
			String name = keys[i];
			String value="";
			Object obj = jd.get(keys[i]);
			if (obj!=null) {
				value = obj.toString();
			}
			XmlBuilder ds = new XmlBuilder("property");

			ds.addAttribute("key", name);
			if (obj!=null){
				ds.addAttribute("className", obj.getClass().getName());
			} else {
				ds.addAttribute("className", "null");
			}
			ds.setValue(value);

			xbRoot.addSubElement(ds);
		}

		return xbRoot;
	}

	public XmlBuilder jobDetailToXmlBuilder(JobDetail jobDetail) {
		XmlBuilder xbRoot = new XmlBuilder("jobDetail");

		String name = jobDetail.getKey().getName();
		xbRoot.addAttribute("name", name);
		xbRoot.addAttribute("fullName", jobDetail.getKey().getGroup() + "." + name);
		String description = "-";
		if(StringUtils.isNotEmpty(jobDetail.getDescription()))
			description = jobDetail.getDescription();

		xbRoot.addAttribute("description", description);
		xbRoot.addAttribute("isStateful", (jobDetail.isConcurrentExecutionDisallowed() && jobDetail.isPersistJobDataAfterExecution()));
		xbRoot.addAttribute("isDurable", jobDetail.isDurable());
		xbRoot.addAttribute("jobClass", jobDetail.getJobClass().getName());

		return xbRoot;
	}

	public XmlBuilder triggerToXmlBuilder(Trigger trigger) {
		XmlBuilder xbRoot = new XmlBuilder("triggerDetail");

		TriggerKey triggerKey = trigger.getKey();
		xbRoot.addAttribute("fullName", triggerKey.getGroup() + "." + triggerKey.getName());
		xbRoot.addAttribute("triggerName", triggerKey.getName());
		xbRoot.addAttribute("triggerGroup", triggerKey.getGroup());
		String cn = trigger.getCalendarName();

		xbRoot.addAttribute("calendarName", (cn == null ? "none" : cn));

		xbRoot.addAttribute("endTime", convertDate(trigger.getEndTime()));
		xbRoot.addAttribute("finalFireTime", convertDate(trigger.getFinalFireTime()));
		xbRoot.addAttribute("previousFireTime", convertDate(trigger.getPreviousFireTime()));
		xbRoot.addAttribute("nextFireTime", convertDate(trigger.getNextFireTime()));
		xbRoot.addAttribute("startTime", convertDate(trigger.getStartTime()));

		xbRoot.addAttribute("misfireInstruction", Integer.toString(trigger.getMisfireInstruction()));
		if(trigger instanceof CronTrigger cronTrigger) {
			xbRoot.addAttribute("triggerType", "cron");
			xbRoot.addAttribute("cronExpression", cronTrigger.getCronExpression());
		} else if(trigger instanceof SimpleTrigger simpleTrigger) {
			xbRoot.addAttribute("triggerType", "simple");
			xbRoot.addAttribute("repeatInterval", simpleTrigger.getRepeatInterval());
		} else {
			xbRoot.addAttribute("triggerType", "unknown");
		}

		return xbRoot;
	}

	private String convertDate(Date date) {
		try {
			return null == date ? "" : DateFormatUtils.format(date, DateFormatUtils.GENERIC_DATETIME_FORMATTER);
		} catch (Exception e) {
			log.debug("cannot convert date [{}] to format [{}]", date, DateFormatUtils.FORMAT_DATETIME_GENERIC, e);
			return "";
		}
	}
}
