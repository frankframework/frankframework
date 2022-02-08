package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.scheduler.job.CheckReloadJob;
import nl.nn.adapterframework.testutil.TestConfiguration;

public class CheckReloadJobTest extends JdbcTestBase {

	private CheckReloadJob jobDef;
	private TestConfiguration configuration;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		runMigrator("Migrator/Ibisconfig_4_unittests_changeset.xml");

		configuration = new TestConfiguration();

		jobDef = new CheckReloadJob() {
			@Override
			protected String getDataSource() {
				return getDataSourceName();
			}
		};
		jobDef.setName("CheckReloadJob");
		configuration.autowireByName(jobDef);
		configuration.configure();
	}

	@Test
	public void testWithEmptyTable() throws Exception {
		jobDef.configure();

		jobDef.execute(configuration.getIbisManager());

		assertEquals(2, jobDef.getMessageKeeper().size());
		assertTrue(jobDef.getMessageKeeper().getMessage(0).getMessageText().contains("job successfully configured"));
		assertTrue(jobDef.getMessageKeeper().getMessage(1).getMessageText().contains("No database configuration found to reload"));

	}

}
