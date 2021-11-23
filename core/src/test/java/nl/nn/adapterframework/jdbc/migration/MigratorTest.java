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
	private TestConfiguration configuration;
	private Migrator migrator = null;
	private String tableName="DUMMYTABLE";
	private String rootLoggerName="nl.nn.adapterframework";
	private String liquibaseLoggerName="liquibase";

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
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
		try {
			migrator.configure();

			TestAppender.addToRootLogger(appender);
			migrator.update();

			String msg = "ChangeSet /Migrator/DatabaseChangelog.xml::two::Niels Meijer ran successfully in";
			assertTrue("Expecting message ["+msg+"] to be present as log line", appender.toString().contains(msg));
		} finally {
			TestAppender.removeAppender(appender);
		}
	}
	
	@Test
	public void testNoLogLinesWithErrorLogLevel() throws Exception {

		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
		try {
			migrator.configure();

			TestAppender.addToRootLogger(appender);
			Configurator.setLevel(rootLoggerName, Level.ERROR);
			Configurator.setLevel(liquibaseLoggerName, Level.ERROR);
			migrator.update();

			List<String> logLines = appender.getLogLines();
			assertTrue("log level is set to error. INFO log lines should not be present", logLines.size() == 0);
		} finally {
			TestAppender.removeAppender(appender);
			Configurator.setLevel(rootLoggerName, Level.DEBUG);
			Configurator.setLevel(liquibaseLoggerName, Level.INFO);
		}
	}
	
	@Test
	public void testChangingLogLevel() throws Exception {
		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelogError.xml");
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
		try {
			migrator.configure();

			TestAppender.addToRootLogger(appender);
			Configurator.setLevel(rootLoggerName, Level.ERROR);
			Configurator.setLevel(liquibaseLoggerName, Level.ERROR);
			migrator.update();

			List<LogEvent> logEvents = appender.getLogEvents();
			System.err.println("testChangingLogLevel ::::: "+ appender.toString() );
			assertTrue("Expected logEvent count is 1 but was:"+logEvents.size(), logEvents.size()==1);
			assertTrue("Expectd LogEvent level is ERROR but was:"+logEvents.get(0).getLevel(), logEvents.get(0).getLevel().equals(Level.ERROR) );
			String msg = "Change Set /Migrator/DatabaseChangelogError.xml::error::Niels Meijer failed.  Error:";
			assertTrue("Expected log message="+msg, appender.toString().contains(msg));
		} finally {
			TestAppender.removeAppender(appender);
			Configurator.setLevel(rootLoggerName, Level.DEBUG);
			Configurator.setLevel(liquibaseLoggerName, Level.INFO);
		}
	}
}
