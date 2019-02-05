package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.LinkedList;


import org.junit.Before;
import org.junit.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.HolidayCalendar;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;

public class SchedulerAdapterTest extends SchedulerTestBase {

	private static final String JAVALISTENER = "javaListener";
	private static final String CORRELATIONID = "correlationId";
	private static final String MESSAGE = "message";
	
	// @Mock
	private SchedulerAdapter schedulerAdapter;
	private Scheduler scheduler;
	
	@Before
	public void setup() throws SchedulerException {
		schedulerAdapter = new SchedulerAdapter();
		scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.clear();
	}
	
	@Test
	public void testGetJobGroupNamesWithJobsToXml() throws SchedulerException, ParseException {
		scheduleDummyTrigger("DummyJob", "DummyGroup A", 1);
		scheduleDummyTrigger("DummyJob", "DummyGroup B", 2);

		String result = schedulerAdapter.getJobGroupNamesWithJobsToXml(scheduler, null).toXML();
		assertTrue(result.contains("DummyGroup A") && result.contains("DummyGroup B"));
	}
	
	@Test
	public void testGetJobGroupNamesWithJobsToXmlWithMocks() throws SchedulerException, ParseException {
		scheduleDummyTrigger("DummyJob", "DummyGroup A", 1);
		scheduleDummyTrigger("DummyJob", "DummyGroup B", 2);
		
		IbisManager ibisManager = mock(IbisManager.class);
		when(ibisManager.getConfigurations()).thenReturn(new LinkedList<Configuration>());

		String result = schedulerAdapter.getJobGroupNamesWithJobsToXml(scheduler, ibisManager).toXML();
		assertTrue(result.contains("DummyGroup A") && result.contains("DummyGroup B"));
	}
	
	@Test
	public void testGetJobMessages() throws SchedulerException {
		assertEquals("<jobMessages />", schedulerAdapter.getJobMessages(null).toXML().trim());
	}
	
	@Test
	public void testJobDataMapToXmlBuilder() throws SchedulerException {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put(JAVALISTENER, "javaListener");
		jobDataMap.put(CORRELATIONID, "correlationId");
		jobDataMap.put(MESSAGE, "message");
		
		assertEquals("<jobMessages />", schedulerAdapter.getJobMessages(null).toXML().trim());
	}
	
	@Test
	public void testGetSchedulerCalendarNamesToXml() throws SchedulerException {
		scheduler.addCalendar("DummyCalendar A", new HolidayCalendar(), false, false);
		scheduler.addCalendar("DummyCalendar B", new HolidayCalendar(), false, false);
		
		String result = schedulerAdapter.getSchedulerCalendarNamesToXml(scheduler).toXML();
		assertTrue(result.contains("DummyCalendar A") && result.contains("DummyCalendar B"));
	}
	
	@Test
	public void testGetSchedulerMetaDataToXml() throws SchedulerException {
		assertTrue(schedulerAdapter.getSchedulerMetaDataToXml(scheduler).toXML().contains("<schedulerMetaData "));
	}
	
	private void scheduleDummyTrigger(String jobName, String groupName, int amtOfTriggers) throws SchedulerException, ParseException {
		JobDetail jobDetail = createDummyJob(jobName, groupName);
		
		for(int i = 0; i < amtOfTriggers; i++) {
			SimpleTrigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(jobName+" "+(i+1), groupName)
					.forJob(jobDetail)
					.withSchedule(SimpleScheduleBuilder.simpleSchedule()
							.withIntervalInSeconds(133780085)
							.repeatForever())
					.build();
			
			if(i == 0) {
				scheduler.scheduleJob(jobDetail, trigger);
			} else {
				scheduler.scheduleJob(trigger);
			}
		}
	}
	
	@Test
	public void testGetTriggerGroupNamesWithTriggersToXml() throws SchedulerException, ParseException {
		scheduleDummyTrigger("DummyJob", "DummyGroup A", 1);
		scheduleDummyTrigger("DummyJob", "DummyGroup B", 5);
		
		// TODO: Triggers aren't added to the XML's list of triggers (for reference, see SchedulerAdapter coverage)
		String result = schedulerAdapter.getTriggerGroupNamesWithTriggersToXml(scheduler).toXML();
		assertTrue(result.contains("DummyGroup A") && result.contains("DummyGroup B"));
	}
	
	@Test
	public void testTriggerToXmlBuilderWithCronSchedule() throws SchedulerException, ParseException {
		JobDetail jobDetail = createDummyJob();
		
		CronTrigger trigger = TriggerBuilder.newTrigger()
				.withIdentity("DummyJob", "DummyGroup")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 5 * * ?)"))
				.build();
		
		scheduler.scheduleJob(jobDetail, trigger);
		
		assertTrue(schedulerAdapter.triggerToXmlBuilder(scheduler, "DummyJob", "DummyGroup").toXML().contains("cronExpression="));
	}
	
//	@Test
//	public void testTriggerToXmlBuilderWithoutSchedule() throws SchedulerException, ParseException {
//		JobDetail jobDetail = createDummyJob();
//
//		Trigger trigger = TriggerBuilder.newTrigger()
//				.withIdentity("DummyJob", "DummyGroup")
//				.build();
//		
//		scheduler.scheduleJob(jobDetail, trigger);
//		
//		assertTrue(schedulerAdapter.triggerToXmlBuilder(scheduler, "DummyJob", "DummyGroup").toXML().contains("unknown"));
//	}
}