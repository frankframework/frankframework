package nl.nn.adapterframework.testutil;

import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import nl.nn.adapterframework.scheduler.SchedulerHelper;

public class SchedulerHelperMock extends SchedulerHelper {

	public SchedulerHelperMock() {
		setScheduler(Mockito.mock(Scheduler.class));
	}

	@Override
	public void startScheduler() throws SchedulerException {
		//STUB method
	}
}
