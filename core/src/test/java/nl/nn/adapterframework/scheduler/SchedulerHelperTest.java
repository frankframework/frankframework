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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class SchedulerHelperTest extends SchedulerTestBase {

	//Fill the scheduler with 4 entries, remove 1 and check which schedules are left.
	@Test
	public void testDeleteTrigger() throws SchedulerException, ParseException {
		schedulerHelper.scheduleJob(createServiceJob("target", null), 1000);
		schedulerHelper.scheduleJob(createServiceJob("target", "something"), 1000);

		schedulerHelper.deleteTrigger("target", "something");

		assertFalse(schedulerHelper.contains("target", "something"));
		assertTrue(schedulerHelper.contains("target", null));
	}

	@Test
	public void testNullIsDefaultGroup() throws SchedulerException, ParseException {
		schedulerHelper.scheduleJob(createServiceJob("target", null), 1000);

		//Make sure null, DEFAULT and SchedulerHelper.DEFAULT_GROUP are all the same
		assertTrue(schedulerHelper.contains("target", null));
		assertTrue(schedulerHelper.contains("target", "DEFAULT"));
		assertTrue(schedulerHelper.contains("target", SchedulerHelper.DEFAULT_GROUP));
	}

	@Test
	public void testContains() throws SchedulerException, ParseException {
		schedulerHelper.scheduleJob(createServiceJob("target", "some-group"), 1000);

		assertTrue(schedulerHelper.contains("target", "some-group"));
		//Test and make sure it doesn't also exist in the default group
		assertFalse(schedulerHelper.contains("target", SchedulerHelper.DEFAULT_GROUP));
	}

	@Test
	public void testGetJobDetail() throws SchedulerException, ParseException {
		schedulerHelper.scheduleJob(createServiceJob("testJob"), 1000);

		JobDetail result = schedulerHelper.getJobDetail("testJob");
		assertNotNull(result);
		//Validate it's got the correct jobDetail
		assertEquals("testJob", result.getKey().getName());
	}

	@Test
	public void testScheduleCronJob() throws SchedulerException, ParseException {
		JobDetail jobDetail = createServiceJob("testJob"+Math.random());

		schedulerHelper.scheduleJob(jobDetail, "0 0 1 * * ?");
		assertTrue(schedulerHelper.contains(jobDetail.getKey().getName()));
	}

	@Test
	public void testScheduleIntervalJob() throws SchedulerException, ParseException {
		JobDetail jobDetail = createServiceJob("testJob"+Math.random());

		schedulerHelper.scheduleJob(jobDetail, 0);
		assertTrue(schedulerHelper.contains(jobDetail.getKey().getName()));
	}

	@Test(expected = SchedulerException.class)
	public void addTwoTheSameJobs() throws SchedulerException, ParseException {
		JobDetail jobDetail = createServiceJob("testJob");

		schedulerHelper.scheduleJob(jobDetail, 0);
		assertTrue(schedulerHelper.contains(jobDetail.getKey().getName()));

		schedulerHelper.scheduleJob(jobDetail, 1);
	}

	@Test
	public void addTwoTheSameJobsWithOverride() throws SchedulerException, ParseException {
		JobDetail jobDetail = createServiceJob("testJob");
		String jobName = jobDetail.getKey().getName();

		schedulerHelper.scheduleJob(jobDetail, null, 3600*1000, false);
		assertTrue(schedulerHelper.contains(jobName));

		schedulerHelper.scheduleJob(jobDetail, null, 3600*1000, true);
		assertTrue(schedulerHelper.contains(jobName));

		Trigger trigger = schedulerHelper.getTrigger(jobName);
		assertNotNull("no trigger found", trigger);
	}

	@Test
	public void testScheduleJobWithoutSchedule() throws SchedulerException, ParseException {
		JobDetail jobDetail = createServiceJob("testJob"+Math.random());

		schedulerHelper.scheduleJob(jobDetail, null, -1, false);
		assertFalse(schedulerHelper.contains(jobDetail.getKey().getName()));
	}

	@Test(expected = SchedulerException.class)
	public void testScheduleDuplicateJob() throws SchedulerException, ParseException {
		JobDetail jobDetail = createServiceJob("testJob"+Math.random());

		schedulerHelper.scheduleJob(jobDetail, "0 0 1 * * ?");
		schedulerHelper.scheduleJob(jobDetail, 0);
	}

	@Test
	public void testStopScheduler() throws SchedulerException {
		schedulerHelper.startScheduler();
		//make sure the scheduler can start without any issues
		assertTrue(schedulerHelper.getScheduler().isStarted());
	}
}