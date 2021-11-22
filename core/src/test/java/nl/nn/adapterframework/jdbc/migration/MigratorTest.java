package nl.nn.adapterframework.jdbc.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.testutil.TestAppender;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.MessageKeeper;

public class MigratorTest extends JdbcTestBase {
	TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
	private static final String loggerName = "liquibase";
	private TestConfiguration configuration;
	private Migrator migrator = null;
	private String tableName="DUMMYTABLE";

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
		if (dbmsSupport.isTablePresent(connection, tableName)) {
			JdbcUtil.executeStatement(connection, "DROP TABLE "+tableName);
		}
		if (dbmsSupport.isTablePresent(connection, "DATABASECHANGELOG")) {
			JdbcUtil.executeStatement(connection, "DROP TABLE DATABASECHANGELOG");
		}

		migrator = getConfiguration().createBean(Migrator.class);
		AppConstants.getInstance().setProperty("jdbc.migrator.dataSource", getDataSourceName());
	}

	@Test
	public void testSimpleChangelogFile() throws Exception {
		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
		migrator.configure();
		migrator.update();

		MessageKeeper messageKeeper = configuration.getMessageKeeper();
		assertNotNull("no message logged to the messageKeeper", messageKeeper);
		assertEquals(2, messageKeeper.size()); //Configuration startup message + liquibase update
		assertEquals("Configuration [TestConfiguration] LiquiBase applied [2] change(s) and added tag [two:Niels Meijer]", messageKeeper.getMessage(1).getMessageText());
		assertFalse("table ["+tableName+"] should not exist", dbmsSupport.isTablePresent(connection, tableName));
	}

	@Test
	public void testFaultyChangelogFile() throws Exception {
		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelogError.xml");
		migrator.configure();
		migrator.update();

		ConfigurationWarnings warnings = configuration.getConfigurationWarnings();
		assertEquals(1, warnings.size());

		String warning = warnings.get(0);
		assertTrue(warning.contains("TestConfiguration [TestConfiguration] Error running LiquiBase update. Failed to execute [3] change(s)")); //Test ObjectName + Error
		assertTrue(warning.contains("Migration failed for change set /Migrator/DatabaseChangelogError.xml::error::Niels Meijer")); //Test liquibase exception
		//H2 logs 'Table \"DUMMYTABLE\" already exists' Oracle throws 'ORA-00955: name is already used by an existing object'
		assertTrue("table ["+tableName+"] should exist", dbmsSupport.isTablePresent(connection, tableName));
	}
	
	@Test
	public void testScriptExecutionLogs() throws Exception {

		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");

		try {
			migrator.configure();

			TestAppender.addToLogger(loggerName, appender);
			migrator.update();

			List<String> logLines = appender.getLogLines();

			boolean flag = false;
			for (String line : logLines) {
				if(line.contains("ChangeSet /Migrator/DatabaseChangelog.xml::two::Niels Meijer ran successfully in")) {
					flag=true;
				}
			}
			assertTrue(flag);
		} finally {
			TestAppender.removeAppenderFrom(loggerName, appender);
		}
	}
	
	@Test
	public void testChangingLogLevel() throws Exception {
		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelogError.xml");

		try {
			migrator.configure();

			TestAppender.addToLogger(loggerName, appender);
			Configurator.setLevel(loggerName, Level.ERROR);
			migrator.update();

			List<LogEvent> logEvents = appender.getLogEvents();
			assertTrue(logEvents.size()==1);
			assertTrue(logEvents.get(0).getLevel().equals(Level.ERROR));
			assertTrue(logEvents.get(0).getMessage().toString().contains("Change Set /Migrator/DatabaseChangelogError.xml::error::Niels Meijer failed.  Error:"));
		} finally {
			TestAppender.removeAppenderFrom(loggerName, appender);
			Configurator.setLevel(loggerName, Level.DEBUG);
		}
	}
}
