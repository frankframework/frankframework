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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.Message;

public class SchedulerSenderTest extends SchedulerTestBase {

	private SchedulerSender schedulerSender;
	private final String JOB_NAME = "senderName";

	@Override
	@BeforeEach
	public void setUp() throws SchedulerException {
		super.setUp();
		schedulerSender = configuration.createBean(SchedulerSender.class);
		schedulerSender.setName(JOB_NAME);
		schedulerSender.setSchedulerHelper(schedulerHelper);
	}

	@Test
	public void testConfigureWithJobNamePattern() throws ConfigurationException {
		schedulerSender.setJavaListener("dummyJavaListener");
		schedulerSender.setCronExpressionPattern("0 0 5 * * ?");
		schedulerSender.setJobNamePattern("DummyJobNamePattern");

		schedulerSender.configure();
		assertNotNull(schedulerSender.getParameterList().findParameter("_jobname"), "expected Parameter _jobname to be present");
	}

	@Test
	public void testConfigure() throws ConfigurationException, SenderException, SchedulerException {
		schedulerSender.setJavaListener("dummyJavaListener");
		schedulerSender.setCronExpressionPattern("0 0 5 * * ?");

		schedulerSender.configure();
		schedulerSender.sendMessage(new Message("message"), null);
		assertNull(schedulerSender.getParameterList().findParameter("_jobname"));

		assertTrue(schedulerHelper.contains(JOB_NAME));
	}

	@Test
	public void testJobGroup() throws ConfigurationException, SenderException, SchedulerException, IOException {
		schedulerSender.setJavaListener("dummyJavaListener");
		schedulerSender.setCronExpressionPattern("0 0 5 * * ?");
		schedulerSender.setJobGroup("test");

		assertFalse(schedulerHelper.contains(JOB_NAME));
		assertFalse(schedulerHelper.contains(JOB_NAME, "test"));

		schedulerSender.configure();
		Message name = schedulerSender.sendMessage(new Message("message"), null).getResult();
		assertEquals(JOB_NAME, name.asString());

		assertTrue(schedulerHelper.contains(JOB_NAME, "test"));
		assertFalse(schedulerHelper.contains(JOB_NAME));
	}

	@Test
	public void testConfigureWithoutJavaListener() throws ConfigurationException {
		schedulerSender.setCronExpressionPattern("0 0 5 * * ?");
		assertThrows(ConfigurationException.class, () -> schedulerSender.configure());
	}

	@Test
	public void testConfigureWithoutCronExpressionPattern() throws ConfigurationException {
		schedulerSender.setJavaListener("dummyJavaListener");
		assertThrows(ConfigurationException.class, () -> schedulerSender.configure());
	}
}
