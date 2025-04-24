package org.frankframework.scheduler;

import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.logging.log4j.Logger;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.frankframework.core.Adapter;
import org.frankframework.scheduler.job.LoadDatabaseSchedulesJob;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.mock.FixedQuerySenderMock;
import org.frankframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper;
import org.frankframework.util.MessageKeeperMessage;

public class DatabaseSchedulerTest extends Mockito {

	protected Logger log = LogUtil.getLogger(this);
	private TestConfiguration configuration;
	private LoadDatabaseSchedulesJob job;

	@BeforeEach
	public void setup() throws Exception {
		configuration = new TestConfiguration();
		configuration.getIbisManager(); //Sets a dummy IbisManager if non is found

		job = configuration.createBean();
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
		Adapter adapter = configuration.createBean();
		adapter.setName("testAdapter");
		configuration.addAdapter(adapter);

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
