package nl.nn.adapterframework.jdbc.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.BytesResource;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.core.Resource.GlobalScopeProvider;
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase;
import nl.nn.adapterframework.testutil.ConfigurationMessageEventListener;
import nl.nn.adapterframework.testutil.TestAppender;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.MessageKeeper;

public class MigratorTest extends TransactionManagerTestBase {

	private LiquibaseMigrator migrator = null;
	private String tableName="DUMMYTABLE";

	@Override
	protected void prepareDatabase() throws Exception {
		super.prepareDatabase();
		//Ignore programmatic creation of Temp table, run Liquibase instead!
		dropTableIfPresent(tableName);
		dropTableIfPresent("DATABASECHANGELOG");
		dropTableIfPresent("DATABASECHANGELOGLOCK");

		migrator = getConfiguration().createBean(LiquibaseMigrator.class);
		migrator.setDatasourceName(getDataSourceName());
	}

	@Override
	public void setup() throws Exception {
		super.setup();

		//Make sure there are no previous warnings present
		getConfiguration().getConfigurationWarnings().destroy();
		getConfiguration().getConfigurationWarnings().afterPropertiesSet();
		getConfiguration().getMessageKeeper().clear();
	}

	@Test
	public void testSimpleChangelogFile() throws Exception {
		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
		migrator.update();

		MessageKeeper messageKeeper = getConfiguration().getMessageKeeper();
		assertNotNull("no message logged to the messageKeeper", messageKeeper);
		assertEquals(1, messageKeeper.size());
		assertEquals("Configuration ["+getTransactionManagerType().name()+"] LiquiBase applied [2] change(s) and added tag [two:Niels Meijer]", messageKeeper.getMessage(0).getMessageText());
		assertFalse("table ["+tableName+"] should not exist", isTablePresent(tableName));
	}

	@Test
	public void testFaultyChangelogFile() throws Exception {
		AppConstants.getInstance(getConfiguration().getClassLoader()).setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelogError.xml");
		migrator.update();

		ConfigurationWarnings warnings = getConfiguration().getConfigurationWarnings();
		assertEquals(1, warnings.size());

		String warning = warnings.get(0);
		assertThat(warning, containsString("LiquibaseMigrator Error running LiquiBase update. Failed to execute [3] change(s)")); //Test ObjectName + Error
		assertThat(warning, containsString("Migration failed for changeset Migrator/DatabaseChangelogError.xml::error::Niels Meijer")); //Test liquibase exception
		//H2 logs 'Table \"DUMMYTABLE\" already exists' Oracle throws 'ORA-00955: name is already used by an existing object'
		assertTrue("table ["+tableName+"] should exist", isTablePresent(tableName));
	}

	@Test
	public void testSQLWriter() throws Exception {

		Resource resource = Resource.getResource("/Migrator/DatabaseChangelog_plus_changes.xml");
		assertNotNull(resource);

		StringWriter writer = new StringWriter();
		migrator.update(writer, resource);

		String sqlChanges = TestFileUtils.getTestFile("/Migrator/sql_changes_"+getDataSourceName().toLowerCase()+".sql");

		String result = applyIgnores(writer.toString());
		result = removeComments(result);
		TestAssertions.assertEqualsIgnoreCRLF(sqlChanges, result);
	}

	@Test
	public void testSQLWriterBytesResource() throws Exception {
		// Arrange
		Resource resource = Resource.getResource("/Migrator/DatabaseChangelog_plus_changes.xml");
		String sqlChanges = TestFileUtils.getTestFile("/Migrator/sql_changes_"+getDataSourceName().toLowerCase()+".sql");
		assertNotNull(resource);

		resource = new BytesResource(resource.openStream(), "inputstreamresource.xml", new GlobalScopeProvider());
		StringWriter writer = new StringWriter();

		// Act
		migrator.update(writer, resource);

		// Assert
		String result = applyIgnores(writer.toString());
		result = removeComments(result);
		result = result.replaceAll("inputstreamresource.xml", "Migrator/DatabaseChangelog_plus_changes.xml");

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
		}
		else {
			fail("no match found");
			return null;
		}
		sqlScript = sqlScript.replaceAll("\\'[4-9]\\.\\d+\\.\\d{1,3}\\'", "'VERSION'"); //Replace the Liquibase Version
		sqlScript = sqlScript.replaceAll("'\\d{1,2}:[a-f0-9]{32}'", "'CHANGESET-CHECKSUM'"); //Replace the Liquibase Changeset Checksum

		sqlScript = sqlScript.replaceAll("SET SEARCH_PATH TO public, \"\\$user\",\"public\";", ""); //Remove search path setting
		sqlScript = sqlScript.replaceAll("\r\n", "\n"); //change CRLF into LF
		sqlScript = sqlScript.replaceAll("\n\n\n", "\n"); //remove duplicate linefeeds

		return sqlScript.replaceAll("(LOCKEDBY = ')(.*)(WHERE)", "LOCKEDBY = 'IGNORE', LOCKGRANTED = 'IGNORE' WHERE");
	}

	@Test
	public void testScriptExecutionLogs() throws Exception {
		AppConstants.getInstance().setProperty("liquibase.changeLogFile", "/Migrator/DatabaseChangelog.xml");
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
		try {
			Configurator.reconfigure();
			TestAppender.addToRootLogger(appender);
			migrator.validate();
			assertTrue(appender.contains("Successfully acquired change log lock")); //Validate Liquibase logs on INFO level

			Configurator.setRootLevel(Level.DEBUG); //Capture all loggers (at debug level)
			Configurator.setLevel("nl.nn", Level.WARN); //Exclude Frank!Framework loggers
			Configurator.setLevel("liquibase", Level.WARN); //Set all Liquibase loggers to WARN
			appender.clearLogs();

			migrator.update();

			String msg = "LiquiBase applied [2] change(s) and added tag [two:Niels Meijer]";
			assertFalse("expected message not to be logged but found ["+appender.getLogLines()+"]", appender.contains(msg)); //Validate Liquibase doesn't log

			ConfigurationMessageEventListener configurationMessages = getConfiguration().getBean("ConfigurationMessageListener", ConfigurationMessageEventListener.class);
			assertTrue(configurationMessages.contains(msg)); //Validate Liquibase did run
		} finally {
			TestAppender.removeAppender(appender);
			Configurator.reconfigure();
		}
	}
}
