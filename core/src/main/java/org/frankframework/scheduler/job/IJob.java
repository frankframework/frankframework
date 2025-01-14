/*
   Copyright 2021, 2024 WeAreFrank!

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
package org.frankframework.scheduler.job;

import org.quartz.JobDetail;

import org.frankframework.core.FrankElement;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.NameAware;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.scheduler.ConfiguredJob;
import org.frankframework.util.Locker;
import org.frankframework.util.MessageKeeper;

@FrankDocGroup(FrankDocGroupValue.JOB)
public interface IJob extends IConfigurable, FrankElement, NameAware {

	/**
	 * Actual implementation of the {@link IJob}. Is wrapped around a {@link Locker} and {@link MessageKeeper exceptions} will be managed automatically.
	 * @exception TimeoutException when the TransactionTimeout has been reached
	 * @exception JobExecutionException when the implementation fails to execute
	 */
	public void execute() throws JobExecutionException, TimeoutException;

	/**
	 * Triggers the Job at the specified number of milliseconds. Keep cronExpression empty in order to use interval.
	 * Value <code>0</code> may be used to run once at startup of the application.
	 * A value of 0 in combination with function 'sendMessage' will set dependencyTimeout on the IbisLocalSender to -1 to keep waiting indefinitely instead of 60 seconds for the adapter to start.
	 */
	public void setInterval(long interval);
	public long getInterval();

	/**
	 * CRON expression that determines the frequency of execution.
	 * Can <b>not</b> be used in combination with Interval.
	 */
	public void setCronExpression(String cronExpression);
	public String getCronExpression();

	public JobDetail getJobDetail();

	/** The name of the Job, used in combination with JobGroup to create a unique key. */
	@Override
	public String getName();

	/** The group of the Job, used in combination with Name to create a unique key. */
	public String getJobGroup();

	/** Only register (and trigger) Jobs that have been successfully configured. */
	public boolean isConfigured();

	public MessageKeeper getMessageKeeper();

	/**
	 * Optional element to avoid parallel execution of the Job, by multiple threads or servers. The Job is NOT executed when the lock cannot be obtained!
	 * In case another thread, potentially on another server, holds the lock and does not release it in a timely manner, it will not trigger the job.
	 */
	public void setLocker(Locker locker);
	public Locker getLocker();

	/** Called from {@link ConfiguredJob} which should trigger this job definition. */
	public void executeJob();
}
