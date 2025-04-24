package org.frankframework.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.Configuration;
import org.frankframework.scheduler.job.SendMessageJob;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.Locker;
import org.frankframework.util.SpringUtils;

public class IbisJobDetailTest {

	private SendMessageJob jobDef1;
	private SendMessageJob jobDef2;

	@BeforeEach
	public void setup() {
		Configuration configuration = new TestConfiguration();

		jobDef1 = SpringUtils.createBean(configuration);
		jobDef1.setName("fakeName");
		jobDef1.setJavaListener("fakeListener");
		configuration.addScheduledJob(jobDef1);

		jobDef2 = SpringUtils.createBean(configuration);
		jobDef2.setName("fakeName2");
		jobDef2.setJavaListener("fakeListener");
		configuration.addScheduledJob(jobDef2);
}

	@Test
	public void compareEmptyJobs() throws Exception {
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail) IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOneCron() throws Exception {
		jobDef1.setCronExpression("0 0 * ? * * *");
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOtherCron() throws Exception {
		jobDef2.setCronExpression("0 0 * ? * * *");
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareEqualCron() throws Exception {
		jobDef1.setCronExpression("0 0 * ? * * *");
		jobDef2.setCronExpression("0 0 * ? * * *");
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareDifferentCron() throws Exception {
		jobDef1.setCronExpression("0 0 * ? * * *");
		jobDef2.setCronExpression("1 1 * ? * * *");
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOneInterval() throws Exception {
		jobDef1.setInterval(100);
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOtherInterval() throws Exception {
		jobDef2.setInterval(100);
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareEqualInterval() throws Exception {
		jobDef1.setInterval(100);
		jobDef2.setInterval(100);
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareDifferentInterval() throws Exception {
		jobDef1.setInterval(100);
		jobDef2.setInterval(200);
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOneLocker() throws Exception {
		Locker locker = new Locker() {
			@Override
			public void configure() {
				// override configure, to avoid having to fully configure the Locker. We just use it's existence here.
			}
		};
		jobDef1.setLocker(locker);
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOtherLocker() throws Exception {
		Locker locker = new Locker() {
			@Override
			public void configure() {
				// override configure, to avoid having to fully configure the Locker. We just use it's existence here.
			}
		};
		jobDef2.setLocker(locker);
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareEqualLocker() throws Exception {
		Locker locker1 = new Locker() {
			@Override
			public void configure() {
				// override configure, to avoid having to fully configure the Locker. We just use it's objectId here.
			}

			@Override
			public String getObjectId() {
				return "fakeObjectId";
			}
		};
		jobDef1.setLocker(locker1);
		Locker locker2 = new Locker() {
			@Override
			public void configure() {
				// override configure, to avoid having to fully configure the Locker. We just use it's objectId here.
			}

			@Override
			public String getObjectId() {
				return "fakeObjectId";
			}
		};
		jobDef2.setLocker(locker2);
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareDifferentLocker() throws Exception {
		Locker locker1 = new Locker() {
			@Override
			public void configure() {
				// override configure, to avoid having to fully configure the Locker. We just use it's objectId here.
			}

			@Override
			public String getObjectId() {
				return "fakeObjectId 1";
			}
		};
		jobDef1.setLocker(locker1);
		Locker locker2 = new Locker() {
			@Override
			public void configure() {
				// override configure, to avoid having to fully configure the Locker. We just use it's objectId here.
			}

			@Override
			public String getObjectId() {
				return "fakeObjectId 2";
			}
		};
		jobDef2.setLocker(locker2);
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOneMessage() throws Exception {
		jobDef1.setMessage("fakeMessage");
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOtherMessage() throws Exception {
		jobDef2.setMessage("fakeMessage");
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareEqualMessage() throws Exception {
		jobDef1.setMessage("fakeMessage");
		jobDef2.setMessage("fakeMessage");
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareDifferentMessage() throws Exception {
		jobDef1.setMessage("fakeMessage 1");
		jobDef2.setMessage("fakeMessage 2");
		jobDef1.configure();
		jobDef2.configure();
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}


}
