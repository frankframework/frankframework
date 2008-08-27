/*
 * $Log: JobDef.java,v $
 * Revision 1.9  2008-08-27 16:21:48  europe\L190409
 * added configure()
 *
 * Revision 1.8  2007/12/12 09:09:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow for query-type jobs
 *
 * Revision 1.7  2007/05/16 11:48:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved javadoc
 *
 * Revision 1.6  2007/02/21 16:04:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Definition / configuration of scheduler jobs.
 * 
 * Specified in the Configuration.xml by a &lt;job&gt; inside a &lt;scheduler&gt;. The scheduler element must
 * be a direct child of configuration, not of adapter.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Job</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDescription(String) description}</td><td>optional description of the job</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCronExpression(String) cronExpression}</td><td>cron expression that determines the frequency of excution (see below)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFunction(String) function}</td><td>one of: StopAdapter, StartAdapter, StopReceiver, StartReceiver, SendMessage, ExecuteQuery</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAdapterName(String) adapterName}</td><td>Adapter on which job operates</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReceiverName(String) receiverName}</td><td>Receiver on which job operates. If function is 'sendMessage' is used this name is also used as name of JavaListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be excecuted</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * <br>
 * Operation of scheduling:
 * <ul>
 *   <li>at configuration time {@link nl.nn.adapterframework.configuration.Configuration#registerScheduledJob(JobDef) Configuration.registerScheduledJob()} is called; </li>
 *   <li>this calls {@link nl.nn.adapterframework.scheduler.SchedulerHelper.scheduleJob() SchedulerHelper.scheduleJob(Object, JobDef)};</li>
 *   <li>this creates a Quartz JobDetail object, and copies adaptername, receivername, function and a reference to the configuration to jobdetail's datamap;</li>
 *   <li>it sets the class to execute to AdapterJob</li>
 *   <li>this job is scheduled using the cron expression</li> 
 * </ul>
 * 
 * Operation the job is triggered:
 * <ul>
 *   <li>AdapterJob.execute is called</li>
 *   <li>AdapterJob.execute calls config.handleAdapter()</li>
 *   <li>Depending on the value of <code>function</code> the Adapter or Receiver is stopped or started, or an empty message is sent</li>
 *   <li>If function=sendMessage, an IbisLocalSender is used to call a JavaListener that has to have an attribute <code>name</code> that is equal to receiverName!!</li>
 * </ul>
 *
 * All registered jobs are displayed in the IbisConsole under 'Show Scheduler Status'.
 * <p>
 * N.B.: Jobs can only be specified in the Configuration.xml <i>BELOW</i> the adapter called. It must be already defined!
 *
 * <b>CronExpressions</b>
 *  * <p>A "Cron-Expression" is a string comprised of 6 or 7 fields separated by
 * white space. The 6 mandatory and 1 optional fields are as follows:<br>
 *
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
 * is used to specify "the nth" XXX day of the month.  For example, the value
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
 * <p>Here are some full examples:<br>
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
 * @author  Johan  Verrips
 * @version Id
 */
public class JobDef {
	public static final String version = "$RCSfile: JobDef.java,v $  $Revision: 1.9 $ $Date: 2008-08-27 16:21:48 $";
	
    private String name;
    private String cronExpression;
    private String function;
    private String adapterName;
    private String description;
    private String receiverName;
	private String query;
	private String jmsRealm;

    private String jobGroup=AppConstants.getInstance().getString("scheduler.defaultJobGroup", "DEFAULT");
    
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public void configure(Configuration config) throws ConfigurationException {
		if (!("dumpStatistics".equals(function) || "dumpStatisticsAndReset".equals(function))) {
			if (config.getRegisteredAdapter(getAdapterName()) == null) {
				String msg="Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] not registered.";
				throw new ConfigurationException(msg);
			}
			if (StringUtils.isNotEmpty(getReceiverName())){
				if (! config.isRegisteredReceiver(getAdapterName(), getReceiverName())) {
					String msg="Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] receiver ["+getReceiverName()+"] not registered.";
					throw new ConfigurationException(msg);
				}
			}
		}
	}


   /**
     * Defaults to the value under key <code>scheduler.defaultJobGroup</code> in the {@link AppConstants}.
     * If the value is not specified, it assumes <code>DEFAULT</code>
     * @param jobGroup
     */
	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}
	public String getJobGroup() {
		return jobGroup;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
	   return description;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}
	public String getCronExpression() {
		return cronExpression;
	}

	public void setFunction(String function) {
		this.function = function;
	}
	public String getFunction() {
		return function;
	}

	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
	}
	public String getAdapterName() {
		return adapterName;
	}
  
	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}
	public String getReceiverName() {
		return receiverName;
	}

	public void setQuery(String query) {
		this.query = query;
	}
	public String getQuery() {
		return query;
	}
	
	public void setJmsRealm(String jmsRealm) {
		this.jmsRealm = jmsRealm;
	}
	public String getJmsRealm() {
		return jmsRealm;
	}

}
