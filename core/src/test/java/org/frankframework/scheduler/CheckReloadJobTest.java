package org.frankframework.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;

import org.frankframework.scheduler.job.CheckReloadJob;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;

@WithLiquibase
public class CheckReloadJobTest {

	private CheckReloadJob jobDef;

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) {

		jobDef = new CheckReloadJob() {
			@Override
			protected String getDataSource() {
				return databaseTestEnvironment.getDataSourceName();
			}
		};

		// Ensure we have an IbisManager via side effects of method
		//noinspection ResultOfMethodCallIgnored
		databaseTestEnvironment.getConfiguration().getIbisManager();

		jobDef.setName("CheckReloadJob");
		databaseTestEnvironment.autowire(jobDef);
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
