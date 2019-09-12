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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.LinkedList;


import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.SchedulerException;
import org.quartz.impl.calendar.HolidayCalendar;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;

public class SchedulerAdapterTest extends SchedulerTestBase {

	private static final String JAVALISTENER = "javaListener";
	private static final String CORRELATIONID = "correlationId";
	private static final String MESSAGE = "message";

	private SchedulerAdapter schedulerAdapter;

	@Before
	public void setUp() throws SchedulerException, ParseException {
		super.setUp();
		schedulerAdapter = new SchedulerAdapter();
	}

	@Test
	public void testGetJobGroupNamesWithJobsToXml() throws SchedulerException, ParseException {
		scheduleDummyTrigger("DummyJob", "DummyGroup A");
		scheduleDummyTrigger("DummyJob", "DummyGroup B");

		String result = schedulerAdapter.getJobGroupNamesWithJobsToXml(schedulerHelper.getScheduler(), null).toXML();
		assertTrue(result.contains("DummyGroup A") && result.contains("DummyGroup B"));
	}

	@Test
	public void testGetJobGroupNamesWithJobsToXmlWithMocks() throws SchedulerException, ParseException {
		scheduleDummyTrigger("DummyJob", "DummyGroup A");
		scheduleDummyTrigger("DummyJob", "DummyGroup B");

		IbisManager ibisManager = mock(IbisManager.class);
		when(ibisManager.getConfigurations()).thenReturn(new LinkedList<Configuration>());

		String result = schedulerAdapter.getJobGroupNamesWithJobsToXml(schedulerHelper.getScheduler(), ibisManager).toXML();
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
		schedulerHelper.getScheduler().addCalendar("DummyCalendar A", new HolidayCalendar(), false, false);
		schedulerHelper.getScheduler().addCalendar("DummyCalendar B", new HolidayCalendar(), false, false);

		String result = schedulerAdapter.getSchedulerCalendarNamesToXml(schedulerHelper.getScheduler()).toXML();
		assertTrue(result.contains("DummyCalendar A") && result.contains("DummyCalendar B"));
	}

	@Test
	public void testGetSchedulerMetaDataToXml() throws SchedulerException {
		assertTrue(schedulerAdapter.getSchedulerMetaDataToXml(schedulerHelper.getScheduler()).toXML().contains("<schedulerMetaData "));
	}

	private void scheduleDummyTrigger(String jobName, String groupName) throws SchedulerException, ParseException {
		schedulerHelper.scheduleJob(createServiceJob(jobName, groupName), "0 0 5 * * ?)");
	}

	@Test
	public void testGetTriggerGroupNamesWithTriggersToXml() throws SchedulerException, ParseException {
		scheduleDummyTrigger("DummyJob", "DummyGroup A");
		scheduleDummyTrigger("DummyJob", "DummyGroup B");

		// TODO: Triggers aren't added to the XML's list of triggers (for reference, see SchedulerAdapter coverage)
		String result = schedulerAdapter.getTriggerGroupNamesWithTriggersToXml(schedulerHelper.getScheduler()).toXML();
		assertTrue(result.contains("DummyGroup A") && result.contains("DummyGroup B"));
	}

	@Test
	public void testTriggerToXmlBuilderWithCronSchedule() throws SchedulerException, ParseException {
		schedulerHelper.scheduleJob(createServiceJob("DummyJob", "DummyGroup"), "0 0 5 * * ?)");

		assertTrue(schedulerAdapter.triggerToXmlBuilder(schedulerHelper.getScheduler(), "DummyJob", "DummyGroup")
				.toXML().contains("cronExpression="));
	}
}