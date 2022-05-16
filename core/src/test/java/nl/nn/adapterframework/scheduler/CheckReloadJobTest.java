package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.scheduler.job.CheckReloadJob;

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

		jobDef.setName("CheckReloadJob");
		getConfiguration().autowireByName(jobDef);
	}

	@Test
	public void testWithEmptyTable() throws Exception {
		jobDef.configure();

		jobDef.execute(getConfiguration().getIbisManager());

		assertEquals(1, jobDef.getMessageKeeper().size());
		assertTrue(jobDef.getMessageKeeper().getMessage(0).getMessageText().contains("job successfully configured"));
	}
}
