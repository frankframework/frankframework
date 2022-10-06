/*
   Copyright 2013, 2015, 2016, 2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDetail;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IConfigurationAware;
import nl.nn.adapterframework.core.TransactionAttributes;
import nl.nn.adapterframework.scheduler.job.IJob;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;

/**
 * Definition / configuration of scheduler jobs.
 *
 * Specified in the Configuration.xml by a &lt;job&gt; inside a &lt;scheduler&gt;. The scheduler element must
 * be a direct child of configuration, not of adapter.
 * <br/>
 * All registered jobs are displayed in the IbisConsole under 'Scheduler'.
 * <p>
 * <br/>
 * Operation of scheduling:
 * <ul>
 *   <li>at configuration time {@link Configuration#registerScheduledJob(IJob) Configuration.registerScheduledJob()} is called; </li>
 *   <li>this calls {@link SchedulerHelper#scheduleJob(IJob) SchedulerHelper.scheduleJob()};</li>
 *   <li>this creates a Quartz JobDetail object, and copies adapterName, receiverName, function and a reference to the configuration to jobdetail's datamap;</li>
 *   <li>it sets the class to execute to AdapterJob</li>
 *   <li>this job is scheduled using the cron expression</li>
 * </ul>
 * </p>
 *
 * <b>CronExpressions</b>
 * <p>
 * A "Cron-Expression" is a string comprised of 6 or 7 fields separated by
 * white space. The 6 mandatory and 1 optional fields are as follows:<br/>
 *</p>
 * <table cellspacing="8">
 *   <tr>
 *     <th align="left">Field Name</th>
 *     <th align="left">&nbsp;</th>
 *     <th align="left">Allowed Values</th>
 *     <th align="left">&nbsp;</th>
 *     <th align="left">Allowed Special Characters</th>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>Seconds</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>0-59</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>, - * /</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>Minutes</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>0-59</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>, - * /</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>Hours</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>0-23</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>, - * /</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>Day-of-month</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>1-31</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>, - * ? / L C</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>Month</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>1-12 or JAN-DEC</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>, - * /</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>Day-of-Week</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>1-7 or SUN-SAT</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>, - * ? / L C #</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>Year (Optional)</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>empty, 1970-2099</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>, - * /</code></td>
 *   </tr>
 * </table>
 * </p>
 *
 * <p>The '*' character is used to specify all values. For example, "*" in
 * the minute field means "every minute".</p>
 *
 * <p>The '?' character is allowed for the day-of-month and day-of-week fields.
 * It is used to specify 'no specific value'. This is useful when you need
 * to specify something in one of the two fileds, but not the other. See the
 * examples below for clarification.</p>
 *
 * <p>The '-' character is used to specify ranges For example "10-12" in the
 * hour field means "the hours 10, 11 and 12".</p>
 *
 * <p>The ',' character is used to specify additional values. For example
 * "MON,WED,FRI" in the day-of-week field means "the days Monday,
 * Wednesday, and Friday".</p>
 *
 * <p>The '/' character is used to specify increments. For example "0/15" in
 * the seconds field means "the seconds 0, 15, 30, and 45".  And "5/15" in
 * the seconds field means "the seconds 5, 20, 35, and 50".  You can also
 * specify '/' after the '*' character - in this case '*' is equivalent to
 * having '0' before the '/'.</p>
 *
 * <p>The 'L' character is allowed for the day-of-month and day-of-week fields.
 * This character is short-hand for "last", but it has different meaning in each of
 * the two fields.  For example, the value "L" in the  day-of-month field means
 * "the last day of the month" - day 31 for  January, day 28 for February on
 * non-leap years.  If used in the day-of-week field by itself, it simply
 * means "7" or "SAT". But if used in the day-of-week field after another value,
 * it means "the last xxx day of the month" - for example "6L" means
 * "the last friday of the month".  When using the 'L' option, it is
 * important not to specify lists, or ranges of values, as you'll get confusing
 * results.</p>
 *
 * <p>The '#' character is allowed for the day-of-week field.  This character
 * is used to specify "the nth" XX day of the month.  For example, the value
 * of "6#3" in the day-of-week field means the third Friday of the month
 * (day 6 = Friday and "#3" = the 3rd one in the month). Other
 * examples: "2#1" = the first Monday of the month and  "4#5" = the fifth
 * Wednesday of the month.  Note that if you specify "#5" and there is not 5 of
 * the given day-of-week in the month, then no firing will occur that month.</p>
 *
 * <p>The 'C' character is allowed for the day-of-month and day-of-week fields.
 * This character is short-hand for "calendar".  This means values are
 * calculated against the associated calendar, if any.  If no calendar is
 * associated, then it is equivalent to having an all-inclusive calendar.
 * A value of "5C" in the day-of-month field means "the first day included by
 * the calendar on or after the 5th".  A value of "1C" in the day-of-week field
 * means "the first day included by the calendar on or after sunday".</p>
 *
 * <p>The legal characters and the names of months and days of the week are not
 * case sensitive.</p>
 *
 * <p>Here are some full examples:<br/>
 * <table cellspacing="8">
 *   <tr>
 *     <th align="left">Expression</th>
 *     <th align="left">&nbsp;</th>
 *     <th align="left">Meaning</th>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 0 12 * * ?"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 12pm (noon) every day</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 ? * *"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am every day</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 * * ?"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am every day</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 * * ? *"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am every day</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 * * ? 2005"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am every day during the year 2005</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 * 14 * * ?"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire every minute starting at 2pm and ending at 2:59pm, every day</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 0/5 14 * * ?"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 0/5 14,18 * * ?"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire every 5 minutes starting at 2pm and ending at 2:55pm, AND fire every 5 minutes starting at 6pm and ending at 6:55pm, every day</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 0-5 14 * * ?"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire every minute starting at 2pm and ending at 2:05pm, every day</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 10,44 14 ? 3 WED"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 ? * MON-FRI"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 15 * ?"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am on the 15th day of every month</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 L * ?"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am on the last day of every month</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 ? * 6L"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am on the last Friday of every month</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 ? * 6L"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am on the last Friday of every month</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 ? * 6L 2002-2005"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am on every last friday of every month during the years 2002, 2003, 2004 and 2005</code></td>
 *   </tr>
 *   <tr>
 *     <td align="left"><code>"0 15 10 ? * 6#3"</code></td>
 *     <td align="left">&nbsp;</th>
 *     <td align="left"><code>Fire at 10:15am on the third Friday of every month</code></td>
 *   </tr>
 * </table>
 * </p>
 *
 * <p>Pay attention to the effects of '?' and '*' in the day-of-week and
 * day-of-month fields!</p>
 *
 * <p><b>NOTES:</b>
 * <ul>
 *   <li>
 *      Support for the features described for the 'C' character is
 *      not complete.
 *   </li>
 *   <li>
 *      Support for specifying both a day-of-week and a day-of-month
 *      value is not complete (you'll need to use the '?' character in on of these
 *      fields).
 *   </li>
 *   <li>Be careful when setting fire times between mid-night and 1:00 AM -
 *       "daylight savings" can cause a skip or a repeat depending on whether
 *       the time moves back or jumps forward.
 *   </li>
 * </ul>
 * </p>
 *
 */
public abstract class JobDef extends TransactionAttributes implements IConfigurationAware, IJob {

	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter boolean configured;

	private @Getter String name;
	private @Getter String description;
	private @Getter String jobGroup = null;
	private @Getter String cronExpression;
	private @Getter long interval = -1;

	private @Getter(onMethod = @__(@Override)) Locker locker = null;
	private @Getter int numThreads = 1;
	private int countThreads = 0;

	private MessageKeeper messageKeeper; //instantiated in configure()
	private int messageKeeperSize = 10; //default length

	private StatisticsKeeper statsKeeper;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getName())) {
			throw new ConfigurationException("a name must be specified");
		}

		if(StringUtils.isEmpty(getJobGroup())) { //If not explicitly set, configure this JobDef under the config it's specified in
			setJobGroup(applicationContext.getId());
		}

		SchedulerHelper.validateJob(getJobDetail(), getCronExpression());

		if (getLocker()!=null) {
			getLocker().configure();
		}

		statsKeeper = new StatisticsKeeper(getName());

		getMessageKeeper().add("job successfully configured");
		configured = true;
	}

	@Override
	public JobDetail getJobDetail() {
		return IbisJobBuilder.fromJobDef(this).build();
	}

	public synchronized boolean incrementCountThreads() {
		if (countThreads < getNumThreads()) {
			countThreads++;
			return true;
		}
		return false;
	}

	public synchronized void decrementCountThreads() {
		countThreads--;
	}

	/** Called before executeJob to prepare resources for executeJob method. Returns false if job does not need to run */
	public boolean beforeExecuteJob() {
		return true;
	}

	/** Called from {@link ConfiguredJob} which should trigger this job definition. */
	@Override
	public final void executeJob() {
		if (!incrementCountThreads()) {
			String msg = "maximum number of threads that may execute concurrently [" + getNumThreads() + "] is exceeded, the processing of this thread will be aborted";
			getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
			log.error(getLogPrefix()+msg);
			return;
		}
		try {
			if(beforeExecuteJob()) {
				if (getLocker() != null) {
					String objectId = null;
					try {
						objectId = getLocker().acquire(getMessageKeeper());
					} catch (Exception e) {
						getMessageKeeper().add(e.getMessage(), MessageKeeperLevel.ERROR);
						log.error(getLogPrefix()+e.getMessage());
					}
					if (objectId!=null) {
						TimeoutGuard tg = new TimeoutGuard("Job "+getName());
						try {
							tg.activateGuard(getTransactionTimeout());
							runJob();
						} finally {
							if (tg.cancel()) {
								log.error(getLogPrefix()+"thread has been interrupted");
							}
						}
						try {
							getLocker().release(objectId);
						} catch (Exception e) {
							String msg = "error while removing lock: " + e.getMessage();
							getMessageKeeper().add(msg, MessageKeeperLevel.WARN);
							log.warn(getLogPrefix()+msg);
						}
					} else {
						getMessageKeeper().add("unable to acquire lock ["+getName()+"] did not run");
					}
				} else {
					runJob();
				}
			}
		} finally {
			decrementCountThreads();
		}
	}

	/**
	 * Wrapper around running the job, to log and deal with Exception in a uniform manner.
	 */
	private void runJob() {
		long startTime = System.currentTimeMillis();
		getMessageKeeper().add("starting to run the job");

		try {
			execute();
		} catch (Exception e) {
			String msg = "error while executing job ["+this+"] (as part of scheduled job execution): " + e.getMessage();
			getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
			log.error(getLogPrefix()+msg, e);
		}

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		statsKeeper.addValue(duration);
		getMessageKeeper().add("finished running the job in ["+(duration)+"] ms");
	}

	protected IbisManager getIbisManager() {
		return getApplicationContext().getBean(IbisManager.class);
	}

	protected String getLogPrefix() {
		return "Job ["+getName()+"] ";
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(this.getClass().getSimpleName());
		if(name != null) builder.append(" name ["+name+"]");
		if(jobGroup != null) builder.append(" jobGroup ["+jobGroup+"]");
		if(cronExpression != null) builder.append(" cronExpression ["+cronExpression+"]");
		if(interval > -1) builder.append(" interval ["+interval+"]");
		return builder.toString();
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

	@Override
	/** Name of the job
	 * @ff.mandatory
	 */
	public void setName(String name) {
		this.name = name;
	}

	/** Description of the job */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	@Override
	public void setInterval(long interval) {
		this.interval = interval;
	}

	@Override
	public void setLocker(Locker locker) {
		this.locker = locker;
		locker.setName("Locker of job ["+getName()+"]");
	}

	/** Number of threads that may execute concurrently
	 * @ff.default 1
	 */
	public void setNumThreads(int newNumThreads) {
		numThreads = newNumThreads;
	}

	@Override
	public synchronized MessageKeeper getMessageKeeper() {
		if (messageKeeper == null)
			messageKeeper = new MessageKeeper(messageKeeperSize < 1 ? 1 : messageKeeperSize);
		return messageKeeper;
	}

	/** Number of messages displayed in ibisconsole
	 * @ff.default 10
	 */
	public void setMessageKeeperSize(int size) {
		this.messageKeeperSize = size;
	}

	@Override
	public synchronized StatisticsKeeper getStatisticsKeeper() {
		return statsKeeper;
	}
}