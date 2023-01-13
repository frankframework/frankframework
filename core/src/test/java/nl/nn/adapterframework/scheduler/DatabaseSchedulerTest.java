package nl.nn.adapterframework.scheduler;

import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.logging.log4j.Logger;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.scheduler.job.LoadDatabaseSchedulesJob;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.mock.FixedQuerySenderMock;
import nl.nn.adapterframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeperMessage;

public class DatabaseSchedulerTest extends Mockito {

	protected Logger log = LogUtil.getLogger(this);
	private TestConfiguration configuration;
	private LoadDatabaseSchedulesJob job;

	@BeforeEach
	public void setup() throws Exception {
		configuration = new TestConfiguration();
		configuration.getIbisManager(); //Sets a dummy IbisManager if non is found

		job = configuration.createBean(LoadDatabaseSchedulesJob.class);
		job.setName("testJob");
		job.configure();
	}

	@AfterEach
	public void tearDown() {
		configuration.close();
		configuration = null; // <- force GC to cleanup!
	}

	@Test
	public void executeJob() throws Exception {
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

		configuration.mockQuery("SELECT COUNT(*) FROM IBISSCHEDULES", builder.build());

		job.execute();

		MessageKeeper messageKeeper = job.getMessageKeeper();
		for (int i = 0; i < messageKeeper.size(); i++) {
			MessageKeeperMessage message = messageKeeper.getMessage(i);
			if("ERROR".equals(message.getMessageLevel())) {
				assertThat(message.getMessageText(), CoreMatchers.containsString("adapter [testAdapter] receiver [testReceiver] not registered"));
			}
		}
	}
}
