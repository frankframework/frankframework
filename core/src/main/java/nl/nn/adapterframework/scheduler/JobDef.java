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

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.senders.IbisLocalSender;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.DirectoryCleaner;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

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
 * <tr><td>{@link #setCronExpression(String) cronExpression}</td><td>cron expression that determines the frequency of execution (see below)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInterval(long) interval}</td><td>Repeat the job at the specified number of ms. Set to 0 to only run once just after all adapters have been started. Keep cronExpression empty to use interval.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFunction(String) function}</td><td>one of: StopAdapter, StartAdapter, StopReceiver, StartReceiver, SendMessage, ExecuteQuery</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConfigurationName(String) configurationName}</td><td>Configuration on which job operates</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAdapterName(String) adapterName}</td><td>Adapter on which job operates</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReceiverName(String) receiverName}</td><td>Receiver on which job operates. If function is 'sendMessage' is used this name is also used as name of JavaListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be executed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransactionAttribute(String) transactionAttribute}</td><td>Defines transaction and isolation behaviour. Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. Possible values are: 
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table></td><td>Supports</td></tr>
 * <tr><td>{@link #setTransactionTimeout(int) transactionTimeout}</td><td>Timeout (in seconds) of transaction started to process a message.</td><td><code>0</code> (use system default)</code></td></tr>
 * <tr><td>{@link #setNumThreads(int) numThreads}</td><td>the number of threads that may execute concurrently</td><td>1</td></tr>
 * <tr><td>{@link #setMessageKeeperSize(int) messageKeeperSize}</td><td>number of message displayed in IbisConsole</td><td>10</td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <tr><th>nested elements (accessible in descender-classes)</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.util.Locker locker}</td><td>optional: the job will only be executed if a lock could be set successfully</td></tr>
 * </table>
 * </p>
 * <p> 
 * <br>
 * Operation of scheduling:
 * <ul>
 *   <li>at configuration time {@link nl.nn.adapterframework.configuration.Configuration#registerScheduledJob(JobDef) Configuration.registerScheduledJob()} is called; </li>
 *   <li>this calls {@link nl.nn.adapterframework.scheduler.SchedulerHelper#scheduleJob(IbisManager, JobDef) SchedulerHelper.scheduleJob()};</li>
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
 */
public class JobDef {
	protected Logger log=LogUtil.getLogger(this);
	protected Logger heartbeatLog = LogUtil.getLogger("HEARTBEAT");

	public static final String JOB_FUNCTION_STOP_ADAPTER="StopAdapter";	
	public static final String JOB_FUNCTION_START_ADAPTER="StartAdapter";	
	public static final String JOB_FUNCTION_STOP_RECEIVER="StopReceiver";	
	public static final String JOB_FUNCTION_START_RECEIVER="StartReceiver";	
	public static final String JOB_FUNCTION_SEND_MESSAGE="SendMessage";	
	public static final String JOB_FUNCTION_QUERY="ExecuteQuery";	
	public static final String JOB_FUNCTION_DUMPSTATS="dumpStatistics";	
	public static final String JOB_FUNCTION_DUMPSTATSFULL="dumpStatisticsFull";	
	public static final String JOB_FUNCTION_CLEANUPDB="cleanupDatabase";
	public static final String JOB_FUNCTION_CLEANUPFS="cleanupFileSystem";
	public static final String JOB_FUNCTION_RECOVER_ADAPTERS="recoverAdapters";

    private String name;
    private String cronExpression;
    private long interval = -1;
    private String function;
    private String configurationName;
    private String adapterName;
    private String description;
    private String receiverName;
	private String query;
	private String jmsRealm;
	private Locker locker=null;
	private int numThreads = 1;
	private int countThreads = 0;

	private MessageKeeper messageKeeper; //instantiated in configure()
	private int messageKeeperSize = 10; //default length

	private int transactionAttribute=TransactionDefinition.PROPAGATION_SUPPORTS;
	private int transactionTimeout=0;

	private TransactionDefinition txDef=null;
	private PlatformTransactionManager txManager;

	private String jobGroup = Scheduler.DEFAULT_GROUP;

	private List<DirectoryCleaner> directoryCleaners = new ArrayList<DirectoryCleaner>();

	private class MessageLogObject {
		private String jmsRealmName;
		private String tableName;
		private String expiryDateField;

		public MessageLogObject(String jmsRealmName, String tableName, String expiryDateField) {
			this.jmsRealmName = jmsRealmName;
			this.tableName = tableName;
			this.expiryDateField = expiryDateField;
		}

		public boolean equals(Object o) {
			MessageLogObject mlo = (MessageLogObject) o;
			if (mlo.getJmsRealmName().equals(jmsRealmName) &&
				mlo.getTableName().equals(tableName) &&
				mlo.expiryDateField.equals(expiryDateField)) {
				return true;
			} else {
				return false;
			}
		}

		public String getJmsRealmName() {
			return jmsRealmName;
		}

		public String getTableName() {
			return tableName;
		}

		public String getExpiryDateField() {
			return expiryDateField;
		}
	}
    
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public void configure(Configuration config) throws ConfigurationException {
		MessageKeeper messageKeeper = getMessageKeeper();

		if (StringUtils.isEmpty(getFunction())) {
			throw new ConfigurationException("jobdef ["+getName()+"] function must be specified");
		}
		if (!(getFunction().equalsIgnoreCase(JOB_FUNCTION_STOP_ADAPTER)||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_START_ADAPTER)||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_STOP_RECEIVER)||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_START_RECEIVER)||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_SEND_MESSAGE)||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_QUERY)||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_DUMPSTATS) ||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_DUMPSTATSFULL) ||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_CLEANUPDB) ||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_CLEANUPFS) ||
				getFunction().equalsIgnoreCase(JOB_FUNCTION_RECOVER_ADAPTERS)
			)) {
			throw new ConfigurationException("jobdef ["+getName()+"] function ["+getFunction()+"] must be one of ["+
			JOB_FUNCTION_STOP_ADAPTER+","+
			JOB_FUNCTION_START_ADAPTER+","+
			JOB_FUNCTION_STOP_RECEIVER+","+
			JOB_FUNCTION_START_RECEIVER+","+
			JOB_FUNCTION_SEND_MESSAGE+","+
			JOB_FUNCTION_QUERY+","+
			JOB_FUNCTION_DUMPSTATS+","+
			JOB_FUNCTION_DUMPSTATSFULL+","+
			JOB_FUNCTION_CLEANUPDB+","+
			JOB_FUNCTION_CLEANUPFS+
			JOB_FUNCTION_RECOVER_ADAPTERS+
			"]");
		}
		if (getFunction().equalsIgnoreCase(JOB_FUNCTION_DUMPSTATS)) {
			// nothing special for now
		} else 
		if (getFunction().equalsIgnoreCase(JOB_FUNCTION_DUMPSTATSFULL)) {
			// nothing special for now
		} else 
		if (getFunction().equalsIgnoreCase(JOB_FUNCTION_CLEANUPDB)) {
			// nothing special for now
		} else 
		if (getFunction().equalsIgnoreCase(JOB_FUNCTION_CLEANUPFS)) {
			// nothing special for now
		} else 
		if (getFunction().equalsIgnoreCase(JOB_FUNCTION_RECOVER_ADAPTERS)) {
			// nothing special for now
		} else 
		if (getFunction().equalsIgnoreCase(JOB_FUNCTION_QUERY)) {
			if (StringUtils.isEmpty(getJmsRealm())) {
				throw new ConfigurationException("jobdef ["+getName()+"] for function ["+getFunction()+"] a jmsRealm must be specified");
			}
		} else {
			if (StringUtils.isEmpty(getAdapterName())) {
				throw new ConfigurationException("jobdef ["+getName()+"] for function ["+getFunction()+"] a adapterName must be specified");
			}
			if (config.getRegisteredAdapter(getAdapterName()) == null) {
				String msg="Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] not registered.";
				throw new ConfigurationException(msg);
			}
			if (getFunction().equalsIgnoreCase(JOB_FUNCTION_STOP_RECEIVER) || getFunction().equalsIgnoreCase(JOB_FUNCTION_START_RECEIVER)) {
				if (StringUtils.isEmpty(getReceiverName())) {
					throw new ConfigurationException("jobdef ["+getName()+"] for function ["+getFunction()+"] a receiverName must be specified");
				}
				if (StringUtils.isNotEmpty(getReceiverName())){
					if (! config.isRegisteredReceiver(getAdapterName(), getReceiverName())) {
						String msg="Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] receiver ["+getReceiverName()+"] not registered.";
						throw new ConfigurationException(msg);
					}
				}
			}
		}
		if (getLocker()!=null) {
			getLocker().configure();
		}

		txDef = SpringTxManagerProxy.getTransactionDefinition(getTransactionAttributeNum(),getTransactionTimeout());

		messageKeeper.add("job successfully configured");
	}

	public JobDetail getJobDetail(IbisManager ibisManager) {
		JobDetail jobDetail = new JobDetail(getName(), getJobGroup(), ConfiguredJob.class);
		jobDetail.getJobDataMap().put("manager", ibisManager); // reference to manager.
		jobDetail.getJobDataMap().put("jobdef", this);
		if (StringUtils.isNotEmpty(getDescription())) {
			jobDetail.setDescription(getDescription());
		}
		return jobDetail;
	}

	protected void executeJob(IbisManager ibisManager) {
		if (incrementCountThreads()) { 
			try {
				IbisTransaction itx = null;
				TransactionStatus txStatus = null;
				if (getTxManager()!=null) {
					//txStatus = getTxManager().getTransaction(txDef);
					itx = new IbisTransaction(getTxManager(), txDef, "scheduled job ["+getName()+"]");
					txStatus = itx.getStatus();
				}
				try {
					if (getLocker()!=null) {
						String objectId = null;
						try {
							try {
								objectId = getLocker().lock();
							} catch (Exception e) {
								boolean isUniqueConstraintViolation = false;
								if (e instanceof SQLException) {
									SQLException sqle = (SQLException) e;
									isUniqueConstraintViolation = locker.getDbmsSupport().isUniqueConstraintViolation(sqle);
								}
								String msg = "error while setting lock: " + e.getMessage();
								if (isUniqueConstraintViolation) {
									getMessageKeeper().add(msg, MessageKeeperMessage.INFO_LEVEL);
									log.info(getLogPrefix()+msg);
								} else {
									getMessageKeeper().add(msg, MessageKeeperMessage.ERROR_LEVEL);
									log.error(getLogPrefix()+msg);
								}
							}
							if (objectId!=null) {
								TimeoutGuard tg = new TimeoutGuard("Job "+getName());
								try {
									tg.activateGuard(getTransactionTimeout());
									runJob(ibisManager);
								} finally {
									if (tg.cancel()) {
										log.error(getLogPrefix()+"thread has been interrupted");
									} 
								}
							}
						} finally {
							if (objectId!=null) {
								try {
									getLocker().unlock(objectId);
								} catch (Exception e) {
									String msg = "error while removing lock: " + e.getMessage();
									getMessageKeeper().add(msg, MessageKeeperMessage.WARN_LEVEL);
									log.warn(getLogPrefix()+msg);
								}
							}
						}
					} else {
						runJob(ibisManager);
					}
				} finally {
					if (txStatus!=null) {
						//getTxManager().commit(txStatus);
						itx.commit();
					}
				}
			} finally {
				decrementCountThreads();
			}
		} else {
			String msg = "maximum number of threads that may execute concurrently [" + getNumThreads() + "] is exceeded, the processing of this thread will be interrupted";
			getMessageKeeper().add(msg, MessageKeeperMessage.ERROR_LEVEL);
			log.error(getLogPrefix()+msg);
		}
	}

	public synchronized boolean incrementCountThreads() {
		if (countThreads < getNumThreads()) {
			countThreads++;
			return true;
		} else
		{
			return false;
		}
	}

	public synchronized void decrementCountThreads() {
		countThreads--;
	}

	protected void runJob(IbisManager ibisManager) {
		String function = getFunction();

		if (function.equalsIgnoreCase(JOB_FUNCTION_DUMPSTATS)) {
			ibisManager.dumpStatistics(HasStatistics.STATISTICS_ACTION_MARK_MAIN);
		} else 
		if (function.equalsIgnoreCase(JOB_FUNCTION_DUMPSTATSFULL)) {
			ibisManager.dumpStatistics(HasStatistics.STATISTICS_ACTION_MARK_FULL);
		} else 
		if (function.equalsIgnoreCase(JOB_FUNCTION_CLEANUPDB)) {
			cleanupDatabase(ibisManager);
		} else 
		if (function.equalsIgnoreCase(JOB_FUNCTION_CLEANUPFS)) {
			cleanupFileSystem(ibisManager);
		} else
		if (function.equalsIgnoreCase(JOB_FUNCTION_RECOVER_ADAPTERS)) {
			recoverAdapters(ibisManager);
		} else
		if (function.equalsIgnoreCase(JOB_FUNCTION_QUERY)) {
			executeQueryJob(ibisManager);
		} else
		if (function.equalsIgnoreCase(JOB_FUNCTION_SEND_MESSAGE)) {
			executeSendMessageJob();
		} else{
			ibisManager.handleAdapter(getFunction(), getConfigurationName(), getAdapterName(), getReceiverName(), "scheduled job ["+getName()+"]", true);
		}
	}

	private void cleanupDatabase(IbisManager ibisManager) {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = formatter.format(date);

		List<String> jmsRealmNames = new ArrayList<String>();

		for (Configuration configuration : ibisManager.getConfigurations()) {
			List<JobDef> scheduledJobs = configuration.getScheduledJobs();
			for (JobDef jobdef : configuration.getScheduledJobs()) {
				if (jobdef.getLocker()!=null) {
					String jmsRealmName = jobdef.getLocker().getJmsRealName();
					if (!jmsRealmNames.contains(jmsRealmName)) {
						jmsRealmNames.add(jmsRealmName);
					}
				}
			}
		}

		for (IAdapter adapter : ibisManager.getRegisteredAdapters()) {
			if (adapter instanceof Adapter) {
				PipeLine pipeLine = ((Adapter)adapter).getPipeLine();
				if (pipeLine != null) {
					for (IPipe pipe : pipeLine.getPipes()) {
						if (pipe instanceof IExtendedPipe) {
							IExtendedPipe extendedPipe = (IExtendedPipe)pipe;
							if (extendedPipe.getLocker() != null) {
								String jmsRealmName = extendedPipe.getLocker().getJmsRealName();
								if (!jmsRealmNames.contains(jmsRealmName)) {
									jmsRealmNames.add(jmsRealmName);
								}
							}
						}
					}
				}
			}
		}

		for (Iterator iter = jmsRealmNames.iterator(); iter.hasNext();) {
			String jmsRealmName = (String) iter.next();
			setJmsRealm(jmsRealmName);
			DirectQuerySender qs;
			qs = (DirectQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
			qs.setJmsRealm(jmsRealmName);
			String deleteQuery;
			if (qs.getDatabaseType() == DbmsSupportFactory.DBMS_MSSQLSERVER) {
				deleteQuery = "DELETE FROM IBISLOCK WHERE EXPIRYDATE < CONVERT(datetime, '" + formattedDate + "', 120)";
			} else {
				deleteQuery = "DELETE FROM IBISLOCK WHERE EXPIRYDATE < TO_TIMESTAMP('" + formattedDate + "', 'YYYY-MM-DD HH24:MI:SS')";
			}
			setQuery(deleteQuery);
			qs = null;
			executeQueryJob(ibisManager);
		}

		List messageLogs = new ArrayList();
		for(IAdapter iadapter : ibisManager.getRegisteredAdapters()) {
			Adapter adapter = (Adapter)iadapter;
			PipeLine pipeline = adapter.getPipeLine();
			for (int i=0; i<pipeline.getPipes().size(); i++) {
				IPipe pipe = pipeline.getPipe(i);
				if (pipe instanceof MessageSendingPipe) {
					MessageSendingPipe msp=(MessageSendingPipe)pipe;
					if (msp.getMessageLog()!=null) {
						ITransactionalStorage transactionStorage = msp.getMessageLog();
						if (transactionStorage instanceof JdbcTransactionalStorage) {
							JdbcTransactionalStorage messageLog = (JdbcTransactionalStorage)transactionStorage;
							String jmsRealmName = messageLog.getJmsRealName();
							String expiryDateField = messageLog.getExpiryDateField();
							String tableName = messageLog.getTableName();
							MessageLogObject mlo = new MessageLogObject(jmsRealmName, tableName, expiryDateField);
							if (!messageLogs.contains(mlo)) {
								messageLogs.add(mlo);
							}
						}
					}
				}
			}
		}

		for (Iterator iter = messageLogs.iterator(); iter.hasNext();) {
			MessageLogObject mlo = (MessageLogObject) iter.next();
			setJmsRealm(mlo.getJmsRealmName());
			DirectQuerySender qs;
			qs = (DirectQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
			qs.setJmsRealm(mlo.getJmsRealmName());
			String deleteQuery;
			if (qs.getDatabaseType() == DbmsSupportFactory.DBMS_MSSQLSERVER) {
				deleteQuery = "DELETE FROM " + mlo.getTableName() + " WHERE TYPE IN ('" + JdbcTransactionalStorage.TYPE_MESSAGELOG_PIPE + "','" + JdbcTransactionalStorage.TYPE_MESSAGELOG_RECEIVER + "') AND " + mlo.getExpiryDateField() + " < CONVERT(datetime, '" + formattedDate + "', 120)";
			} else {
				deleteQuery = "DELETE FROM " + mlo.getTableName() + " WHERE TYPE IN ('" + JdbcTransactionalStorage.TYPE_MESSAGELOG_PIPE + "','" + JdbcTransactionalStorage.TYPE_MESSAGELOG_RECEIVER + "') AND " + mlo.getExpiryDateField() + " < TO_TIMESTAMP('" + formattedDate + "', 'YYYY-MM-DD HH24:MI:SS')";
			}
			setQuery(deleteQuery);
			qs = null;
			executeQueryJob(ibisManager);
		}
	}

	private void cleanupFileSystem(IbisManager ibisManager) {
		for (Iterator it=directoryCleaners.iterator();it.hasNext();) {
			DirectoryCleaner directoryCleaner = (DirectoryCleaner)it.next();
			directoryCleaner.cleanup();
		}
	}

	private void executeQueryJob(IbisManager ibisManager) {
		DirectQuerySender qs;
		qs = (DirectQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
		try {
			qs.setName("QuerySender");
			qs.setJmsRealm(getJmsRealm());
			qs.setQueryType("other");
			qs.configure(true);
			qs.open();
			String result = qs.sendMessage("dummy", getQuery());
			log.info("result [" + result + "]");
		} catch (Exception e) {
			String msg = "error while executing query ["+getQuery()+"] (as part of scheduled job execution): " + e.getMessage();
			getMessageKeeper().add(msg,MessageKeeperMessage.ERROR_LEVEL);
			log.error(getLogPrefix()+msg);
		} finally {
			qs.close();
		}
	}

	private void executeSendMessageJob() {
		try {
			// send job
			IbisLocalSender localSender = new IbisLocalSender();
			localSender.setJavaListener(receiverName);
			localSender.setIsolated(false);
			localSender.setName("AdapterJob");
			localSender.configure();
            
			localSender.open();
			try {
				localSender.sendMessage(null, "");
			}
			finally {
				localSender.close();
			}
		}
		catch(Exception e) {
			String msg = "Error while sending message (as part of scheduled job execution): " + e.getMessage();
			getMessageKeeper().add(msg, MessageKeeperMessage.ERROR_LEVEL);
			log.error(getLogPrefix()+msg);
		}
	}

	private void recoverAdapters(IbisManager ibisManager) {
		int countAdapter=0;
		int countAdapterStateStarted=0;
		int countReceiver=0;
		int countReceiverStateStarted=0;
		for (IAdapter iAdapter : ibisManager.getRegisteredAdapters()) {
			countAdapter++;
			if (iAdapter instanceof Adapter) {
				Adapter adapter = (Adapter) iAdapter;
				RunStateEnum adapterRunState = adapter.getRunState();
				if (adapterRunState.equals(RunStateEnum.ERROR)) {
					log.debug("trying to recover adapter [" + adapter.getName()
							+ "]");
					try {
						adapter.setRecover(true);
						adapter.configure();
					} catch (ConfigurationException e) {
						// do nothing
						log.warn("error during recovering adapter ["
								+ adapter.getName() + "]: " + e.getMessage());
					} finally {
						adapter.setRecover(false);
					}
					if (adapter.configurationSucceeded()) {
						adapter.stopRunning();
						int count = 10;
						while (count-- >= 0
								&& !adapter.getRunState().equals(
										RunStateEnum.STOPPED)) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// do nothing
							}
						}
					}
					// check for start is in method startRunning in Adapter self
					if (adapter.isAutoStart()) {
						adapter.startRunning();
					}
					log.debug("finished recovering adapter ["
							+ adapter.getName() + "]");
				}
				String message = "adapter [" + adapter.getName()
						+ "] has state [" + adapterRunState + "]";
				adapterRunState = adapter.getRunState();
				if (adapterRunState.equals(RunStateEnum.STARTED)) {
					countAdapterStateStarted++;
					heartbeatLog.info(message);
				} else if (adapterRunState.equals(RunStateEnum.ERROR)) {
					heartbeatLog.error(message);
				} else {
					heartbeatLog.warn(message);
				}
				for (Iterator receiverIt = adapter.getReceiverIterator();
						receiverIt.hasNext();) {
					countReceiver++;
					IReceiver iReceiver = (IReceiver) receiverIt.next();
					if (iReceiver instanceof ReceiverBase) {
						ReceiverBase receiver = (ReceiverBase) iReceiver;
					
						RunStateEnum receiverRunState = receiver.getRunState();
						if (!adapterRunState.equals(RunStateEnum.ERROR)
								&& receiverRunState.equals(RunStateEnum.ERROR)) {
							log.debug("trying to recover receiver ["
									+ receiver.getName() + "] of adapter ["
									+ adapter.getName() + "]");
							try {
								if (receiver!=null) {
									receiver.setRecover(true);
								}
								adapter.configureReceiver(receiver);
							} finally {
								if (receiver!=null) {
									receiver.setRecover(false);
								}
							}
							if (receiver!=null) {
								if (receiver.configurationSucceeded()) {
									receiver.stopRunning();
									int count = 10;
									while (count-- >= 0
											&& !receiver.getRunState().equals(
													RunStateEnum.STOPPED)) {
										try {
											Thread.sleep(1000);
										} catch (InterruptedException e) {
											log.debug("Interrupted waiting for receiver to stop", e);
										}
									}
								}
								// check for start is in method startRunning in
								// ReceiverBase self
								receiver.startRunning();
								log.debug("finished recovering receiver ["
										+ receiver.getName() + "] of adapter ["
										+ adapter.getName() + "]");
							}
						}
						receiverRunState = receiver.getRunState();
						message = "receiver [" + receiver.getName()
								+ "] of adapter [" + adapter.getName()
								+ "] has state [" + receiverRunState + "]";
						if (receiverRunState.equals(RunStateEnum.STARTED)) {
							countReceiverStateStarted++;
							heartbeatLog.info(message);
						} else if (receiverRunState.equals(RunStateEnum.ERROR)) {
							heartbeatLog.error(message);
						} else {
							heartbeatLog.warn(message);
						}
					} else {
						log.warn("will not try to recover receiver ["
								+ iReceiver.getName() + "] of adapter ["
								+ adapter.getName()
								+ "], is not of type Receiver but ["
								+ iAdapter.getClass().getName() + "]");
					}
				}
			} else {
				log.warn("will not try to recover adapter ["
						+ iAdapter.getName()
						+ "], is not of type Adapter but ["
						+ iAdapter.getClass().getName() + "]");
			}
		}
		heartbeatLog.info("[" + countAdapterStateStarted + "/" + countAdapter
				+ "] adapters and [" + countReceiverStateStarted + "/"
				+ countReceiver + "] receivers have state ["
				+ RunStateEnum.STARTED + "]");
	}

	public String getLogPrefix() {
		return "Job ["+getName()+"] ";
	}

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

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public long getInterval() {
		return interval;
	}

	public void setFunction(String function) {
		this.function = function;
	}
	public String getFunction() {
		return function;
	}

	public void setConfigurationName(String configurationName) {
		this.configurationName = configurationName;
	}
	public String getConfigurationName() {
		return configurationName;
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

	public void setLocker(Locker locker) {
		this.locker = locker;
		locker.setName("Locker of job ["+getName()+"]");
	}
	public Locker getLocker() {
		return locker;
	}

	public void setTransactionAttribute(String attribute) throws ConfigurationException {
		transactionAttribute = JtaUtil.getTransactionAttributeNum(attribute);
		if (transactionAttribute<0) {
			String msg="illegal value for transactionAttribute ["+attribute+"]";
			messageKeeper.add(msg);
			throw new ConfigurationException(msg);
		}
	}
	public String getTransactionAttribute() {
		return JtaUtil.getTransactionAttributeString(transactionAttribute);
	}

	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
	public int getTransactionTimeout() {
		return transactionTimeout;
	}

	public void setTxManager(PlatformTransactionManager manager) {
		txManager = manager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	public void setNumThreads(int newNumThreads) {
		numThreads = newNumThreads;
	}
	public int getNumThreads() {
		return numThreads;
	}

	public synchronized MessageKeeper getMessageKeeper() {
		if (messageKeeper == null)
			messageKeeper = new MessageKeeper(messageKeeperSize < 1 ? 1 : messageKeeperSize);
		return messageKeeper;
	}

	public void setMessageKeeperSize(int size) {
		this.messageKeeperSize = size;
	}

	public void addDirectoryCleaner(DirectoryCleaner directoryCleaner) {
		directoryCleaners.add(directoryCleaner);
	}
}
