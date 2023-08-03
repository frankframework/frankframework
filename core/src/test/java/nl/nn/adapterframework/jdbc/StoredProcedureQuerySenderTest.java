package nl.nn.adapterframework.jdbc;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class StoredProcedureQuerySenderTest extends JdbcTestBase {

	StoredProcedureQuerySender sender;

	PipeLineSession session;

	@Before
	public void setUp() throws Exception {
		runMigrator("Jdbc/StoredProcedureQuerySender/DatabaseChangelog-StoredProcedures.xml");

		sender = getConfiguration().createBean(StoredProcedureQuerySender.class);
		sender.setSqlDialect("Oracle");
		sender.setDatasourceName(productKey);

		session = new PipeLineSession();
	}

	@After
	public void tearDown() throws Exception {
		if (session != null) {
			session.close();
		}
	}

	@Test
	public void testSimpleStoredProcedureNoResultNoParameters() throws Exception {
		// Arrange
		String value = UUID.randomUUID().toString();
		sender.setQuery("CALL INSERT_MESSAGE('" + value + "', 'P')");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		// Due to differences between databases, "rows updated" is sometimes 1, sometimes 0
		assertTrue("Result should start with [<result><rowsupdated>]", result.getResult().asString().startsWith("<result><rowsupdated>"));

		// Check presence of row that should have been inserted
		int rowsCounted = countRowsWithMessageValue(value);

		assertEquals(1, rowsCounted);
	}

	@Test
	public void testSimpleStoredProcedureResultQueryNoParameters() throws Exception {
		// Arrange
		String value = UUID.randomUUID().toString();
		sender.setQuery("CALL INSERT_MESSAGE('" + value + "', 'P')");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setResultQuery("SELECT COUNT(*) FROM SP_TESTDATA WHERE TMESSAGE = '" + value + "'");
		sender.setScalar(true);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		assertEquals("1", result.getResult().asString());
	}

	@Test
	public void testSimpleStoredProcedureResultQueryInputParameters() throws Exception {
		// Arrange
		String value = UUID.randomUUID().toString();
		sender.setQuery("CALL INSERT_MESSAGE(?, 'P')");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setResultQuery("SELECT COUNT(*) FROM SP_TESTDATA WHERE TMESSAGE = '" + value + "'");
		sender.setScalar(true);

		Parameter parameter = new Parameter("message", value);
		parameter.configure();
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		assertEquals("1", result.getResult().asString());
	}

	@Test
	public void testStoredProcedureInputAndOutputParameters() throws Exception {

		assumeThat("H2 does not support OUT parameters, skipping test case", productKey, not(equalToIgnoringCase("H2")));
		assumeThat("Oracle OUT parameters do not yet work, skipping test case", productKey, not(equalToIgnoringCase("Oracle")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);

		sender.setQuery("CALL GET_MESSAGE_BY_ID(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setOutputParameters("2");
		sender.setScalar(true);

		Parameter parameter = new Parameter("id", String.valueOf(id));
		parameter.configure();
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		assertEquals(value, result.getResult().asString());
	}

	@Test
	public void testStoredProcedureInputAndOutputParametersXmlOutput() throws Exception {

		assumeThat("H2 does not support OUT parameters, skipping test case", productKey, not(equalToIgnoringCase("H2")));
		assumeThat("Oracle OUT parameters do not yet work, skipping test case", productKey, not(equalToIgnoringCase("Oracle")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);

		sender.setQuery("CALL GET_MESSAGE_AND_TYPE_BY_ID(?, ?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setOutputParameters("2, 3");

		Parameter parameter = new Parameter("id", String.valueOf(id));
		parameter.configure();
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		String expectedOutput = TestFileUtils
				.getTestFile("/Jdbc/StoredProcedureQuerySender/multi-output-parameter-xml-result.xml")
				.replace("MESSAGE-CONTENTS", value);

		final String actual = result.getResult()
				.asString()
				.replaceAll("(?m)<fielddefinition>.+?</fielddefinition>", "<fielddefinition>IGNORE</fielddefinition>");
		assertXmlEquals(expectedOutput, actual);
	}

	@Test
	public void testStoredProcedureReturningResultSet() throws Exception {
		assumeThat("PostgreSQL and Oracle do not support stored procedures that directly return multi-row results, skipping test", productKey, not(isOneOf("Oracle", "PostgreSQL")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] =insertRowWithMessageValue(value);
		}

		sender.setQuery("CALL GET_MESSAGES_BY_CONTENT(?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.SELECT.name());

		Parameter parameter = new Parameter("content", value);
		parameter.configure();
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		int idx = 0;
		String expectedOutput = TestFileUtils
				.getTestFile("/Jdbc/StoredProcedureQuerySender/multi-row-results.xml")
				.replace("MESSAGE-CONTENTS", value);
		StringBuffer sb = new StringBuffer();
		Matcher matcher = Pattern.compile("MSG-ID").matcher(expectedOutput);
		while (matcher.find()) {
			matcher.appendReplacement(sb, String.valueOf(ids[idx++]));
		}
		matcher.appendTail(sb);

		final String actual = result.getResult()
				.asString()
				.replaceAll("(?m)<fielddefinition>.+?</fielddefinition>", "<fielddefinition>IGNORE</fielddefinition>");

		assertXmlEquals(sb.toString(), actual);
	}

	private int countRowsWithMessageValue(final String value) throws SQLException, JdbcException {
		String checkValueStatement = dbmsSupport.convertQuery("SELECT COUNT(*) FROM SP_TESTDATA WHERE TMESSAGE = ?", "Oracle");
		int rowsCounted;
		try (PreparedStatement statement = getConnection().prepareStatement(checkValueStatement)) {
			statement.setString(1, value);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				rowsCounted = resultSet.getInt(1);
			} else {
				rowsCounted = 0;
			}
		}
		return rowsCounted;
	}

	private long insertRowWithMessageValue(final String value) throws SQLException, JdbcException {
		String insertValueQuery = dbmsSupport.convertQuery("INSERT INTO SP_TESTDATA (TMESSAGE, TCHAR) VALUES (?, 'E')", "Oracle");
		// Column name of generated key-field should be in lowercase for PostgreSQL
		try (PreparedStatement statement = getConnection().prepareStatement(insertValueQuery, new String[] {"tkey"})) {
			statement.setString(1, value);
			statement.executeUpdate();
			ResultSet generatedKeys = statement.getGeneratedKeys();
			if (!generatedKeys.next()) {
				fail("No generated keys from insert statement");
			}
			return generatedKeys.getLong(1);
		}
	}
}
