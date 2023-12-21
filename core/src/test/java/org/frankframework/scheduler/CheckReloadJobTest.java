package org.frankframework.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.frankframework.scheduler.job.CheckReloadJob;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.junit.jupiter.api.BeforeEach;

@WithLiquibase
public class CheckReloadJobTest {

	private CheckReloadJob jobDef;

	@DatabaseTest.Parameter(0)
	private TransactionManagerType transactionManagerType;

	@DatabaseTest.Parameter(1)
	private String dataSourceName;

	private TestConfiguration getConfiguration() {
		return transactionManagerType.getConfigurationContext(dataSourceName);
	}

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) {

		jobDef = new CheckReloadJob() {
			@Override
			protected String getDataSource() {
				return dataSourceName;
			}
		};

		getConfiguration().getIbisManager(); //call once to ensure it exists.

		jobDef.setName("CheckReloadJob");
		getConfiguration().autowireByName(jobDef);
	}

	@DatabaseTest
	public void testWithEmptyTable() throws Exception {
		jobDef.configure();

		jobDef.execute();

		assertTrue(jobDef.getMessageKeeper().getMessage(0).getMessageText().contains("job successfully configured"));
	}

	@DatabaseTest
	public void testBeforeExecuteJobWithEmptyTable() throws Exception {
		jobDef.configure();

		assertFalse(jobDef.beforeExecuteJob());

		assertEquals(2, jobDef.getMessageKeeper().size());
		assertEquals("skipped job execution: no database configurations found", jobDef.getMessageKeeper().getMessage(1).getMessageText());
	}
}
