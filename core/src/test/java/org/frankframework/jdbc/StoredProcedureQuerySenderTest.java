package org.frankframework.jdbc;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.dbms.JdbcException;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import liquibase.Liquibase;

@WithLiquibase(tableName = StoredProcedureQuerySenderTest.TABLE_NAME, file = "Jdbc/StoredProcedureQuerySender/DatabaseChangelog-StoredProcedures.xml")
public class StoredProcedureQuerySenderTest {

	protected static final String TABLE_NAME = "SP_TESTDATA";
	public static final String TEST_DATA_STRING = "tést-data";
	StoredProcedureQuerySender sender;
	protected static Logger log = LogUtil.getLogger(StoredProcedureQuerySenderTest.class);
	protected Liquibase liquibase;

	PipeLineSession session;

	@DatabaseTest.Parameter(0)
	private TransactionManagerType transactionManagerType;

	@DatabaseTest.Parameter(1)
	private String dataSourceName;

	@BeforeEach
	public void setUp(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		sender = databaseTestEnvironment.getConfiguration().createBean(StoredProcedureQuerySender.class);
		sender.setSqlDialect("Oracle");
		sender.setDatasourceName(dataSourceName);

		session = new PipeLineSession();
	}

	@AfterEach
	public void tearDown(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		if (session != null) {
			session.close();
		}
	}

	@DatabaseTest
	public void testSimpleStoredProcedureNoResultNoParameters(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		assumeTrue(dataSourceName.equals("H2"), "H2 driver gives incorrect results for this test case");

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
		assumeTrue(result.getResult().asString().startsWith("<result><rowsupdated>"), "Result should start with [<result><rowsupdated>]");

		// Check presence of row that should have been inserted
		int rowsCounted = countRowsWithMessageValue(value, databaseTestEnvironment);

		assertEquals(1, rowsCounted);
	}

	@DatabaseTest
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

	@DatabaseTest
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

	@DatabaseTest
	public void testSimpleStoredProcedureBlobInputParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(dataSourceName.contains("H2") || dataSourceName.contains("PostgreSQL") || dataSourceName.contains("DB2"), "H2, PSQL, DB2 not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);
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
		String blobValue = getBlobValueAsString(id, databaseTestEnvironment);
		assertEquals(TEST_DATA_STRING, blobValue);
	}

	@DatabaseTest
	public void testSimpleStoredProcedureClobInputParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(dataSourceName.contains("H2") || dataSourceName.contains("PostgreSQL") || dataSourceName.contains("DB2"), "H2, PSQL, DB2 not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);
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
		String clobValue = getClobValueAsString(id, databaseTestEnvironment);
		assertEquals(TEST_DATA_STRING, clobValue);
	}

	@DatabaseTest
	public void testSimpleStoredProcedureClobInputParameter2(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(dataSourceName.contains("H2") || dataSourceName.contains("PostgreSQL") || dataSourceName.contains("DB2"), "H2, PSQL, DB2 not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);
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
		String clobValue = getClobValueAsString(id, databaseTestEnvironment);
		assertEquals(TEST_DATA_STRING, clobValue);
	}

	@DatabaseTest
	public void testStoredProcedureInputAndOutputParameters(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(dataSourceName.equalsIgnoreCase("H2"), "H2 does not support OUT parameters, skipping test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);

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

	@DatabaseTest
	public void testStoredProcedureInputAndOutputParameterNullValue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(dataSourceName.equalsIgnoreCase("H2"), "H2 does not support OUT parameters, skipping test case");

		// Arrange
		long id = insertRowWithMessageValue(null, databaseTestEnvironment);

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

	@DatabaseTest
	public void testStoredProcedureBlobOutputParameter1(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testStoredProcedureBlobOutputParameter(false, true, null, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testStoredProcedureBlobOutputParameter2(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testStoredProcedureBlobOutputParameter(false, true, StandardCharsets.UTF_8.name(), databaseTestEnvironment);
	}

	@DatabaseTest
	public void testStoredProcedureBlobOutputParameter3(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testStoredProcedureBlobOutputParameter(true, false, StandardCharsets.UTF_8.name(), databaseTestEnvironment);
	}

	@DatabaseTest
	public void testStoredProcedureBlobOutputParameter4(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testStoredProcedureBlobOutputParameter(true, true, StandardCharsets.UTF_8.name(), databaseTestEnvironment);
	}

	private void testStoredProcedureBlobOutputParameter(boolean blobSmartGet, boolean compressed, String charSet, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(dataSourceName.contains("H2") || dataSourceName.contains("PostgreSQL"), "H2, PSQL not supported for this test case");


		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithBlobValue(value, compressed, databaseTestEnvironment);

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

	@DatabaseTest
	public void testStoredProcedureClobOutputParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(dataSourceName.contains("H2") || dataSourceName.contains("PostgreSQL"), "H2, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithClobValue(value, databaseTestEnvironment);

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

	@DatabaseTest
	public void testStoredProcedureBlobOutputParameterNullValue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(dataSourceName.contains("H2") || dataSourceName.contains("PostgreSQL"), "H2, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);

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

	@DatabaseTest
	public void testStoredProcedureClobOutputParameterNullValue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(dataSourceName.contains("H2") || dataSourceName.contains("PostgreSQL"), "H2, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);

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

	@DatabaseTest
	public void testStoredProcedureInputAndOutputParametersXmlOutput(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(dataSourceName.equalsIgnoreCase("H2"), "H2 does not support OUT parameters, skipping test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);

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

	@DatabaseTest
	public void testStoredProcedureOutputParameterConversion(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(dataSourceName.equalsIgnoreCase("H2"), "H2 does not support OUT parameters, skipping test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		insertRowWithMessageValue(value, databaseTestEnvironment);

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

	@DatabaseTest
	public void testStoredProcedureReturningResultSetQueryTypeSelect(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(dataSourceName.contains("Oracle") || dataSourceName.contains("PostgreSQL"), "Oracle, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
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

		final String expectedOutput = loadOutputExpectation("/Jdbc/StoredProcedureQuerySender/multi-row-results-querytype-select.xml", value, ids);
		final String actual = cleanActualOutput(result);

		assertXmlEquals(expectedOutput, actual);
	}

	@DatabaseTest
	public void testStoredProcedureReturningResultSetQueryTypeOther(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(dataSourceName.contains("Oracle") || dataSourceName.contains("PostgreSQL"), "Oracle, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
		}

		sender.setQuery("CALL GET_MESSAGES_BY_CONTENT(?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		final String expectedOutput = loadOutputExpectation("/Jdbc/StoredProcedureQuerySender/multi-row-results-querytype-other.xml", value, ids);
		final String actual = cleanActualOutput(result);

		assertXmlEquals(expectedOutput, actual);
	}

	@DatabaseTest
	public void testStoredProcedureReturningResultSetAndOutParameters(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(dataSourceName.contains("H2") || dataSourceName.contains("PostgreSQL") || dataSourceName.contains("Oracle"), "H2, PSQL, Oracle not supported for this test case");


		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
		}

		sender.setQuery("CALL GET_MESSAGES_AND_COUNT(?, ?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);
		Parameter count = new Parameter();
		count.setName("count");
		count.setMode(Parameter.ParameterMode.OUTPUT);
		count.setType(Parameter.ParameterType.INTEGER);
		sender.addParameter(count);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		final String expectedOutput = loadOutputExpectation("/Jdbc/StoredProcedureQuerySender/multi-row-results-with-extra-out-param.xml", value, ids);
		final String actual = cleanActualOutput(result);

		assertXmlEquals(expectedOutput, actual);
	}

	@DatabaseTest
	public void testStoredProcedureReturningCursorSingleOutParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		assumeTrue(dataSourceName.contains("Oracle"), "REFCURSOR not supported, skipping test");

		// NOTE: This test only works on a clean database as it selects all rows and matches that against fixed expectation.
		int rowCount = countAllRows(databaseTestEnvironment);
		assumeTrue(rowCount == 0, "This test only works on an empty test-data table");

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
		}

		sender.setQuery("CALL GET_ALL_MESSAGES_CURSOR(?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());

		Parameter cursorParam = new Parameter();
		cursorParam.setName("cursor1");
		cursorParam.setType(Parameter.ParameterType.LIST);
		cursorParam.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(cursorParam);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		final String expectedOutput = loadOutputExpectation("/Jdbc/StoredProcedureQuerySender/cursor-output-parameter-xml-result-single.xml", value, ids);
		final String actual = cleanActualOutput(result);

		assertXmlEquals(expectedOutput, actual);
	}

	@DatabaseTest
	public void testStoredProcedureReturningCursorInOutParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeTrue(dataSourceName.contains("Oracle"), "REFCURSOR not supported, skipping test");

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
		}

		sender.setQuery("CALL GET_MESSAGES_CURSOR(?,?,?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);
		Parameter countParam = new Parameter();
		countParam.setName("count");
		countParam.setType(Parameter.ParameterType.INTEGER);
		countParam.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(countParam);
		Parameter cursorParam = new Parameter();
    	cursorParam.setName("cursor1");
    	cursorParam.setType(Parameter.ParameterType.LIST);
    	cursorParam.setMode(Parameter.ParameterMode.OUTPUT);
    	sender.addParameter(cursorParam);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		final String expectedOutput = loadOutputExpectation("/Jdbc/StoredProcedureQuerySender/cursor-output-parameter-xml-result-multi.xml", value, ids);
		final String actual = cleanActualOutput(result);

		assertXmlEquals(expectedOutput, actual);
	}

	@DatabaseTest
	public void testStoredProcedureReturningCursorNotSupported() throws Exception {
		assumeTrue(dataSourceName.equals("MS_SQL"));

		// Arrange
		String value = UUID.randomUUID().toString();
		sender.setQuery("CALL GET_MESSAGES_CURSOR(?,?,?)");
		sender.setQueryType(JdbcQuerySenderBase.QueryType.OTHER.name());

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);
		Parameter countParam = new Parameter();
		countParam.setName("count");
		countParam.setType(Parameter.ParameterType.INTEGER);
		countParam.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(countParam);
		Parameter cursorParam = new Parameter();
    	cursorParam.setName("cursor1");
    	cursorParam.setType(Parameter.ParameterType.LIST);
    	cursorParam.setMode(Parameter.ParameterMode.OUTPUT);
    	sender.addParameter(cursorParam);

		sender.configure();
		sender.open();

		Message message = Message.nullMessage();

		// Act // Assert
		SenderException exception = assertThrows(SenderException.class, () -> sender.sendMessage(message, session));

		assertThat(exception.getMessage(), containsString("REF_CURSOR is not supported"));
	}

	@DatabaseTest
	public void testCallFunction() throws Exception {
		assumeTrue(dataSourceName.equalsIgnoreCase("Oracle"), "CALL to custom function only tested on Oracle so far");

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

	private int countRowsWithMessageValue(final String value, DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		String checkValueStatement = databaseTestEnvironment.getDbmsSupport().convertQuery("SELECT COUNT(*) FROM SP_TESTDATA WHERE TMESSAGE = ?", "Oracle");
		int rowsCounted;
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement statement = connection.prepareStatement(checkValueStatement)) {
			statement.setString(1, value);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				rowsCounted = resultSet.getInt(1);
			} else {
				rowsCounted = 0;
			}
		}
		connection.close();
		return rowsCounted;
	}

	private int countAllRows(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException, JdbcException {
		String checkValueStatement = databaseTestEnvironment.getDbmsSupport().convertQuery("SELECT COUNT(*) FROM SP_TESTDATA", "Oracle");
		int rowsCounted;
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement statement = connection.prepareStatement(checkValueStatement)) {
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				rowsCounted = resultSet.getInt(1);
			} else {
				rowsCounted = 0;
			}
		}
		connection.close();
		return rowsCounted;
	}

	private long insertRowWithMessageValue(final String value, DatabaseTestEnvironment databaseTestEnvironment) throws SQLException, JdbcException {
		String insertValueQuery = databaseTestEnvironment.getDbmsSupport().convertQuery("INSERT INTO SP_TESTDATA (TMESSAGE, TCHAR) VALUES (?, 'E')", "Oracle");
		// Column name of generated key-field should be in lowercase for PostgreSQL
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement statement = connection.prepareStatement(insertValueQuery, new String[]{"tkey"})) {
			statement.setString(1, value);
			statement.executeUpdate();
			ResultSet generatedKeys = statement.getGeneratedKeys();
			if (!generatedKeys.next()) {
				connection.close();
				fail("No generated keys from insert statement");
			}
			return generatedKeys.getLong(1);
		} finally {
			connection.close();
		}
	}

	private long insertRowWithBlobValue(final String value, final boolean compressed, DatabaseTestEnvironment databaseTestEnvironment) throws SQLException, JdbcException {
		String insertValueQuery = databaseTestEnvironment.getDbmsSupport().convertQuery("INSERT INTO SP_TESTDATA (TBLOB, TCHAR) VALUES (?, 'B')", "Oracle");
		// Column name of generated key-field should be in lowercase for PostgreSQL
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement statement = connection.prepareStatement(insertValueQuery, new String[]{"tkey"})) {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
			statement.setBlob(1, (compressed) ? new DeflaterInputStream(inputStream) : inputStream);
			statement.executeUpdate();
			ResultSet generatedKeys = statement.getGeneratedKeys();
			if (!generatedKeys.next()) {
				connection.close();
				fail("No generated keys from insert statement");
			}
			return generatedKeys.getLong(1);
		} finally {
			connection.close();
		}
	}

	private long insertRowWithClobValue(final String value, DatabaseTestEnvironment databaseTestEnvironment) throws SQLException, JdbcException {
		String insertValueQuery = databaseTestEnvironment.getDbmsSupport().convertQuery("INSERT INTO SP_TESTDATA (TCLOB, TCHAR) VALUES (?, 'C')", "Oracle");
		// Column name of generated key-field should be in lowercase for PostgreSQL
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement statement = connection.prepareStatement(insertValueQuery, new String[]{"tkey"})) {
			statement.setClob(1, new StringReader(value));
			statement.executeUpdate();
			ResultSet generatedKeys = statement.getGeneratedKeys();
			if (!generatedKeys.next()) {
				connection.close();
				fail("No generated keys from insert statement");
			}
			return generatedKeys.getLong(1);
		} finally {
			connection.close();
		}
	}

	private String getBlobValueAsString(final long id, DatabaseTestEnvironment databaseTestEnvironment) throws SQLException, JdbcException, IOException {
		String getBlobQuery = databaseTestEnvironment.getDbmsSupport().convertQuery("SELECT TBLOB FROM SP_TESTDATA WHERE TKEY = ?", "Oracle");
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement statement = connection.prepareStatement(getBlobQuery)) {
			statement.setLong(1, id);
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				connection.close();
				fail("No data found for id = " + id);
			}
			Blob blob = rs.getBlob(1);
			try (InputStream in = blob.getBinaryStream()) {
				return StreamUtil.streamToString(in);
			}
		} finally {
			connection.close();
		}
	}

	private String getClobValueAsString(final long id, DatabaseTestEnvironment databaseTestEnvironment) throws SQLException, JdbcException, IOException {
		String getClobQuery = databaseTestEnvironment.getDbmsSupport().convertQuery("SELECT TCLOB FROM SP_TESTDATA WHERE TKEY = ?", "Oracle");
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement statement = connection.prepareStatement(getClobQuery)) {
			statement.setLong(1, id);
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				connection.close();
				fail("No data found for id = " + id);
			}
			Clob clob = rs.getClob(1);
			try (Reader in = clob.getCharacterStream()) {
				return StreamUtil.readerToString(in, "\n");
			}
		} finally {
			connection.close();
		}
	}

	private static String cleanActualOutput(final SenderResult result) throws IOException {
		return result.getResult()
				.asString()
				.replaceAll("\\.0+<", "<")
				.replaceAll("(?ms)<fielddefinition>.+?</fielddefinition>", "<fielddefinition>IGNORE</fielddefinition>");
	}

	private static String loadOutputExpectation(final String file, final String messageContents) throws IOException {
		return TestFileUtils
				.getTestFile(file)
				.replace("MESSAGE-CONTENTS", messageContents);
	}

	private static String loadOutputExpectation(final String file, final String value, final long[] ids) throws IOException {
		String expectedOutput = loadOutputExpectation(file, value);
		StringBuilder sb = new StringBuilder();
		Matcher matcher = Pattern.compile("MSG-ID").matcher(expectedOutput);
		int idx = 0;
		while (matcher.find()) {
			matcher.appendReplacement(sb, String.valueOf(ids[idx++]));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
}
