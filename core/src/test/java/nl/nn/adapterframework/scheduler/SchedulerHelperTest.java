package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

public class SchedulerHelperTest extends SchedulerTestBase {
	
	private SchedulerHelper schedulerHelper;
	
	@Before
	public void setup() throws SchedulerException, ParseException {
		schedulerHelper = new SchedulerHelper();
		schedulerHelper.setScheduler(StdSchedulerFactory.getDefaultScheduler());
		schedulerHelper.getScheduler().clear();
	}
	
	@Test
	public void testDeleteTrigger() throws SchedulerException, ParseException {
		JobDetail jobDetail = createDummyJob();
		
		schedulerHelper.deleteTrigger(jobDetail.getKey().getName(), jobDetail.getKey().getGroup());
		assertTrue(schedulerHelper.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals("DummyGroup")).isEmpty());
	}
	
	@Test
	public void testGetJobForTrigger() throws SchedulerException, ParseException {
		JobDetail jobDetail = createDummyJob();
		SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger()
				.withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
				.forJob(jobDetail)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule()
						.withIntervalInSeconds(133780085)
						.repeatForever())
				.build();
		
		schedulerHelper.getScheduler().clear();
		schedulerHelper.getScheduler().scheduleJob(jobDetail, simpleTrigger);
		
		JobDetail result = schedulerHelper.getJobForTrigger(jobDetail.getKey().getName(), jobDetail.getKey().getGroup());
		assertNotNull(result);
	}
	
	@Test
	public void testScheduleJob() throws SchedulerException, ParseException {
		JobDetail jobDetail = createDummyJob();
		
		schedulerHelper.scheduleJob(jobDetail, "0 0 1 * * ?", -1, false);
		assertNotNull(schedulerHelper.getScheduler().getJobDetail(jobDetail.getKey()));
		
		schedulerHelper.scheduleJob(jobDetail, null, 0, true);
		assertNotNull(schedulerHelper.getScheduler().getJobDetail(jobDetail.getKey()));
	}
	
	@Test
	public void testScheduleJobWithoutSchedule() throws SchedulerException, ParseException {
		JobDetail jobDetail = createDummyJob();
		
		schedulerHelper.scheduleJob(jobDetail, null, -1, false);
		assertNull(schedulerHelper.getScheduler().getJobDetail(jobDetail.getKey()));
	}
	
	@Test(expected = SchedulerException.class)
	public void testScheduleDuplicateJob() throws SchedulerException, ParseException {
		JobDetail jobDetail = createDummyJob();
		
		schedulerHelper.scheduleJob(jobDetail, "0 0 1 * * ?", -1, false);
		schedulerHelper.scheduleJob(jobDetail, null, 0, false);
	}
	
	@Test
	public void testScheduleJobWithPredefinedInterval() throws SchedulerException, ParseException {
		JobDetail jobDetail = createDummyJob();
		
		schedulerHelper.scheduleJob(jobDetail, null, 133780085, false);
		assertNotNull(schedulerHelper.getScheduler().getJobDetail(jobDetail.getKey()));
	}

	@Test
	public void testStartScheduler() throws SchedulerException {
		schedulerHelper.startScheduler();
		assertTrue(schedulerHelper.getScheduler().isStarted());
	}
}