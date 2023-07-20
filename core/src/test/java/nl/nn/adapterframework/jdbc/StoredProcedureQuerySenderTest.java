package nl.nn.adapterframework.jdbc;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

public class StoredProcedureQuerySenderTest extends JdbcTestBase {

	StoredProcedureQuerySender sender;

	PipeLineSession session;

	@Before
	public void setUp() throws Exception {
		assumeThat("H2 does not support proper stored procedures, skipping test suite", productKey, not(equalToIgnoringCase("H2")));

		// TODO: Add stored procedures for all databases (except H2). Until then, limit the test to MySQL, Postgres.
		assumeThat(productKey, isOneOf("MySQL", "PostgreSQL"));

		runMigrator("Migrator/DatabaseChangelog-StoredProcedures.xml");

		sender = getConfiguration().createBean(StoredProcedureQuerySender.class);
		sender.setSqlDialect(productKey);
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
		sender.setQuery("call insert_message('" + value + "', 'P')");
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
		sender.setQuery("call insert_message('" + value + "', 'P')");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setResultQuery("SELECT COUNT(*) FROM sp_testdata WHERE tmessage = '" + value + "'");
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
		sender.setQuery("call insert_message(?, 'P')");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setResultQuery("SELECT COUNT(*) FROM sp_testdata WHERE tmessage = '" + value + "'");
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
		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);

		String query;
		if (productKey.equalsIgnoreCase("MySQL")) {
			query = "call get_message_by_id(?, ?)";
		} else if (productKey.equalsIgnoreCase("PostgreSQL")) {
			query = "call get_message_by_id(?, ?)";
		} else {
			throw new IllegalStateException("Don't yet know how to make procedure-call with out-parameters for database '" + productKey + "'");
		}
		sender.setQuery(query);
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

	private int countRowsWithMessageValue(final String value) throws SQLException, JdbcException {
		String checkValueStatement = dbmsSupport.convertQuery("SELECT COUNT(*) FROM sp_testdata WHERE tmessage = ?", "H2");
		int rowsCounted;
		try (PreparedStatement statement = getConnection().prepareStatement(checkValueStatement)) {
			statement.setString(1, value);
			ResultSet resultSet = statement.executeQuery();
			resultSet.next();
			rowsCounted = resultSet.getInt(1);
		}
		return rowsCounted;
	}

	private long insertRowWithMessageValue(final String value) throws SQLException, JdbcException {
		String insertValueStatement = dbmsSupport.convertQuery("INSERT INTO sp_testdata (tmessage, tchar) VALUES (?, 'E')", "H2");
		try (PreparedStatement statement = getConnection().prepareStatement(insertValueStatement, Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, value);
			statement.executeUpdate();
			ResultSet generatedKeys = statement.getGeneratedKeys();
			generatedKeys.next();
			return generatedKeys.getLong(1);
		}
	}
}
