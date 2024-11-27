package org.frankframework.testutil;

import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import org.frankframework.scheduler.SchedulerHelper;

public class SchedulerHelperMock extends SchedulerHelper {

	public SchedulerHelperMock() {
		setScheduler(Mockito.mock(Scheduler.class));
	}

	@Override
	public void startScheduler() throws SchedulerException {
		//STUB method
	}
}
