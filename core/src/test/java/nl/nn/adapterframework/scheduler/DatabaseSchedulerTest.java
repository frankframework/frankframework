package nl.nn.adapterframework.scheduler;

import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.logging.log4j.Logger;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.testutil.FixedQuerySenderMock;
import nl.nn.adapterframework.testutil.FixedQuerySenderMock.ResultSetBuilder;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeperMessage;

public class DatabaseSchedulerTest extends Mockito {

	protected Logger log = LogUtil.getLogger(this);
	private TestConfiguration configuration;
	private JobDef job;

	@Before
	public void setup() throws Exception {
		configuration = new TestConfiguration(true);
		configuration.getIbisManager(); //Sets a dummy IbisManager if non is found

		job = configuration.createBean(JobDef.class);
		job.setName("testJob");
		job.setFunction(JobDefFunctions.LOAD_DATABASE_SCHEDULES.getLabel());
		job.configure();
	}

	@After
	public void tearDown() {
		configuration.close();
		configuration = null; // <- force GC to cleanup!
	}

	@Test
	public void test() throws Exception {
		ResultSetBuilder builder = FixedQuerySenderMock.ResultSetBuilder.create();
		builder.setValue("JOBNAME", "dummy name");
		builder.setValue("JOBGROUP", "dummy group");
		builder.setValue("ADAPTER", "testAdapter");
		builder.setValue("RECEIVER", "testReceiver");
		builder.setValue("CRON", "");
		builder.setValue("EXECUTIONINTERVAL", "10");
		builder.setValue("MESSAGE", "dummy message");
		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("testAdapter");
		configuration.registerAdapter(adapter);

		FixedQuerySenderMock mock = new FixedQuerySenderMock(builder.build());
		configuration.mockCreateBean(FixedQuerySender.class, mock);

		job.runJob(configuration.getIbisManager());

		MessageKeeper messageKeeper = job.getMessageKeeper();
		for (int i = 0; i < messageKeeper.size(); i++) {
			MessageKeeperMessage message = messageKeeper.getMessage(i);
			if("ERROR".equals(message.getMessageLevel())) {
				assertThat(message.getMessageText(), CoreMatchers.containsString("adapter [testAdapter] receiver [testReceiver] not registered"));
			}
		}
	}
}
