/*
   Copyright 2013, 2015, 2016, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.RestServiceDispatcher;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.scheduler.IbisJobDetail.JobType;
import nl.nn.adapterframework.senders.IbisLocalSender;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DirectoryCleaner;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

/**
 * Definition / configuration of scheduler jobs.
 * 
 * Specified in the Configuration.xml by a &lt;job&gt; inside a &lt;scheduler&gt;. The scheduler element must
 * be a direct child of configuration, not of adapter.
 * <tr><td>{@link #setNumThreads(int) numThreads}</td><td>the number of threads that may execute concurrently</td><td>1</td></tr>
 * <tr><td>{@link #setMessageKeeperSize(int) messageKeeperSize}</td><td>number of message displayed in IbisConsole</td><td>10</td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <tr><th>nested elements (accessible in descender-classes)</th><th>description</th></tr>
 * <tr><td>{@link Locker locker}</td><td>optional: the job will only be executed if a lock could be set successfully</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.util.DirectoryCleaner directoryCleaner}</td><td>optional: specification of the directories to clean when function is cleanupfilesystem</td></tr>
 * </table>
 * </p>
 * <p> 
 * <br>
 * Operation of scheduling:
 * <ul>
 *   <li>at configuration time {@link Configuration#registerScheduledJob(JobDef) Configuration.registerScheduledJob()} is called; </li>
 *   <li>this calls {@link SchedulerHelper#scheduleJob(IbisManager, JobDef) SchedulerHelper.scheduleJob()};</li>
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

	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = AppConstants.getInstance().getBoolean("configurations.autoDatabaseClassLoader", false);

    private String name;
    private String cronExpression;
    private long interval = -1;
    private JobDefFunctions function;
    private String configurationName;
    private String adapterName;
    private String description;
    private String receiverName;
	private String query;
	private int queryTimeout = 0;
	private String jmsRealm;
	private Locker locker=null;
	private int numThreads = 1;
	private int countThreads = 0;
	private String message = null;

	private MessageKeeper messageKeeper; //instantiated in configure()
	private int messageKeeperSize = 10; //default length

	private StatisticsKeeper statsKeeper;

	private int transactionAttribute=TransactionDefinition.PROPAGATION_SUPPORTS;
	private int transactionTimeout=0;

	private TransactionDefinition txDef=null;
	private PlatformTransactionManager txManager;

	private String jobGroup = null;

	private List<DirectoryCleaner> directoryCleaners = new ArrayList<DirectoryCleaner>();

	private class MessageLogObject {
		private String datasourceName;
		private String tableName;
		private String expiryDateField;
		private String keyField;
		private String typeField;

		public MessageLogObject(String datasourceName, String tableName, String expiryDateField, String keyField, String typeField) {
			this.datasourceName = datasourceName;
			this.tableName = tableName;
			this.expiryDateField = expiryDateField;
			this.keyField = keyField;
			this.typeField = typeField;
		}

		@Override
		public boolean equals(Object o) {
			if(o == null || !(o instanceof MessageLogObject)) return false;

			MessageLogObject mlo = (MessageLogObject) o;
			if (mlo.getDatasourceName().equals(datasourceName) &&
				mlo.getTableName().equals(tableName) &&
				mlo.expiryDateField.equals(expiryDateField)) {
				return true;
			} else {
				return false;
			}
		}

		public String getDatasourceName() {
			return datasourceName;
		}

		public String getTableName() {
			return tableName;
		}

		public String getExpiryDateField() {
			return expiryDateField;
		}

		public String getKeyField() {
			return keyField;
		}

		public String getTypeField() {
			return typeField;
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public void configure(Configuration config) throws ConfigurationException {
		MessageKeeper messageKeeper = getMessageKeeper();
		statsKeeper = new StatisticsKeeper(getName());

		if (StringUtils.isEmpty(getFunction())) {
			throw new ConfigurationException("jobdef ["+getName()+"] function must be specified");
		}

		if(config != null && StringUtils.isEmpty(getJobGroup())) //If not explicitly set, configure this JobDef under the config it's specified in
			setJobGroup(config.getName());

		if (function.equals(JobDefFunctions.QUERY)) {
			if (StringUtils.isEmpty(getJmsRealm())) {
				throw new ConfigurationException("jobdef ["+getName()+"] for function ["+getFunction()+"] a jmsRealm must be specified");
			}
		} else if(!function.isServiceJob()) {
			if (StringUtils.isEmpty(getAdapterName())) {
				throw new ConfigurationException("jobdef ["+getName()+"] for function ["+getFunction()+"] a adapterName must be specified");
			}
			if (config != null && config.getRegisteredAdapter(getAdapterName()) == null) {
				String msg="Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] not registered.";
				throw new ConfigurationException(msg);
			}
			if (function.isEqualToAtLeastOneOf(JobDefFunctions.STOP_RECEIVER, JobDefFunctions.START_RECEIVER)) {
				if (StringUtils.isEmpty(getReceiverName())) {
					throw new ConfigurationException("jobdef ["+getName()+"] for function ["+getFunction()+"] a receiverName must be specified");
				}
				if (config != null && StringUtils.isNotEmpty(getReceiverName())){
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

		JobDetail jobDetail = IbisJobBuilder.fromJobDef(this)
				.setIbisManager(ibisManager)
				.build();

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
									getMessageKeeper().add(msg, MessageKeeperLevel.INFO);
									log.info(getLogPrefix()+msg);
								} else {
									getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
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
									getMessageKeeper().add(msg, MessageKeeperLevel.WARN);
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
			getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
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
		long startTime = System.currentTimeMillis();

		switch(function) {
		case DUMPSTATS:
			ibisManager.dumpStatistics(HasStatistics.STATISTICS_ACTION_MARK_MAIN);
			break;
		case DUMPSTATSFULL:
			ibisManager.dumpStatistics(HasStatistics.STATISTICS_ACTION_MARK_FULL);
			break;
		case CLEANUPDB:
			cleanupDatabase(ibisManager);
			break;
		case CLEANUPFS:
			cleanupFileSystem(ibisManager);
			break;
		case RECOVER_ADAPTERS:
			recoverAdapters(ibisManager);
			break;
		case CHECK_RELOAD:
			checkReload(ibisManager);
			break;
		case QUERY:
			executeQueryJob(ibisManager);
			break;
		case SEND_MESSAGE:
			executeSendMessageJob(ibisManager);
			break;
		case LOAD_DATABASE_SCHEDULES:
			loadDatabaseSchedules(ibisManager);
			break;

		default:
			ibisManager.handleAdapter(getFunction(), getConfigurationName(), getAdapterName(), getReceiverName(), "scheduled job ["+getName()+"]", true);
			break;
		}

		long endTime = System.currentTimeMillis();
		statsKeeper.addValue(endTime - startTime);
	}

	/**
	 * Locate all Lockers, and find out which datasources are used.
	 * @return distinct list of all datasourceNames used by lockers
	 */
	private List<String> getAllLockerDatasourceNames(IbisManager ibisManager) {
		List<String> datasourceNames = new ArrayList<>();

		for (Configuration configuration : ibisManager.getConfigurations()) {
			for (JobDef jobdef : configuration.getScheduledJobs()) {
				if (jobdef.getLocker()!=null) {
					String datasourceName = jobdef.getLocker().getDatasourceName();
					if(StringUtils.isNotEmpty(datasourceName) && !datasourceNames.contains(datasourceName)) {
						datasourceNames.add(datasourceName);
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
								String datasourceName = extendedPipe.getLocker().getDatasourceName();
								if(StringUtils.isNotEmpty(datasourceName) && !datasourceNames.contains(datasourceName)) {
									datasourceNames.add(datasourceName);
								}
							}
						}
					}
				}
			}
		}

		return datasourceNames;
	}

	private List<MessageLogObject> getAllMessageLogsAndErrorstorages(IbisManager ibisManager) {
		List<MessageLogObject> messageLogs = new ArrayList<>();
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
							String datasourceName = messageLog.getDatasourceName();
							String expiryDateField = messageLog.getExpiryDateField();
							String tableName = messageLog.getTableName();
							String keyField = messageLog.getKeyField();
							String typeField = messageLog.getTypeField();
							MessageLogObject mlo = new MessageLogObject(datasourceName, tableName, expiryDateField, keyField, typeField);
							if (!messageLogs.contains(mlo)) {
								messageLogs.add(mlo);
							}
						}
					}
				}
			}
		}
		return messageLogs;
	}

	private void cleanupDatabase(IbisManager ibisManager) {
		Date date = new Date();

		List<String> datasourceNames = getAllLockerDatasourceNames(ibisManager);

		for (Iterator<String> iter = datasourceNames.iterator(); iter.hasNext();) {
			String datasourceName = iter.next();
			DirectQuerySender qs = null;
			String deleteQuery = null;
			try {
				qs = ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
				qs.setDatasourceName(datasourceName);
				qs.setName("executeQueryJob");
				qs.setQueryType("other");
				qs.setTimeout(getQueryTimeout());
				qs.configure(true);
				qs.open();

				deleteQuery = "DELETE FROM IBISLOCK WHERE EXPIRYDATE < "+qs.getDbmsSupport().getDatetimeLiteral(date);
				Message result = qs.sendMessage(new Message(deleteQuery), null);
				log.info("result [" + result + "]");
			} catch (Exception e) {
				String msg = "error while executing query ["+deleteQuery+"] (as part of scheduled job execution): " + e.getMessage();
				getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
				log.error(getLogPrefix()+msg);
			} finally {
				if(qs != null) {
					qs.close();
				}
			}
		}

		List<MessageLogObject> messageLogs = getAllMessageLogsAndErrorstorages(ibisManager);

		for (MessageLogObject mlo: messageLogs) {
			DirectQuerySender qs = null;
			String deleteQuery = null;
			try {
				qs = ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
				qs.setDatasourceName(mlo.getDatasourceName());
				qs.setName("executeQueryJob");
				qs.setQueryType("other");
				qs.setTimeout(getQueryTimeout());
				qs.configure(true);
				qs.open();

				if (qs.getDatabaseType() == Dbms.MSSQL) {
					deleteQuery = "DELETE FROM " + mlo.getTableName() + " WHERE " + mlo.getKeyField() + " IN (SELECT " + mlo.getKeyField() + " FROM " + mlo.getTableName()
							+ " WITH (readpast) WHERE " + mlo.getTypeField() + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
							+ "') AND " + mlo.getExpiryDateField() + " < "+qs.getDbmsSupport().getDatetimeLiteral(date)+")";
				}
				else {
					deleteQuery = "DELETE FROM " + mlo.getTableName()  + " WHERE " + mlo.getTypeField() + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode() + "') AND " + mlo.getExpiryDateField() + " < "+qs.getDbmsSupport().getDatetimeLiteral(date);
				}

				Message result = qs.sendMessage(new Message(deleteQuery), null);
				log.info("result [" + result + "]");
			} catch (Exception e) {
				String msg = "error while executing query ["+deleteQuery+"] (as part of scheduled job execution): " + e.getMessage();
				getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
				log.error(getLogPrefix()+msg);
			} finally {
				if(qs != null) {
					qs.close();
				}
			}
		}
	}

	private void cleanupFileSystem(IbisManager ibisManager) {
		for (DirectoryCleaner directoryCleaner: directoryCleaners) {
			directoryCleaner.cleanup();
		}
	}

	private void checkReload(IbisManager ibisManager) {
		if (ibisManager.getIbisContext().isLoadingConfigs()) {
			String msg = "skipping checkReload because one or more configurations are currently loading";
			getMessageKeeper().add(msg, MessageKeeperLevel.INFO);
			log.info(getLogPrefix() + msg);
			return;
		}
		String configJmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();

		if (StringUtils.isNotEmpty(configJmsRealm)) {
			List<String> configNames = new ArrayList<String>();
			List<String> configsToReload = new ArrayList<String>();

			Connection conn = null;
			ResultSet rs = null;
			FixedQuerySender qs = (FixedQuerySender) ibisManager.getIbisContext().createBeanAutowireByName(FixedQuerySender.class);
			qs.setJmsRealm(configJmsRealm);
			qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
			String booleanValueTrue = qs.getDbmsSupport().getBooleanValue(true);
			String selectQuery = "SELECT VERSION FROM IBISCONFIG WHERE NAME=? AND ACTIVECONFIG = '"+booleanValueTrue+"' and AUTORELOAD = '"+booleanValueTrue+"'";
			try {
				qs.configure();
				qs.open();
				conn = qs.getConnection();
				PreparedStatement stmt = conn.prepareStatement(selectQuery);
				for (Configuration configuration : ibisManager.getConfigurations()) {
					String configName = configuration.getName();
					configNames.add(configName);
					if ("DatabaseClassLoader".equals(configuration.getClassLoaderType())) {
						stmt.setString(1, configName);
						rs = stmt.executeQuery();
						if (rs.next()) {
							String ibisConfigVersion = rs.getString(1);
							String configVersion = configuration.getVersion(); //DatabaseClassLoader configurations always have a version
							if(StringUtils.isEmpty(configVersion) && configuration.getClassLoader() != null) { //If config hasn't loaded yet, don't skip it!
								log.warn(getLogPrefix()+"skipping autoreload for configuration ["+configName+"] unable to determine [configuration.version]");
							}
							else if (!StringUtils.equalsIgnoreCase(ibisConfigVersion, configVersion)) {
								log.info(getLogPrefix()+"configuration ["+configName+"] with version ["+configVersion+"] will be reloaded with new version ["+ibisConfigVersion+"]");
								configsToReload.add(configName);
							}
						}
					}
				}
			} catch (Exception e) {
				getMessageKeeper().add("error while executing query [" + selectQuery	+ "] (as part of scheduled job execution)", e);
			} finally {
				JdbcUtil.fullClose(conn, rs);
				qs.close();
			}

			if (!configsToReload.isEmpty()) {
				for (String configToReload : configsToReload) {
					ibisManager.getIbisContext().reload(configToReload);
				}
			}
			
			if (CONFIG_AUTO_DB_CLASSLOADER) {
				// load new (activated) configs
				List<String> dbConfigNames = null;
				try {
					dbConfigNames = ConfigurationUtils.retrieveConfigNamesFromDatabase(ibisManager.getIbisContext(), configJmsRealm, true);
				} catch (ConfigurationException e) {
					getMessageKeeper().add("error while retrieving configuration names from database", e);
				}
				if (dbConfigNames != null && !dbConfigNames.isEmpty()) {
					for (String currentDbConfigurationName : dbConfigNames) {
						if (!configNames.contains(currentDbConfigurationName)) {
							ibisManager.getIbisContext().load(currentDbConfigurationName);
						}
					}
				}
				// unload old (deactivated) configurations
				if (configNames != null && !configNames.isEmpty()) {
					for (String currentConfigurationName : configNames) {
						if (!dbConfigNames.contains(currentConfigurationName) && "DatabaseClassLoader".equals(ibisManager.getConfiguration(currentConfigurationName).getClassLoaderType())) {
							ibisManager.getIbisContext().unload(currentConfigurationName);
						}
					}
				}
			}
		}
	}

	/**
	 * 1. This method first stores all database jobs that can are found in the Quartz Scheduler in a Map.
	 * 2. It then loops through all records found in the database.
	 * 3. If the job is found, remove it from the Map and compares it with the already existing scheduled job. 
	 *    Only if they differ, it overwrites the current job.
	 *    If it is not present it add the job to the scheduler.
	 * 4. Once it's looped through all the database jobs, loop through the remaining jobs in the Map.
	 *    Since they have been removed from the database, remove them from the Quartz Scheduler
	 */
	private void loadDatabaseSchedules(IbisManager ibisManager) {
		if(!(ibisManager instanceof DefaultIbisManager)) {
			getMessageKeeper().add("manager is not an instance of DefaultIbisManager", MessageKeeperLevel.ERROR);
			return;
		}

		Map<JobKey, IbisJobDetail> databaseJobDetails = new HashMap<JobKey, IbisJobDetail>();
		Scheduler scheduler = null;
		SchedulerHelper sh = null;
		try {
			sh = ((DefaultIbisManager) ibisManager).getSchedulerHelper();
			scheduler = sh.getScheduler();

			// Fill the databaseJobDetails Map with all IbisJobDetails that have been stored in the database
			Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
			for(JobKey jobKey : jobKeys) {
				IbisJobDetail detail = (IbisJobDetail) scheduler.getJobDetail(jobKey);
				if(detail.getJobType() == JobType.DATABASE) {
					databaseJobDetails.put(detail.getKey(), detail);
				}
			}
		} catch (SchedulerException e) {
			getMessageKeeper().add("unable to retrieve jobkeys from scheduler", e);
		}

		// Get all IbisSchedules that have been stored in the database
		String configJmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
		FixedQuerySender qs = (FixedQuerySender) ibisManager.getIbisContext().createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(configJmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISSCHEDULES");

		Connection conn = null;
		ResultSet rs = null;
		try {
			qs.configure();
			qs.open();
			conn = qs.getConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT JOBNAME,JOBGROUP,ADAPTER,RECEIVER,CRON,EXECUTIONINTERVAL,MESSAGE,LOCKER,LOCK_KEY FROM IBISSCHEDULES");
			rs = stmt.executeQuery();

			while(rs.next()) {
				String jobName = rs.getString("JOBNAME");
				String jobGroup = rs.getString("JOBGROUP");
				String adapterName = rs.getString("ADAPTER");
				String receiverName = rs.getString("RECEIVER");
				String cronExpression = rs.getString("CRON");
				int interval = rs.getInt("EXECUTIONINTERVAL");
				String message = rs.getString("MESSAGE");
				boolean hasLocker = rs.getBoolean("LOCKER");
				String lockKey = rs.getString("LOCK_KEY");

				JobKey key = JobKey.jobKey(jobName, jobGroup);

				//Create a new JobDefinition so we can compare it with existing jobs
				DatabaseJobDef jobdef = new DatabaseJobDef();
				jobdef.setCronExpression(cronExpression);
				jobdef.setName(jobName);
				jobdef.setInterval(interval);
				jobdef.setJobGroup(jobGroup);
				jobdef.setAdapterName(adapterName);
				jobdef.setReceiverName(receiverName);
				jobdef.setMessage(message);

				if(hasLocker) {
					Locker locker = (Locker) ibisManager.getIbisContext().createBeanAutowireByName(Locker.class);
					locker.setName(lockKey);
					locker.setObjectId(lockKey);
					locker.setJmsRealm(configJmsRealm);
					jobdef.setLocker(locker);
				}

				try {
					jobdef.configure();
				} catch (ConfigurationException e) {
					getMessageKeeper().add("unable to configure DatabaseJobDef ["+jobdef+"] with key ["+key+"]", e);
				}

				// If the job is found, find out if it is different from the existing one and update if necessarily
				if(databaseJobDetails.containsKey(key)) {
					IbisJobDetail oldJobDetails = databaseJobDetails.get(key);
					if(!oldJobDetails.compareWith(jobdef)) {
						log.debug("updating DatabaseSchedule ["+key+"]");
						try {
							sh.scheduleJob(ibisManager, jobdef);
						} catch (SchedulerException e) {
							getMessageKeeper().add("unable to update schedule ["+key+"]", e);
						}
					}
					// Remove the key that has been found from the databaseJobDetails Map
					databaseJobDetails.remove(key);
				} else {
					// The job was not found in the databaseJobDetails Map, which indicates it's new and has to be added
					log.debug("add DatabaseSchedule ["+key+"]");
					try {
						sh.scheduleJob(ibisManager, jobdef);
					} catch (SchedulerException e) {
						getMessageKeeper().add("unable to add schedule ["+key+"]", e);
					}
				}
			}
		} catch (Exception e) { // Only catch database related exceptions!
			getMessageKeeper().add("unable to retrieve schedules from database", e);
		} finally {
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}

		// Loop through all remaining databaseJobDetails, which were not present in the database. Since they have been removed, unschedule them!
		for(JobKey key : databaseJobDetails.keySet()) {
			log.debug("delete DatabaseSchedule ["+key+"]");
			try {
				scheduler.deleteJob(key);
			} catch (SchedulerException e) {
				getMessageKeeper().add("unable to remove schedule ["+key+"]", e);
			}
		}
	}

	private void executeQueryJob(IbisManager ibisManager) {
		DirectQuerySender qs;
		qs = (DirectQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
		try {
			qs.setName("executeQueryJob");
			qs.setJmsRealm(getJmsRealm());
			qs.setQueryType("other");
			qs.setTimeout(getQueryTimeout());
			qs.configure(true);
			qs.open();
			Message result = qs.sendMessage(new Message(getQuery()), null);
			log.info("result [" + result + "]");
		} catch (Exception e) {
			String msg = "error while executing query ["+getQuery()+"] (as part of scheduled job execution): " + e.getMessage();
			getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
			log.error(getLogPrefix()+msg);
		} finally {
			qs.close();
		}
	}

	private void executeSendMessageJob(IbisManager ibisManager) {
		try {
			// send job
			IbisLocalSender localSender = new IbisLocalSender();
			localSender.setJavaListener(getReceiverName());
			localSender.setIsolated(false);
			localSender.setName("AdapterJob");
			if (getInterval() == 0) {
				localSender.setDependencyTimeOut(-1);
			}
			if (StringUtils.isNotEmpty(getAdapterName())) {
				IAdapter iAdapter = ibisManager.getRegisteredAdapter(getAdapterName());
				if (iAdapter == null) {
					log.warn("Cannot find adapter ["+getAdapterName()+"], cannot execute job");
					return;
				}
				Configuration configuration = iAdapter.getConfiguration();
				localSender.setConfiguration(configuration);
			}
			localSender.configure();
			localSender.open();
			try {
				//sendMessage message cannot be NULL
				Message message = new Message((getMessage()==null) ? "" : getMessage());
				localSender.sendMessage(message, null);
			}
			finally {
				localSender.close();
			}
		}
		catch(Exception e) {
			String msg = "error while sending message (as part of scheduled job execution): " + e.getMessage();
			getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
			log.error(getLogPrefix()+msg, e);
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
				for (Iterator<IReceiver> receiverIt = adapter.getReceiverIterator(); receiverIt.hasNext();) {
					IReceiver iReceiver = receiverIt.next();
					countReceiver++;

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
						} else if (receiverRunState
								.equals(RunStateEnum.STARTED)) {
							// workaround for started RestListeners of which
							// uriPattern is not registered correctly
							IListener listener = receiver.getListener();
							if (listener instanceof RestListener) {
								RestListener restListener = (RestListener) listener;
								String matchingPattern = RestServiceDispatcher
										.getInstance().findMatchingPattern("/"
												+ restListener.getUriPattern());
								if (matchingPattern == null) {
									log.debug("trying to recover receiver ["
											+ receiver.getName()
											+ "] (restListener) of adapter ["
											+ adapter.getName() + "]");
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
												+ receiver.getName() + "] (restListener) of adapter ["
												+ adapter.getName() + "]");
									}
								}
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

	@IbisDoc({"name of the job", ""})
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	
	@IbisDoc({"optional description of the job", ""})
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
	   return description;
	}

	@IbisDoc({"cron expression that determines the frequency of execution (see below)", ""})
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}
	public String getCronExpression() {
		return cronExpression;
	}

	@IbisDoc({"repeat the job at the specified number of ms. keep cronexpression empty to use interval. set to 0 to only run once at startup of the application. a value of 0 in combination with function 'sendmessage' will set dependencytimeout on the ibislocalsender to -1 the keep waiting indefinitely instead of max 60 seconds for the adapter to start.", ""})
	public void setInterval(long interval) {
		this.interval = interval;
	}

	public long getInterval() {
		return interval;
	}

	@IbisDoc({"one of: stopadapter, startadapter, stopreceiver, startreceiver, sendmessage, executequery, cleanupfilesystem", ""})
	public void setFunction(String function) throws ConfigurationException {
		try {
			this.function = JobDefFunctions.fromValue(function);
		}
		catch (IllegalArgumentException iae) {
			throw new ConfigurationException("jobdef ["+getName()+"] unknown function ["+function+"]. Must be one of "+ JobDefFunctions.getNames());
		}
	}
	public String getFunction() {
		return function==null?null:function.getName();
	}
	public JobDefFunctions getJobDefFunction() {
		return function;
	}

	@IbisDoc({"configuration on which job operates", ""})
	public void setConfigurationName(String configurationName) {
		this.configurationName = configurationName;
	}
	public String getConfigurationName() {
		return configurationName;
	}

	@IbisDoc({"adapter on which job operates", ""})
	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
	}
	public String getAdapterName() {
		return adapterName;
	}

	@IbisDoc({"receiver on which job operates. If function is 'sendmessage' this should be the name of the javalistener you wish to call", ""})
	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}
	public String getReceiverName() {
		return receiverName;
	}

	@IbisDoc({"the sql query text to be executed", ""})
	public void setQuery(String query) {
		this.query = query;
	}
	public String getQuery() {
		return query;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}

	@IbisDoc({"the number of seconds the driver will wait for a statement object to execute. if the limit is exceeded, a timeoutexception is thrown. 0 means no timeout", "0"})
	public void setQueryTimeout(int i) {
		queryTimeout = i;
	}

	@IbisDoc({"", " "})
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

	@IbisDoc({"The transactionAttribute declares transactional behavior of job execution. It "
			+ "applies both to database transactions and XA transactions. "
	        + "In general, a transactionAttribute is used to start a new transaction or suspend the current one when required. "
			+ "For developers: it is equal "
	        + "to <a href=\"http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494\">EJB transaction attribute</a>. "
	        + "Possible values for transactionAttribute: "
	        + "  <table border=\"1\">"
	        + "    <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>"
	        + "    <tr><td colspan=\"1\" rowspan=\"2\">Required</td>    <td>none</td><td>T2</td></tr>"
	        + "											      <tr><td>T1</td>  <td>T1</td></tr>"
	        + "    <tr><td colspan=\"1\" rowspan=\"2\">RequiresNew</td> <td>none</td><td>T2</td></tr>"
	        + "											      <tr><td>T1</td>  <td>T2</td></tr>"
	        + "    <tr><td colspan=\"1\" rowspan=\"2\">Mandatory</td>   <td>none</td><td>error</td></tr>"
	        + "											      <tr><td>T1</td>  <td>T1</td></tr>"
	        + "    <tr><td colspan=\"1\" rowspan=\"2\">NotSupported</td><td>none</td><td>none</td></tr>"
	        + "											      <tr><td>T1</td>  <td>none</td></tr>"
	        + "    <tr><td colspan=\"1\" rowspan=\"2\">Supports</td>    <td>none</td><td>none</td></tr>"
	        + " 										      <tr><td>T1</td>  <td>T1</td></tr>"
	        + "    <tr><td colspan=\"1\" rowspan=\"2\">Never</td>       <td>none</td><td>none</td></tr>"
	        + "											      <tr><td>T1</td>  <td>error</td></tr>"
	        + "  </table>", "Supports"})

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

    @IbisDoc({"Like <code>transactionAttribute</code>, but the chosen "
    	    + "option is represented with a number. The numbers mean:"
    	    + "<table>"
    	    + "<tr><td>0</td><td>Required</td></tr>"
    	    + "<tr><td>1</td><td>Supports</td></tr>"
    	    + "<tr><td>2</td><td>Mandatory</td></tr>"
    	    + "<tr><td>3</td><td>RequiresNew</td></tr>"
    	    + "<tr><td>4</td><td>NotSupported</td></tr>"
    	    + "<tr><td>5</td><td>Never</td></tr>"
    	    + "</table>", "1"})
	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

	@IbisDoc({"timeout (in seconds) of transaction started to process a message.", "<code>0</code> (use system default)"})
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

	@IbisDoc({"the number of threads that may execute concurrently", "1"})
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

	@IbisDoc({"number of message displayed in ibisconsole", "10"})
	public void setMessageKeeperSize(int size) {
		this.messageKeeperSize = size;
	}

	public synchronized StatisticsKeeper getStatisticsKeeper() {
		return statsKeeper;
	}
	
	public void addDirectoryCleaner(DirectoryCleaner directoryCleaner) {
		directoryCleaners.add(directoryCleaner);
	}

	@IbisDoc({"message to be send into the pipeline", ""})
	public void setMessage(String message) {
		if(StringUtils.isNotEmpty(message)) {
			this.message = message;
		}
	}
	public String getMessage() {
		return message;
	}
}
