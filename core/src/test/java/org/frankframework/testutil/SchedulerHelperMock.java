package org.frankframework.testutil;

import org.frankframework.scheduler.SchedulerHelper;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

public class SchedulerHelperMock extends SchedulerHelper {

	public SchedulerHelperMock() {
		setScheduler(Mockito.mock(Scheduler.class));
	}

	@Override
	public void startScheduler() throws SchedulerException {
		//STUB method
	}
}
