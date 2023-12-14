package nl.nn.adapterframework.jdbc;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterInputStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.StreamUtil;

public class StoredProcedureQuerySenderTest extends JdbcTestBase {

	public static final String TEST_DATA_STRING = "tést-data";
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
	public void testSimpleStoredProcedureBlobInputParameter() throws Exception {
		assumeThat("H2, PSQL, DB2 not supported for this test case", productKey, not(isOneOf("H2", "PostgreSQL", "DB2")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);
		sender.setQuery("CALL SET_BLOB(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter p1 = new Parameter("id", String.valueOf(id));
		p1.setType(Parameter.ParameterType.NUMBER);
		sender.addParameter(p1);
		Parameter p2 = new Parameter("data", null);
		p2.setSessionKey("data");
		p2.setType(Parameter.ParameterType.BINARY);
		sender.addParameter(p2);
		session.put("data", new ByteArrayInputStream(TEST_DATA_STRING.getBytes(StandardCharsets.UTF_8)));

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		String blobValue = getBlobValueAsString(id);
		assertEquals(TEST_DATA_STRING, blobValue);
	}

	@Test
	public void testSimpleStoredProcedureClobInputParameter() throws Exception {
		assumeThat("H2, PSQL, DB2 not supported for this test case", productKey, not(isOneOf("H2", "PostgreSQL", "DB2")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);
		sender.setQuery("CALL SET_CLOB(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter p1 = new Parameter("id", String.valueOf(id));
		p1.setType(Parameter.ParameterType.NUMBER);
		sender.addParameter(p1);
		Parameter p2 = new Parameter("data", null);
		p2.setSessionKey("data");
		p2.setType(Parameter.ParameterType.CHARACTER);
		sender.addParameter(p2);
		session.put("data", new StringReader(TEST_DATA_STRING));

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		String clobValue = getClobValueAsString(id);
		assertEquals(TEST_DATA_STRING, clobValue);
	}

	@Test
	public void testSimpleStoredProcedureClobInputParameter2() throws Exception {
		assumeThat("H2, PSQL, DB2 not supported for this test case", productKey, not(isOneOf("H2", "PostgreSQL", "DB2")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);
		sender.setQuery("CALL SET_CLOB(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter p1 = new Parameter("id", String.valueOf(id));
		p1.setType(Parameter.ParameterType.NUMBER);
		sender.addParameter(p1);
		Parameter p2 = new Parameter("data", null);
		p2.setSessionKey("data");
		p2.setType(Parameter.ParameterType.CHARACTER);
		sender.addParameter(p2);
		Message message1 = Message.asMessage(new StringReader(TEST_DATA_STRING));
		message1.getContext().withSize(TEST_DATA_STRING.getBytes().length);
		session.put("data", message1);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		String clobValue = getClobValueAsString(id);
		assertEquals(TEST_DATA_STRING, clobValue);
	}

	@Test
	public void testStoredProcedureInputAndOutputParameters() throws Exception {

		assumeThat("H2 does not support OUT parameters, skipping test case", productKey, not(equalToIgnoringCase("H2")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);

		sender.setQuery("CALL GET_MESSAGE_BY_ID(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(Parameter.ParameterType.STRING);
		sender.addParameter(outParam1);

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
	public void testStoredProcedureInputAndOutputParameterNullValue() throws Exception {

		assumeThat("H2 does not support OUT parameters, skipping test case", productKey, not(equalToIgnoringCase("H2")));

		// Arrange
		long id = insertRowWithMessageValue(null);

		sender.setQuery("CALL GET_MESSAGE_BY_ID(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(Parameter.ParameterType.STRING);
		sender.addParameter(outParam1);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		assertNull(result.getResult().asString());
	}

	@Test
	public void testStoredProcedureBlobOutputParameter1() throws Exception {
		testStoredProcedureBlobOutputParameter(false, true, null);
	}
	@Test
	public void testStoredProcedureBlobOutputParameter2() throws Exception {
		testStoredProcedureBlobOutputParameter(false, true, StandardCharsets.UTF_8.name());
	}
	@Test
	public void testStoredProcedureBlobOutputParameter3() throws Exception {
		testStoredProcedureBlobOutputParameter(true, false, StandardCharsets.UTF_8.name());
	}
	@Test
	public void testStoredProcedureBlobOutputParameter4() throws Exception {
		testStoredProcedureBlobOutputParameter(true, true, StandardCharsets.UTF_8.name());
	}

	private void testStoredProcedureBlobOutputParameter(boolean blobSmartGet, boolean compressed, String charSet) throws Exception {

		assumeThat("H2, PSQL not supported for this test case", productKey, not(isOneOf("H2", "PostgreSQL")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithBlobValue(value, compressed);

		sender.setQuery("CALL GET_BLOB(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);
		sender.setBlobSmartGet(blobSmartGet);
		sender.setBlobCharset(charSet);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(Parameter.ParameterType.BINARY);
		sender.addParameter(outParam1);

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
	public void testStoredProcedureClobOutputParameter() throws Exception {

		assumeThat("H2, PSQL not supported for this test case", productKey, not(isOneOf("H2", "PostgreSQL")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithClobValue(value);

		sender.setQuery("CALL GET_CLOB(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(Parameter.ParameterType.CHARACTER);
		sender.addParameter(outParam1);

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
	public void testStoredProcedureBlobOutputParameterNullValue() throws Exception {

		assumeThat("H2, PSQL not supported for this test case", productKey, not(isOneOf("H2", "PostgreSQL")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);

		sender.setQuery("CALL GET_BLOB(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(Parameter.ParameterType.BINARY);
		sender.addParameter(outParam1);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		Object resultData = result.getResult().asObject();
		assertTrue(resultData instanceof byte[]);
		byte[] bytes = (byte[]) resultData;
		assertEquals(0, bytes.length);
		assertEquals("", result.getResult().asString());
	}
	@Test
	public void testStoredProcedureClobOutputParameterNullValue() throws Exception {

		assumeThat("H2, PSQL not supported for this test case", productKey, not(isOneOf("H2", "PostgreSQL")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);

		sender.setQuery("CALL GET_CLOB(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(Parameter.ParameterType.CHARACTER);
		sender.addParameter(outParam1);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		assertTrue(StringUtils.isEmpty(result.getResult().asString()));
	}

	@Test
	public void testStoredProcedureInputAndOutputParametersXmlOutput() throws Exception {

		assumeThat("H2 does not support OUT parameters, skipping test case", productKey, not(equalToIgnoringCase("H2")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);

		sender.setQuery("CALL GET_MESSAGE_AND_TYPE_BY_ID(?, ?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(Parameter.ParameterType.STRING);
		sender.addParameter(outParam1);

		Parameter outParam2 = new Parameter("r2", null);
		outParam2.setMode(Parameter.ParameterMode.OUTPUT);
		outParam2.setType(Parameter.ParameterType.STRING);
		sender.addParameter(outParam2);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		String expectedOutput = loadOutputExpectation("/Jdbc/StoredProcedureQuerySender/multi-output-parameter-xml-result.xml", value);

		final String actual = cleanActualOutput(result);
		assertXmlEquals(expectedOutput, actual);
	}

	@Test
	public void testStoredProcedureOutputParameterConversion() throws Exception {

		assumeThat("H2 does not support OUT parameters, skipping test case", productKey, not(equalToIgnoringCase("H2")));

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value);

		sender.setQuery("CALL COUNT_MESSAGES_BY_CONTENT(?, ?, ?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());

		Parameter inParam = new Parameter("message", value);
		inParam.setMode(Parameter.ParameterMode.INOUT);
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(Parameter.ParameterType.INTEGER);
		sender.addParameter(outParam1);

		Parameter outParam2 = new Parameter("r2", null);
		outParam2.setMode(Parameter.ParameterMode.OUTPUT);
		outParam2.setType(Parameter.ParameterType.INTEGER);
		sender.addParameter(outParam2);

		Parameter outParam3 = new Parameter("r3", null);
		outParam3.setMode(Parameter.ParameterMode.OUTPUT);
		outParam3.setType(Parameter.ParameterType.NUMBER);
		sender.addParameter(outParam3);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		final String expectedOutput = loadOutputExpectation("/Jdbc/StoredProcedureQuerySender/multi-output-parameter-type-conversions-test-xml-result.xml", value);
		final String actual = cleanActualOutput(result);

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
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		final String expectedOutput = loadOutputExpectation("/Jdbc/StoredProcedureQuerySender/multi-row-results.xml", value, ids);
		final String actual = cleanActualOutput(result);

		assertXmlEquals(expectedOutput, actual);
	}

	@Test
	public void testCallFunction() throws Exception {
		assumeThat("CALL to custom function only tested on Oracle so far", productKey, equalToIgnoringCase("Oracle"));

		// Arrange
		sender.setQuery("{ ? = call add_numbers(?, ?) }");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());
		sender.setScalar(true);

		Parameter resultParam = new Parameter("result", "0");
		resultParam.setType(Parameter.ParameterType.INTEGER);
		resultParam.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(resultParam);
		Parameter p1 = new Parameter("one", "1");
		p1.setType(Parameter.ParameterType.INTEGER);
		sender.addParameter(p1);
		Parameter p2 = new Parameter("two", "2");
		p2.setType(Parameter.ParameterType.INTEGER);
		sender.addParameter(p2);

		sender.configure();
		sender.open();

		// Act
		SenderResult result = sender.sendMessage(Message.nullMessage(), session);

		// Arrange
		assertTrue(result.isSuccess());
		assertEquals("3", result.getResult().asString());
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

	private long insertRowWithBlobValue(final String value, final boolean compressed) throws SQLException, JdbcException {
		String insertValueQuery = dbmsSupport.convertQuery("INSERT INTO SP_TESTDATA (TBLOB, TCHAR) VALUES (?, 'B')", "Oracle");
		// Column name of generated key-field should be in lowercase for PostgreSQL
		try (PreparedStatement statement = getConnection().prepareStatement(insertValueQuery, new String[] {"tkey"})) {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
			statement.setBlob(1, (compressed) ? new DeflaterInputStream(inputStream) : inputStream);
			statement.executeUpdate();
			ResultSet generatedKeys = statement.getGeneratedKeys();
			if (!generatedKeys.next()) {
				fail("No generated keys from insert statement");
			}
			return generatedKeys.getLong(1);
		}
	}

	private long insertRowWithClobValue(final String value) throws SQLException, JdbcException {
		String insertValueQuery = dbmsSupport.convertQuery("INSERT INTO SP_TESTDATA (TCLOB, TCHAR) VALUES (?, 'C')", "Oracle");
		// Column name of generated key-field should be in lowercase for PostgreSQL
		try (PreparedStatement statement = getConnection().prepareStatement(insertValueQuery, new String[] {"tkey"})) {
			statement.setClob(1, new StringReader(value));
			statement.executeUpdate();
			ResultSet generatedKeys = statement.getGeneratedKeys();
			if (!generatedKeys.next()) {
				fail("No generated keys from insert statement");
			}
			return generatedKeys.getLong(1);
		}
	}

	private String getBlobValueAsString(final long id) throws SQLException, JdbcException, IOException {
		String getBlobQuery = dbmsSupport.convertQuery("SELECT TBLOB FROM SP_TESTDATA WHERE TKEY = ?", "Oracle");
		try (PreparedStatement statement = getConnection().prepareStatement(getBlobQuery)) {
			statement.setLong(1, id);
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				fail("No data found for id = " + id);
			}
			Blob blob = rs.getBlob(1);
			try (InputStream in = blob.getBinaryStream()) {
				return StreamUtil.streamToString(in);
			}
		}
	}

	private String getClobValueAsString(final long id) throws SQLException, JdbcException, IOException {
		String getClobQuery = dbmsSupport.convertQuery("SELECT TCLOB FROM SP_TESTDATA WHERE TKEY = ?", "Oracle");
		try (PreparedStatement statement = getConnection().prepareStatement(getClobQuery)) {
			statement.setLong(1, id);
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				fail("No data found for id = " + id);
			}
			Clob clob = rs.getClob(1);
			try (Reader in = clob.getCharacterStream()) {
				return StreamUtil.readerToString(in, "\n");
			}
		}
	}

	private static String cleanActualOutput(final SenderResult result) throws IOException {
		return result.getResult()
				.asString()
				.replaceAll("\\.0+<", "<")
				.replaceAll("(?m)<fielddefinition>.+?</fielddefinition>", "<fielddefinition>IGNORE</fielddefinition>");
	}

	private static String loadOutputExpectation(final String file, final String messageContents) throws IOException {
		return TestFileUtils
				.getTestFile(file)
				.replace("MESSAGE-CONTENTS", messageContents);
	}

	private static String loadOutputExpectation(final String file, final String value, final long[] ids) throws IOException {
		String expectedOutput = loadOutputExpectation(file, value);
		StringBuffer sb = new StringBuffer();
		Matcher matcher = Pattern.compile("MSG-ID").matcher(expectedOutput);
		int idx = 0;
		while (matcher.find()) {
			matcher.appendReplacement(sb, String.valueOf(ids[idx++]));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
}
