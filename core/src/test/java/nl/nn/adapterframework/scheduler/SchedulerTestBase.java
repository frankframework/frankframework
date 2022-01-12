/*
   Copyright 2019 Nationale-Nederlanden

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.quartz.JobBuilder.newJob;

import java.text.ParseException;

import nl.nn.adapterframework.configuration.IbisManager;

import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public abstract class SchedulerTestBase {

	protected SchedulerHelper schedulerHelper;

	@Before
	public void setUp() throws SchedulerException, ParseException {
		schedulerHelper = new SchedulerHelper();
		schedulerHelper.setScheduler(StdSchedulerFactory.getDefaultScheduler());
		schedulerHelper.getScheduler().clear();
	}

	private JobDataMap createServiceJobDataMap() {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put(ServiceJob.JAVALISTENER_KEY, "my-listener");
		jobDataMap.put(ServiceJob.CORRELATIONID_KEY, "super-correlation-id");
		jobDataMap.put(ServiceJob.MESSAGE_KEY, "my dummy message");

		return jobDataMap;
	}

	private JobDataMap createConfiguredJobDataMap() {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put(ConfiguredJob.JOBDEF_KEY, (JobDef) null);
		jobDataMap.put(ConfiguredJob.MANAGER_KEY, (IbisManager) null);

		return jobDataMap;
	}

	protected JobDetail createServiceJob(String name) throws SchedulerException, ParseException {
		return createServiceJob(name, SchedulerHelper.DEFAULT_GROUP);
	}

	protected JobDetail createServiceJob(String jobName, String groupName) throws SchedulerException, ParseException {
		return newJob(ServiceJob.class)
				.withIdentity(jobName, groupName)
				.usingJobData(createServiceJobDataMap())
				.build();
	}

	protected JobDetail createConfiguredJob(String jobName) throws SchedulerException, ParseException {
		return createConfiguredJob(jobName, SchedulerHelper.DEFAULT_GROUP);
	}

	protected JobDetail createConfiguredJob(String jobName, String groupName) throws SchedulerException, ParseException {
		return newJob(ConfiguredJob.class)
				.withIdentity(jobName, groupName)
				.usingJobData(createConfiguredJobDataMap())
				.build();
	}

	@Test
	public void testServiceJobDetail() throws SchedulerException, ParseException {
		JobDetail job = createServiceJob("test1");
		assertNotNull(job);

		JobKey details = job.getKey();
		assertEquals("test1", details.getName());
		assertEquals(SchedulerHelper.DEFAULT_GROUP, details.getGroup());

		assertEquals(3, job.getJobDataMap().size());
	}

	@Test
	public void testConfiguredJobDetail() throws SchedulerException, ParseException {
		JobDetail job = createConfiguredJob("test2");
		assertNotNull(job);

		JobKey details = job.getKey();
		assertEquals("test2", details.getName());
		assertEquals(SchedulerHelper.DEFAULT_GROUP, details.getGroup());

		assertEquals(2, job.getJobDataMap().size());
	}
}