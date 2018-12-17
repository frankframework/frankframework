/*
   Copyright 2013, 2015, 2016 Nationale-Nederlanden

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

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.statistics.ItemList;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
  */
public class SchedulerAdapter {
	protected Logger log=LogUtil.getLogger(this);

    private DecimalFormat tf=new DecimalFormat(ItemList.PRINT_FORMAT_TIME);
    private DecimalFormat pf=new DecimalFormat(ItemList.PRINT_FORMAT_PERC);
	
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
				el.addAttribute("name", jgnames.get(i));

				// process jobs within group
				XmlBuilder jb = new XmlBuilder("jobs");
				Set<JobKey> jobKeys = theScheduler.getJobKeys(GroupMatcher.jobGroupEquals(jgnames.get(i)));

				for (JobKey jobKey : jobKeys) {
					XmlBuilder jn = new XmlBuilder("job");
					jn.addAttribute("name", jobKey.getName());

					// details for job
					XmlBuilder jd = jobDetailToXmlBuilder(theScheduler, jobKey.getName(), jobKey.getGroup());
					jn.addSubElement(jd);

					// get the triggers for this job
					XmlBuilder tr= getJobTriggers(theScheduler, jobKey.getName(), jobKey.getGroup());
					jn.addSubElement(tr);

					XmlBuilder datamap = jobDataMapToXmlBuilder(theScheduler, jobKey.getName(), jobKey.getGroup());
					jn.addSubElement(datamap);
					jb.addSubElement(jn);

					JobDef jobDef = null;
					for (Configuration configuration : ibisManager.getConfigurations()) {
						jobDef = configuration.getScheduledJob(jobKey.getName());
						if (jobDef != null) {
							break;
						}
					}
					XmlBuilder ms= getJobMessages(jobDef);
					jn.addSubElement(ms);
					XmlBuilder jrs= getJobRunStatistics(jobDef);
					jn.addSubElement(jrs);
				}
				el.addSubElement(jb);
				xbRoot.addSubElement(el);
			}
		} catch (org.quartz.SchedulerException se) {
			log.error(se);
		}
		return xbRoot;
	}

	public XmlBuilder getJobTriggers(Scheduler theScheduler, String jobName, String groupName) {

        XmlBuilder xbRoot = new XmlBuilder("triggersForJob");

        xbRoot.addAttribute("jobName", jobName);
        xbRoot.addAttribute("groupName", groupName);
        try {
            List<String> tgnames = theScheduler.getTriggerGroupNames();

            for (int i = 0; i < tgnames.size(); i++) {
          	  	List<? extends Trigger> triggers = theScheduler.getTriggersOfJob(JobKey.jobKey(jobName, groupName));

                for (int j = 0; j < triggers.size(); j++) {
                	// Trigger trigger = theScheduler.getTrigger(TriggerKey.triggerKey(triggerName, groupName));

                    if ((triggers.get(j).getJobKey().getName().equals(jobName)) && (triggers.get(j).getJobKey().getGroup().equals(groupName))) {
                    	XmlBuilder tr = triggerToXmlBuilder(theScheduler, triggers.get(j).getKey().getName(), triggers.get(j).getKey().getGroup());

                        xbRoot.addSubElement(tr);
                    }
                }
            }
        } catch (org.quartz.SchedulerException se) {
            log.error(se);

        }

        return xbRoot;
    }

	public XmlBuilder getJobMessages(JobDef jobdef) {
		XmlBuilder jobMessages = new XmlBuilder("jobMessages");
		if (jobdef!=null) {
			MessageKeeper jobMessageKeeper = jobdef.getMessageKeeper();
			if (jobMessageKeeper!=null) {
				for (int t=0; t<jobMessageKeeper.size(); t++) {
					XmlBuilder jobMessage=new XmlBuilder("jobMessage");
					jobMessage.setValue(jobMessageKeeper.getMessage(t).getMessageText(),true);
					jobMessage.addAttribute("date", DateUtils.format(jobMessageKeeper.getMessage(t).getMessageDate(), DateUtils.FORMAT_FULL_GENERIC));
					jobMessage.addAttribute("level", jobMessageKeeper.getMessage(t).getMessageLevel());
					jobMessages.addSubElement(jobMessage);
				}
			}
		}
		return jobMessages;
	}

	public XmlBuilder getJobRunStatistics(JobDef jobdef) {
		XmlBuilder jobRunStatistics = new XmlBuilder("jobRunStatistics");
		if (jobdef != null) {
			StatisticsKeeper statsKeeper = jobdef.getStatisticsKeeper();
			if (statsKeeper != null) {
				XmlBuilder jobRunDuration = statsKeeper.toXml("jobRunDuration",
						false, tf, pf);
				jobRunStatistics.addSubElement(jobRunDuration);
			}
		}
		return jobRunStatistics;
	}

    public XmlBuilder getSchedulerCalendarNamesToXml(Scheduler theScheduler) {
        XmlBuilder xbRoot = new XmlBuilder("schedulerCalendars");

        try {
            List<String> names = theScheduler.getCalendarNames();

            for (int i = 0; i < names.size(); i++) {
                XmlBuilder el = new XmlBuilder("calendar");

                el.setValue(names.get(i));
                xbRoot.addSubElement(el);
            }
        } catch (org.quartz.SchedulerException se) {
            log.error(se.toString());
        }
        return xbRoot;
    }
 
    public XmlBuilder getSchedulerMetaDataToXml(Scheduler theScheduler) {
        XmlBuilder xbRoot = new XmlBuilder("schedulerMetaData");

        try {
            SchedulerMetaData smd = theScheduler.getMetaData();

            xbRoot.addAttribute("schedulerName", smd.getSchedulerName());
            xbRoot.addAttribute("schedulerInstanceId", smd.getSchedulerInstanceId().toString());
            xbRoot.addAttribute("version", smd.getVersion());
            xbRoot.addAttribute("isPaused", (smd.isInStandbyMode() ? "True" : "False"));
            xbRoot.addAttribute("isSchedulerRemote", (smd.isSchedulerRemote() ? "True" : "False"));
            xbRoot.addAttribute("isShutdown", (smd.isShutdown() ? "True" : "False"));
            xbRoot.addAttribute("isStarted", (smd.isStarted() ? "True" : "False"));
            xbRoot.addAttribute("jobStoreSupportsPersistence", (smd.isJobStoreSupportsPersistence() ? "True" : "False"));
            xbRoot.addAttribute("numJobsExecuted", Integer.toString(smd.getNumberOfJobsExecuted()));
            try {
                Date runningSince = smd.getRunningSince();

                xbRoot.addAttribute("runningSince", (null == runningSince ? "unknown" : DateUtils.format(runningSince, DateUtils.FORMAT_GENERICDATETIME)));
            } catch (Exception e) {
	            log.debug(e);
	        };
            xbRoot.addAttribute("jobStoreClass", smd.getJobStoreClass().getName());
            xbRoot.addAttribute("schedulerClass", smd.getSchedulerClass().getName());
            xbRoot.addAttribute("threadPoolClass", smd.getThreadPoolClass().getName());
            xbRoot.addAttribute("threadPoolSize", Integer.toString(smd.getThreadPoolSize()));
        } catch (SchedulerException se) {
            log.error(se);
        }

        return xbRoot;
    }

	public XmlBuilder getTriggerGroupNamesWithTriggersToXml(Scheduler theScheduler) {
		XmlBuilder xbRoot = new XmlBuilder("triggerGroups");
		
		try {
			// process groups
			List<String> tgnames = theScheduler.getTriggerGroupNames();
			
			for (int i = 0; i < tgnames.size(); i++) {
				XmlBuilder el = new XmlBuilder("triggerGroup");
				
				el.addAttribute("name", tgnames.get(i));
				
				// process jobs within group
				XmlBuilder tgg = new XmlBuilder("triggers");
				List<? extends Trigger> triggers = theScheduler.getTriggersOfJob(JobKey.jobKey(tgnames.get(i)));
				
				for (int j = 0; j < triggers.size(); j++) {
					XmlBuilder tn = new XmlBuilder("trigger");
					
					tn.addAttribute("name", triggers.get(j).getKey().getName());
					
					//detail of trigger
					XmlBuilder td = triggerToXmlBuilder(theScheduler, triggers.get(j).getKey().getName(), tgnames.get(i));
					
					tn.addSubElement(td);
					
					tgg.addSubElement(tn);
				}
				el.addSubElement(tgg);
				
				xbRoot.addSubElement(el);
			}
		} catch (org.quartz.SchedulerException se) {
			log.error(se);
		}
		return xbRoot;
	}

    public XmlBuilder jobDataMapToXmlBuilder(Scheduler theScheduler, String jobName, String groupName) {

        XmlBuilder xbRoot = new XmlBuilder("jobDataMap");

        try {
            JobDataMap jd = theScheduler.getJobDetail(JobKey.jobKey(jobName, groupName)).getJobDataMap();

            xbRoot.addAttribute("containsTransientData", (jd.containsTransientData() ? "True" : "False"));
            xbRoot.addAttribute("allowsTransientData", (jd.getAllowsTransientData() ? "True" : "False"));
            xbRoot.addAttribute("jobName", jobName);
            xbRoot.addAttribute("groupName", groupName);

            String[] keys = jd.getKeys();

            for (int i = 0; i < keys.length; i++) {
                String name = keys[i];
                String value="";
                if (jd.get(keys[i])!=null) {
                   value = jd.get(keys[i]).toString();
                }
                Object obj=jd.get(keys[i]);
                XmlBuilder ds = new XmlBuilder("property");


                ds.addAttribute("key", name);
                if (obj!=null){
                    ds.addAttribute("className", obj.getClass().getName());
                } else ds.addAttribute("className", "null");
                ds.setValue(value);

                xbRoot.addSubElement(ds);
            }
        } catch (org.quartz.SchedulerException se) {
            log.error(se);
        }
        return xbRoot;
    }
    
    public XmlBuilder jobDetailToXmlBuilder(Scheduler theScheduler, String jobName, String groupName) {
        XmlBuilder xbRoot = new XmlBuilder("jobDetail");

        try {
            JobDetail jd = theScheduler.getJobDetail(JobKey.jobKey(jobName, groupName));

            xbRoot.addAttribute("fullName", jd.getKey().getGroup() + "." + jd.getKey().getName());
            xbRoot.addAttribute("jobName", jd.getKey().getName());
            xbRoot.addAttribute("groupName", jd.getKey().getGroup());
            String description="-";
            if (StringUtils.isNotEmpty(jd.getDescription()))
                description=jd.getDescription();

            xbRoot.addAttribute("description", description);
            xbRoot.addAttribute("isStateful", (jd.isConcurrentExectionDisallowed() && jd.isPersistJobDataAfterExecution() ? "True" : "False"));
            xbRoot.addAttribute("isDurable", (jd.isDurable() ? "True" : "False"));
            xbRoot.addAttribute("jobClass", jd.getJobClass().getName());
            
        } catch (org.quartz.SchedulerException se) {
            log.error(se);
        }
        return xbRoot;
    }

    public XmlBuilder triggerToXmlBuilder(Scheduler theScheduler, String triggerName, String groupName) {
        XmlBuilder xbRoot = new XmlBuilder("triggerDetail");

        try {
            Trigger trigger = theScheduler.getTrigger(TriggerKey.triggerKey(triggerName, groupName));

            xbRoot.addAttribute("fullName", trigger.getKey().getGroup() + "." + trigger.getKey().getName());
            xbRoot.addAttribute("triggerName", trigger.getKey().getName());
            xbRoot.addAttribute("triggerGroup", trigger.getKey().getGroup());
            String cn = trigger.getCalendarName();

            xbRoot.addAttribute("calendarName", (cn == null ? "none" : cn));
            Date date;

            try {
                date = trigger.getEndTime();
                xbRoot.addAttribute("endTime", (null == date ? "" : DateUtils.format(date, DateUtils.FORMAT_GENERICDATETIME)));
            } catch (Exception e) { log.debug(e); };
            try {
                date = trigger.getFinalFireTime();
                xbRoot.addAttribute("finalFireTime", (null == date ? "" : DateUtils.format(date, DateUtils.FORMAT_GENERICDATETIME)));
            } catch (Exception e) { log.debug(e); };
            try {
                date = trigger.getPreviousFireTime();
                xbRoot.addAttribute("previousFireTime", (null == date ? "" : DateUtils.format(date, DateUtils.FORMAT_GENERICDATETIME)));
            } catch (Exception e) { log.debug(e); };
            try {
                date = trigger.getNextFireTime();
                xbRoot.addAttribute("nextFireTime", (null == date ? "" : DateUtils.format(date, DateUtils.FORMAT_GENERICDATETIME)));
            } catch (Exception e) { log.debug(e); };
            try {
                date = trigger.getStartTime();
                xbRoot.addAttribute("startTime", (null == date ? "" : DateUtils.format(date, DateUtils.FORMAT_GENERICDATETIME)));
            } catch (Exception e) { log.debug(e); };
            xbRoot.addAttribute("misfireInstruction", Integer.toString(trigger.getMisfireInstruction()));
            if (trigger instanceof CronTrigger) {
                xbRoot.addAttribute("triggerType", "cron");
                xbRoot.addAttribute("cronExpression", ((CronTrigger)trigger).getCronExpression());
            } else if (trigger instanceof SimpleTrigger) {
                xbRoot.addAttribute("triggerType", "simple");
                xbRoot.addAttribute("repeatInterval", ((SimpleTrigger)trigger).getRepeatInterval());
            } else {
                xbRoot.addAttribute("triggerType", "unknown");
            }

            xbRoot.addAttribute("jobGroup", trigger.getJobKey().getGroup());
            xbRoot.addAttribute("jobName", trigger.getJobKey().getName());

        } catch (SchedulerException se) {
            log.error(se);
        }
        return xbRoot;
    }
}
