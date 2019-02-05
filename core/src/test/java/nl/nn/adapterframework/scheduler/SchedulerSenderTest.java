package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

public class SchedulerSenderTest extends SchedulerTestBase {
	
	private SchedulerSender schedulerSender;
	
	@Before
	public void setup() {
		schedulerSender = new SchedulerSender();
	}
	
	@Test
	public void testConfigure() throws ConfigurationException {
		schedulerSender.setJavaListener(JAVALISTENER);
		schedulerSender.setCronExpressionPattern("0 0 5 * * ?");
		schedulerSender.setJobNamePattern("DummyJobNamePattern");
		
		schedulerSender.configure();
		assertNotNull(schedulerSender.getParameterList().findParameter("_jobname"));
	}
	
	@Test
	public void testConfigureWithoutJobNamePattern() throws ConfigurationException {
		schedulerSender.setJavaListener(JAVALISTENER);
		schedulerSender.setCronExpressionPattern("0 0 5 * * ?");
		
		schedulerSender.configure();
		assertNull(schedulerSender.getParameterList().findParameter("_jobname"));
	}
	
	@Test(expected = ConfigurationException.class)
	public void testConfigureWithoutJavaListener() throws ConfigurationException {
		schedulerSender.setCronExpressionPattern("0 0 5 * * ?");
		schedulerSender.configure();
	}
	
	@Test(expected = ConfigurationException.class)
	public void testConfigureWithoutCronExpressionPattern() throws ConfigurationException {
		schedulerSender.setJavaListener(JAVALISTENER);
		schedulerSender.configure();
	}
	
	@Test
	public void testOpen() throws SenderException, SchedulerException {
		SchedulerHelper schedulerHelper;
		schedulerSender.setSchedulerHelper(schedulerHelper = new SchedulerHelper());
		schedulerHelper.setScheduler(StdSchedulerFactory.getDefaultScheduler());
		
		schedulerSender.open();
		assertTrue(schedulerSender.getSchedulerHelper().getScheduler().isStarted());
	}
	
	@Test
	public void testSendMessage() throws SenderException, TimeOutException, ConfigurationException, SchedulerException {
		ParameterResolutionContext prc = new ParameterResolutionContext();
		
		SchedulerHelper schedulerHelper = new SchedulerHelper();
		schedulerHelper.setScheduler(StdSchedulerFactory.getDefaultScheduler());
		schedulerSender.setSchedulerHelper(schedulerHelper);

		schedulerSender.setName("DummySender");
		schedulerSender.setJavaListener(JAVALISTENER);
		schedulerSender.setCronExpressionPattern("0 0 5 * * ?");
		schedulerSender.setJobNamePattern("DummyJobNamePattern");
		schedulerSender.configure();
		
		assertNotNull(schedulerSender.sendMessage(CORRELATIONID, MESSAGE, prc));
	}
	
	@Test
	public void testSchedule() throws Exception {
		SchedulerHelper schedulerHelper;
		schedulerSender.setSchedulerHelper(schedulerHelper = new SchedulerHelper());
		schedulerHelper.setScheduler(StdSchedulerFactory.getDefaultScheduler());
		
		schedulerSender.schedule("TestScheduleJob", "0 0 5 * * ?", CORRELATIONID, MESSAGE);
		assertNotNull(schedulerSender.getSchedulerHelper().getTrigger("TestScheduleJob"));
	}
}