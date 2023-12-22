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
package org.frankframework.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.quartz.JobBuilder.newJob;

import java.text.ParseException;

import org.frankframework.scheduler.job.IJob;
import org.frankframework.testutil.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public abstract class SchedulerTestBase {

	protected SchedulerHelper schedulerHelper;
	protected TestConfiguration configuration = new TestConfiguration();

	@BeforeEach
	public void setUp() throws SchedulerException {
		schedulerHelper = configuration.createBean(SchedulerHelper.class);
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
		jobDataMap.put(ConfiguredJob.JOBDEF_KEY, (IJob) null);

		return jobDataMap;
	}

	protected JobDetail createServiceJob(String name) throws SchedulerException, ParseException {
		return createServiceJob(name, SchedulerHelper.DEFAULT_GROUP);
	}

	protected JobDetail createServiceJob(String jobName, String groupName) {
		return newJob(ServiceJob.class)
				.withIdentity(jobName, groupName)
				.usingJobData(createServiceJobDataMap())
				.build();
	}

	protected JobDetail createConfiguredJob(String jobName) throws SchedulerException, ParseException {
		return createConfiguredJob(jobName, SchedulerHelper.DEFAULT_GROUP);
	}

	protected JobDetail createConfiguredJob(String jobName, String groupName) {
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

		assertEquals(1, job.getJobDataMap().size());
	}
}
