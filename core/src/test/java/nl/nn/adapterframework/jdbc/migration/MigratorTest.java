package nl.nn.adapterframework.jdbc.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.MessageKeeper;

public class MigratorTest extends JdbcTestBase {
	private TestConfiguration configuration;
	private LiquibaseMigrator migrator = null;
	private String tableName="DUMMYTABLE";
//	private String rootLoggerName="nl.nn.adapterframework";
//	private String liquibaseLoggerName="liquibase";

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

		migrator = getConfiguration().createBean(LiquibaseMigrator.class);
		migrator.setDatasourceName(getDataSourceName());
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
		assertTrue(warning.contains("LiquibaseMigrator Error running LiquiBase update. Failed to execute [3] change(s)")); //Test ObjectName + Error
		assertTrue(warning.contains("Migration failed for change set Migrator/DatabaseChangelogError.xml::error::Niels Meijer")); //Test liquibase exception
		//H2 logs 'Table \"DUMMYTABLE\" already exists' Oracle throws 'ORA-00955: name is already used by an existing object'
		assertTrue("table ["+tableName+"] should exist", dbmsSupport.isTablePresent(connection, tableName));
	}

	@Test
	public void testSQLWriter() throws Exception {
		assumeTrue(getDataSourceName().equals("H2"));

		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
		URL resource = MigratorTest.class.getResource("/Migrator/DatabaseChangelog_plus_changes.xml");
		InputStream file = resource.openStream();
		String filename = "ChangedDatabaseChangelog.xml";

		migrator.configure(file, filename);
		StringWriter writer = new StringWriter();
		migrator.update(writer);

		String sqlChanges = TestFileUtils.getTestFile("/Migrator/sql_changes.sql");

		String result = applyIgnores(writer.toString());
		result = removeComments(result);
		TestAssertions.assertEqualsIgnoreCRLF(sqlChanges, result);
	}

	private String removeComments(String file) throws IOException {
		BufferedReader buf = new BufferedReader(new StringReader(file));

		StringBuilder string = new StringBuilder();
		String line = buf.readLine();
		while (line != null) {
			if(line.startsWith("--")) {
				line = buf.readLine();
				continue;
			}
			string.append(line);
			line = buf.readLine();
			if (line != null) {
				string.append("\n");
			}
		}
		return string.toString();
	}

	private String applyIgnores(String sqlScript) {
		Pattern regex = Pattern.compile("(\\d+)\\'\\);");
		Matcher match = regex.matcher(sqlScript);
		if(match.find()) {
			String deploymentId = match.group(1);
			sqlScript = sqlScript.replace(deploymentId, "IGNORE");
		}
		else {
			fail("no match found");
			return null;
		}

		return sqlScript.replaceAll("(LOCKEDBY = ')(.*)(WHERE)", "LOCKEDBY = 'IGNORE', LOCKGRANTED = 'IGNORE' WHERE");
	}

	//	@Test
//	public void testScriptExecutionLogs() throws Exception {
//
//		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));
//
//		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
//		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
//		try {
//			migrator.configure();
//
//			TestAppender.addToRootLogger(appender);
//			migrator.update();
//
//			String msg = "ChangeSet /Migrator/DatabaseChangelog.xml::two::Niels Meijer ran successfully in";
//			assertTrue("Expecting message ["+msg+"] to be present as log line", appender.toString().contains(msg));
//		} finally {
//			TestAppender.removeAppender(appender);
//		}
//	}
	
//	@Test
//	public void testNoLogLinesWithErrorLogLevel() throws Exception {
//
//		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));
//
//		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
//		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
//		try {
//			migrator.configure();
//
//			TestAppender.addToRootLogger(appender);
//			Configurator.setLevel(rootLoggerName, Level.ERROR);
//			Configurator.setLevel(liquibaseLoggerName, Level.ERROR);
//			migrator.update();
//
//			List<String> logLines = appender.getLogLines();
//			assertTrue("log level is set to error. INFO log lines should not be present", logLines.size() == 0);
//		} finally {
//			TestAppender.removeAppender(appender);
//			Configurator.setLevel(rootLoggerName, Level.DEBUG);
//			Configurator.setLevel(liquibaseLoggerName, Level.INFO);
//		}
//	}
	
//	@Test
//	public void testChangingLogLevel() throws Exception {
//		assertFalse("table ["+tableName+"] should not exist prior to the test", dbmsSupport.isTablePresent(connection, tableName));
//
//		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelogError.xml");
//		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
//		try {
//			migrator.configure();
//
//			TestAppender.addToRootLogger(appender);
//			Configurator.setLevel(rootLoggerName, Level.ERROR);
//			Configurator.setLevel(liquibaseLoggerName, Level.ERROR);
//			migrator.update();
//
//			List<LogEvent> logEvents = appender.getLogEvents();
//			System.err.println("testChangingLogLevel ::::: "+ appender.toString() );
//			assertTrue("Expected logEvent count is 1 but was:"+logEvents.size(), logEvents.size()==1);
//			assertTrue("Expectd LogEvent level is ERROR but was:"+logEvents.get(0).getLevel(), logEvents.get(0).getLevel().equals(Level.ERROR) );
//			String msg = "Change Set /Migrator/DatabaseChangelogError.xml::error::Niels Meijer failed.  Error:";
//			assertTrue("Expected log message="+msg, appender.toString().contains(msg));
//		} finally {
//			TestAppender.removeAppender(appender);
//			Configurator.setLevel(rootLoggerName, Level.DEBUG);
//			Configurator.setLevel(liquibaseLoggerName, Level.INFO);
//		}
//	}
}
