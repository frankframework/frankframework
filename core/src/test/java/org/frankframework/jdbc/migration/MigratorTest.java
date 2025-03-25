package org.frankframework.jdbc.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLWarning;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeEach;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.BytesResource;
import org.frankframework.core.Resource;
import org.frankframework.core.Resource.GlobalScopeProvider;
import org.frankframework.testutil.ConfigurationMessageEventListener;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.TxManagerTest;
import org.frankframework.util.AppConstants;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.MessageKeeper;

@Log4j2
public class MigratorTest {

	private LiquibaseMigrator migrator;
	private final String tableName = "DUMMYTABLE";
	private DatabaseTestEnvironment env;

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		this.env = env;

		migrator = env.createBean(LiquibaseMigrator.class);
		migrator.setDatasourceName(env.getDataSourceName());

		env.getConfiguration().getConfigurationWarnings().destroy();
		env.getConfiguration().getConfigurationWarnings().afterPropertiesSet();
		env.getConfiguration().getMessageKeeper().clear();

		dropTableIfPresent(tableName);
		dropTableIfPresent("DATABASECHANGELOG");
		dropTableIfPresent("DATABASECHANGELOGLOCK");
	}

	private boolean isTablePresent(String tableName) throws Exception {
		try(Connection connection = env.getConnection()) {
			return env.getDbmsSupport().isTablePresent(connection, tableName);
		}
	}
	private void dropTableIfPresent(String tableName) throws Exception {
		try(Connection connection = env.getConnection()) {
			if(env.getDbmsSupport().isTablePresent(connection, tableName)) {
				JdbcTestUtil.executeStatement(connection, "DROP TABLE "+tableName);
				SQLWarning warnings = connection.getWarnings();
				if(warnings != null) {
					log.warn(JdbcUtil.warningsToString(warnings));
				}
				assertFalse(env.getDbmsSupport().isTablePresent(connection, tableName), "table ["+tableName+"] should not exist");
			}
		}
	}

	@TxManagerTest
	public void testSimpleChangelogFile(DatabaseTestEnvironment env) throws Exception {
		AppConstants.getInstance(env.getConfiguration().getClassLoader()).setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
		migrator.update();

		MessageKeeper messageKeeper = env.getConfiguration().getMessageKeeper();
		assertNotNull(messageKeeper, "no message logged to the messageKeeper");
		System.err.println(messageKeeper); // == empty?
		assertEquals(1, messageKeeper.size());
		assertEquals("Configuration ["+env.getName()+"] LiquiBase applied [3] change(s) and added tag [three:Niels Meijer]", messageKeeper.getMessage(0).getMessageText());
		assertTrue(isTablePresent(tableName), "table ["+tableName+"] should exist");
		assertFalse(isTablePresent("TABLETWO"), "table [TABLETWO] should not exist");
	}

	@TxManagerTest
	public void testCreateTableChangelogFile(DatabaseTestEnvironment env) throws Exception {
		AppConstants.getInstance(env.getConfiguration().getClassLoader()).setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelogCreate.xml");
		migrator.update();

		MessageKeeper messageKeeper = env.getConfiguration().getMessageKeeper();
		assertNotNull(messageKeeper, "no message logged to the messageKeeper");
		System.err.println(messageKeeper); // == empty?
		assertEquals(1, messageKeeper.size());
		assertEquals("Configuration ["+env.getName()+"] LiquiBase applying change [one:Niels Meijer] description [createTable tableName=DUMMYTABLE] tag [one:Niels Meijer]", messageKeeper.getMessage(0).getMessageText());
		assertTrue(isTablePresent(tableName), "table ["+tableName+"] should exist");
	}

	@TxManagerTest
	public void testFaultyChangelogFile(DatabaseTestEnvironment env) throws Exception {
		AppConstants.getInstance(env.getConfiguration().getClassLoader()).setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelogError.xml");
		migrator.update();

		ConfigurationWarnings warnings = env.getConfiguration().getConfigurationWarnings();
		assertEquals(1, warnings.size());

		String warning = warnings.get(0);
		assertThat(warning, containsString("LiquibaseMigrator Error running LiquiBase update. Failed to execute [3] change(s)")); // Test ObjectName + Error
		assertThat(warning, containsString("Migration failed for changeset Migrator/DatabaseChangelogError.xml::error::Niels Meijer")); // Test liquibase exception
		// H2 logs 'Table \"DUMMYTABLE\" already exists' Oracle throws 'ORA-00955: name is already used by an existing object'
		assertTrue(isTablePresent(tableName), "table ["+tableName+"] should exist");
	}

	@TxManagerTest
	public void testSQLWriter(DatabaseTestEnvironment env) throws Exception {
		Resource resource = Resource.getResource("/Migrator/DatabaseChangelog_plus_changes.xml");
		assertNotNull(resource);

		StringWriter writer = new StringWriter();
		migrator.update(writer, resource);

		String sqlChanges = TestFileUtils.getTestFile("/Migrator/sql_changes_"+env.getDataSourceName().toLowerCase()+".sql");

		String result = applyIgnores(writer.toString());
		result = removeComments(result);
		// If this test fails, please check if difference in output might be caused by driver-updates or Liquibase updates
		TestAssertions.assertEqualsIgnoreCRLF(sqlChanges, result);
	}

	@TxManagerTest
	public void testSQLWriterBytesResource(DatabaseTestEnvironment env) throws Exception {
		// Arrange
		Resource resource = Resource.getResource("/Migrator/DatabaseChangelog_plus_changes.xml");
		String sqlChanges = TestFileUtils.getTestFile("/Migrator/sql_changes_"+env.getDataSourceName().toLowerCase()+".sql");
		assertNotNull(resource);

		resource = new BytesResource(resource.openStream(), "inputstreamresource.xml", new GlobalScopeProvider());
		StringWriter writer = new StringWriter();

		// Act
		migrator.update(writer, resource);

		// Assert
		String result = applyIgnores(writer.toString());
		result = removeComments(result);
		result = result.replaceAll("inputstreamresource.xml", "Migrator/DatabaseChangelog_plus_changes.xml");

		// If this test fails, please check if difference in output might be caused by driver-updates or Liquibase updates
		TestAssertions.assertEqualsIgnoreCRLF(sqlChanges, result);
	}

	private String removeComments(String file) throws IOException {
		BufferedReader buf = new BufferedReader(new StringReader(file));

		StringBuilder string = new StringBuilder();
		String line = buf.readLine();
		while (line != null) {
			if(line.startsWith("--") || "\n".equals(line)) {
				line = buf.readLine();
				continue;
			}
			string.append(line);
			if (StringUtils.isNotEmpty(line)) {
				string.append("\n");
			}
			line = buf.readLine();
		}
		return string.toString();
	}

	private String applyIgnores(String sqlScript) {
		Pattern regex = Pattern.compile("(\\d+)\\'\\)");
		Matcher match = regex.matcher(sqlScript);
		if(match.find()) {
			String deploymentId = match.group(1);
			sqlScript = sqlScript.replace(deploymentId, "IGNORE");
		} else {
			fail("no match found");
			return null;
		}
		sqlScript = sqlScript.replaceAll("\\'[4-9]\\.\\d+\\.\\d{1,3}\\'", "'VERSION'"); //Replace the Liquibase Version
		sqlScript = sqlScript.replaceAll("'\\d{1,2}:[a-f0-9]{32}'", "'CHANGESET-CHECKSUM'"); //Replace the Liquibase Changeset Checksum

		sqlScript = sqlScript.replaceAll("SET SEARCH_PATH TO public, \"\\$user\",\"public\";", ""); //Remove search path setting
		sqlScript = sqlScript.replace("\r\n", "\n"); //change CRLF into LF
		sqlScript = sqlScript.replace("\n\n\n", "\n"); //remove duplicate linefeeds

		return sqlScript.replaceAll("(LOCKEDBY = ')(.*)(WHERE)", "LOCKEDBY = 'IGNORE', LOCKGRANTED = 'IGNORE' WHERE");
	}

	@TxManagerTest
	public void testScriptExecutionLogs(DatabaseTestEnvironment env) {
		AppConstants.getInstance(env.getConfiguration().getClassLoader()).setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
		Configurator.reconfigure();
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			migrator.validate();
			assertTrue(appender.contains("Successfully acquired change log lock")); //Validate Liquibase logs on INFO level

			Configurator.setRootLevel(Level.DEBUG); //Capture all loggers (at debug level)
			Configurator.setLevel("org.frankframework", Level.WARN); //Exclude Frank!Framework loggers
			Configurator.setLevel("liquibase", Level.WARN); //Set all Liquibase loggers to WARN
			appender.clearLogs();

			migrator.update();

			String msg = "LiquiBase applied [3] change(s) and added tag [three:Niels Meijer]";
			assertFalse(appender.contains(msg), "expected message not to be logged but found ["+appender.getLogLines()+"]"); //Validate Liquibase doesn't log

			ConfigurationMessageEventListener configurationMessages = env.getConfiguration().getBean("ConfigurationMessageListener", ConfigurationMessageEventListener.class);
			assertTrue(configurationMessages.contains(msg)); //Validate Liquibase did run
		} finally {
			Configurator.reconfigure();
		}
	}
}
