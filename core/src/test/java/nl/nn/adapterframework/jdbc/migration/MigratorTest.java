package nl.nn.adapterframework.jdbc.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.MessageKeeper;

public class MigratorTest extends JdbcTestBase {
	private TestConfiguration configuration;
	private Migrator migrator = null;
	private IbisContext ibisContext = Mockito.spy(new IbisContext());

	private TestConfiguration getConfiguration() {
		if(configuration == null) {
			configuration = new TestConfiguration();
		}
		return configuration;
	}

	@After
	public void tearDown() {
		if(migrator != null) {
			migrator.close();
		}
	}

	@Override
	protected void prepareDatabase() throws Exception {
		//Ignore programmatic creation of Temp table, run Liquibase instead!
		if (dbmsSupport.isTablePresent(connection, "DUMMYTABLE")) {
			JdbcUtil.executeStatement(connection, "DROP TABLE DUMMYTABLE cascade constraints");
		}
		if (dbmsSupport.isTablePresent(connection, "DATABASECHANGELOG")) {
			JdbcUtil.executeStatement(connection, "DROP TABLE DATABASECHANGELOG");
		}

		migrator = getConfiguration().createBean(Migrator.class);
		migrator.setDatasourceName(getDataSourceName());
		migrator.setIbisContext(ibisContext);
	}

	private MessageKeeper getMessageKeeper() {
		return ibisContext.getMessageKeeper(TestConfiguration.TEST_CONFIGURATION_NAME);
	}

	@Test
	public void testSimpleChangelogFile() throws Exception {
		assertFalse("table [DUMMYTABLE] should not exist prior to the test", dbmsSupport.isTablePresent(connection, "DUMMYTABLE"));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
		migrator.configure();
		migrator.update();

		MessageKeeper messageKeeper = getMessageKeeper();
		assertNotNull("no message logged to the messageKeeper", messageKeeper);
		assertEquals(1, messageKeeper.size());
		assertEquals("Configuration [TestConfiguration] LiquiBase applied [2] change(s) and added tag [two:Niels Meijer]", messageKeeper.getMessage(0).getMessageText());
		assertFalse("table [DUMMYTABLE] should not exist", dbmsSupport.isTablePresent(connection, "DUMMYTABLE"));
	}

	@Test
	public void testFaultyChangelogFile() throws Exception {
		assertFalse("table [DUMMYTABLE] should not exist prior to the test", dbmsSupport.isTablePresent(connection, "DUMMYTABLE"));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelogError.xml");
		migrator.configure();
		migrator.update();

		ConfigurationWarnings warnings = configuration.getConfigurationWarnings();
		assertEquals(1, warnings.size());

		String warning = warnings.get(0);
		assertTrue(warning.contains("TestConfiguration [TestConfiguration] Error running LiquiBase update. Failed to execute [3] change(s)")); //Test ObjectName + Error
		assertTrue(warning.contains("Migration failed for change set /Migrator/DatabaseChangelogError.xml::error::Niels Meijer")); //Test liquibase exception
		//H2 logs 'Table \"DUMMYTABLE\" already exists' Oracle throws 'ORA-00955: name is already used by an existing object'
		assertTrue("table [DUMMYTABLE] should exist", dbmsSupport.isTablePresent(connection, "DUMMYTABLE"));
	}
}
