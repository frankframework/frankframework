package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.Locker;

public class IbisJobDetailTest {

	private JobDef jobDef1;
	private JobDef jobDef2;

	@Before
	public void setup() throws ConfigurationException {
		jobDef1 = new JobDef();
		jobDef1.setName("fakeName");
		jobDef1.setFunction("StopAdapter");
		jobDef1.setAdapterName("fakeAdapter");

		jobDef2 = new JobDef();
		jobDef2.setName("fakeName");
		jobDef2.setFunction("StopAdapter");
		jobDef2.setAdapterName("fakeAdapter");
}
	
	@Test
	public void compareEmptyJobs() throws Exception {
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOneCron() throws Exception {
		jobDef1.setCronExpression("0 0 *");
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareEqualCron() throws Exception {
		jobDef1.setCronExpression("0 0 *");
		jobDef2.setCronExpression("0 0 *");
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareDifferentCron() throws Exception {
		jobDef1.setCronExpression("0 0 *");
		jobDef2.setCronExpression("1 1 *");
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}
	
	@Test
	public void compareOneInterval() throws Exception {
		jobDef1.setInterval(100);
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareEqualInterval() throws Exception {
		jobDef1.setInterval(100);
		jobDef2.setInterval(100);
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareDifferentInterval() throws Exception {
		jobDef1.setInterval(100);
		jobDef2.setInterval(200);
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}
	
	@Test
	public void compareOneLocker() throws Exception {
		Locker locker1 = new Locker() {
			public void configure() {}
		};
		jobDef1.setLocker(locker1);
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareEqualLocker() throws Exception {
		Locker locker1 = new Locker() {
			public void configure() {}
			
			public String getObjectId() {
				return "fakeObjectId";
			}
		};
		jobDef1.setLocker(locker1);
		Locker locker2 = new Locker() {
			public void configure() {}

			public String getObjectId() {
				return "fakeObjectId";
			}
		};
		jobDef2.setLocker(locker2);
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareDifferentLocker() throws Exception {
		Locker locker1 = new Locker() {
			public void configure() {}
			
			public String getObjectId() {
				return "fakeObjectId 1";
			}
		};
		jobDef1.setLocker(locker1);
		Locker locker2 = new Locker() {
			public void configure() {}

			public String getObjectId() {
				return "fakeObjectId 2";
			}
		};
		jobDef2.setLocker(locker2);
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareOneMessage() throws Exception {
		jobDef1.setMessage("fakeMessage");
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareEqualMessage() throws Exception {
		jobDef1.setMessage("fakeMessage");
		jobDef2.setMessage("fakeMessage");
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertTrue(jobDetail1.compareWith(jobDef2));
	}

	@Test
	public void compareDifferentMessage() throws Exception {
		jobDef1.setMessage("fakeMessage 1");
		jobDef2.setMessage("fakeMessage 2");
		jobDef1.configure(null);
		jobDef2.configure(null);
		IbisJobDetail jobDetail1 = (IbisJobDetail)IbisJobBuilder.fromJobDef(jobDef1).build();
		assertFalse(jobDetail1.compareWith(jobDef2));
	}
	
	
}
