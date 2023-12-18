package org.frankframework.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.frankframework.jdbc.JdbcTestBase;
import org.frankframework.scheduler.job.CheckReloadJob;

public class CheckReloadJobTest extends JdbcTestBase {

	private CheckReloadJob jobDef;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		runMigrator("Migrator/Ibisconfig_4_unittests_changeset.xml");

		jobDef = new CheckReloadJob() {
			@Override
			protected String getDataSource() {
				return getDataSourceName();
			}
		};

		getConfiguration().getIbisManager(); //call once to ensure it exists.

		jobDef.setName("CheckReloadJob");
		getConfiguration().autowireByName(jobDef);
	}

	@Test
	public void testWithEmptyTable() throws Exception {
		jobDef.configure();

		jobDef.execute();

		assertEquals(1, jobDef.getMessageKeeper().size());
		assertTrue(jobDef.getMessageKeeper().getMessage(0).getMessageText().contains("job successfully configured"));
	}

	@Test
	public void testBeforeExecuteJobWithEmptyTable() throws Exception {
		jobDef.configure();

		assertFalse(jobDef.beforeExecuteJob());

		assertEquals(2, jobDef.getMessageKeeper().size());
		assertEquals("skipped job execution: no database configurations found", jobDef.getMessageKeeper().getMessage(1).getMessageText());
	}
}
