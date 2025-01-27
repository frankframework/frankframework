package org.frankframework.jdbc;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterInputStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.JdbcException;
import org.frankframework.parameters.NumberParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.stream.Message;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.StreamUtil;

@WithLiquibase(tableName = StoredProcedureQuerySenderTest.TABLE_NAME, file = "Jdbc/StoredProcedureQuerySender/DatabaseChangelog-StoredProcedures.xml")
@Log4j2
public class StoredProcedureQuerySenderTest {

	public static final String TABLE_NAME = "SP_TESTDATA";
	public static final String TEST_DATA_STRING = "tést-data";

	private StoredProcedureQuerySender sender;

	private PipeLineSession session;

	private Dbms databaseUnderTest;

	@BeforeEach
	public void setUp(DatabaseTestEnvironment databaseTestEnvironment) {
		databaseUnderTest = databaseTestEnvironment.getDbmsSupport().getDbms();

		sender = databaseTestEnvironment.getConfiguration().createBean(StoredProcedureQuerySender.class);
		sender.setSqlDialect("Oracle");
		sender.setDatasourceName(databaseTestEnvironment.getDataSourceName());

		session = new PipeLineSession();
	}

	@AfterEach
	public void tearDown(DatabaseTestEnvironment databaseTestEnvironment) {
		if (session != null) {
			session.close();
		}
	}

	@DatabaseTest
	public void testSimpleStoredProcedureNoResultNoParameters(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		assumeFalse(Dbms.H2 == databaseUnderTest, "H2 driver gives incorrect results for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		sender.setQuery("CALL INSERT_MESSAGE('" + value + "', 'P')");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);

		sender.configure();
		sender.start();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		// Due to differences between databases, "rows updated" is sometimes 1, sometimes 0
		assertTrue(result.getResult().asString().startsWith("<result><rowsupdated>"), "Result should start with [<result><rowsupdated>]");

		// Check presence of row that should have been inserted
		int rowsCounted = countRowsWithMessageValue(value, databaseTestEnvironment);

		assertEquals(1, rowsCounted);
	}

	@DatabaseTest
	public void testSimpleStoredProcedureResultQueryNoParameters() throws Exception {
		// Arrange
		String value = UUID.randomUUID().toString();
		sender.setQuery("CALL INSERT_MESSAGE('" + value + "', 'P')");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setResultQuery("SELECT COUNT(*) FROM SP_TESTDATA WHERE TMESSAGE = '" + value + "'");
		sender.setScalar(true);

		sender.configure();
		sender.start();

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
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setResultQuery("SELECT COUNT(*) FROM SP_TESTDATA WHERE TMESSAGE = '" + value + "'");
		sender.setScalar(true);

		Parameter parameter = new Parameter("message", value);
		sender.addParameter(parameter);

		sender.configure();
		sender.start();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());

		assertEquals("1", result.getResult().asString());
	}

	@DatabaseTest
	public void testSimpleStoredProcedureBlobInputParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(Set.of(Dbms.H2, Dbms.POSTGRESQL, Dbms.DB2).contains(databaseUnderTest), "H2, PSQL, DB2 not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);
		sender.setQuery("CALL SET_BLOB(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		NumberParameter p1 = NumberParameterBuilder.create("id", id);
		sender.addParameter(p1);
		Parameter p2 = new Parameter("data", null);
		p2.setSessionKey("data");
		p2.setType(ParameterType.BINARY);
		sender.addParameter(p2);
		session.put("data", new ByteArrayInputStream(TEST_DATA_STRING.getBytes(StandardCharsets.UTF_8)));

		sender.configure();
		sender.start();

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
		assumeFalse(Set.of(Dbms.H2, Dbms.POSTGRESQL, Dbms.DB2).contains(databaseUnderTest), "H2, PSQL, DB2 not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);
		sender.setQuery("CALL SET_CLOB(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		NumberParameter p1 = NumberParameterBuilder.create("id", id);
		sender.addParameter(p1);
		Parameter p2 = new Parameter("data", null);
		p2.setSessionKey("data");
		p2.setType(ParameterType.CHARACTER);
		sender.addParameter(p2);
		session.put("data", new StringReader(TEST_DATA_STRING));

		sender.configure();
		sender.start();

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
		assumeFalse(Set.of(Dbms.H2, Dbms.POSTGRESQL, Dbms.DB2).contains(databaseUnderTest), "H2, PSQL, DB2 not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);
		sender.setQuery("CALL SET_CLOB(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		NumberParameter p1 = NumberParameterBuilder.create("id", id);
		sender.addParameter(p1);
		Parameter p2 = new Parameter("data", null);
		p2.setSessionKey("data");
		p2.setType(ParameterType.CHARACTER);
		sender.addParameter(p2);
		Message message1 = new Message(new StringReader(TEST_DATA_STRING));
		message1.getContext().withSize(TEST_DATA_STRING.getBytes().length);
		session.put("data", message1);

		sender.configure();
		sender.start();

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

		assumeFalse(Dbms.H2 == databaseUnderTest, "H2 does not support OUT parameters, skipping test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);

		sender.setQuery("CALL GET_MESSAGE_BY_ID(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(ParameterType.STRING);
		sender.addParameter(outParam1);

		sender.configure();
		sender.start();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		assertEquals(value, result.getResult().asString());
	}

	@DatabaseTest
	public void testStoredProcedureInputAndOutputParameterNullValue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(Dbms.H2 == databaseUnderTest, "H2 does not support OUT parameters, skipping test case");

		// Arrange
		long id = insertRowWithMessageValue(null, databaseTestEnvironment);

		sender.setQuery("CALL GET_MESSAGE_BY_ID(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(ParameterType.STRING);
		sender.addParameter(outParam1);

		sender.configure();
		sender.start();

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

		assumeFalse(Set.of(Dbms.H2, Dbms.POSTGRESQL).contains(databaseUnderTest), "H2, PSQL not supported for this test case");


		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithBlobValue(value, compressed, databaseTestEnvironment);

		sender.setQuery("CALL GET_BLOB(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);
		sender.setBlobSmartGet(blobSmartGet);
		sender.setBlobCharset(charSet);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(ParameterType.BINARY);
		sender.addParameter(outParam1);

		sender.configure();
		sender.start();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		assertEquals(value, result.getResult().asString());
	}

	@DatabaseTest
	public void testStoredProcedureClobOutputParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(Set.of(Dbms.H2, Dbms.POSTGRESQL).contains(databaseUnderTest), "H2, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithClobValue(value, databaseTestEnvironment);

		sender.setQuery("CALL GET_CLOB(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(ParameterType.CHARACTER);
		sender.addParameter(outParam1);

		sender.configure();
		sender.start();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		assertEquals(value, result.getResult().asString());
	}

	@DatabaseTest
	public void testStoredProcedureBlobOutputParameterNullValue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(Set.of(Dbms.H2, Dbms.POSTGRESQL).contains(databaseUnderTest), "H2, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);

		sender.setQuery("CALL GET_BLOB(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(ParameterType.BINARY);
		sender.addParameter(outParam1);

		sender.configure();
		sender.start();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		assertTrue(result.getResult().isRequestOfType(byte[].class));
		byte[] bytes = result.getResult().asByteArray();
		assertNotNull(bytes);
		assertEquals(0, bytes.length);
		assertEquals("", result.getResult().asString());
	}

	@DatabaseTest
	public void testStoredProcedureClobOutputParameterNullValue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(Set.of(Dbms.H2, Dbms.POSTGRESQL).contains(databaseUnderTest), "H2, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);

		sender.setQuery("CALL GET_CLOB(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(ParameterType.CHARACTER);
		sender.addParameter(outParam1);

		sender.configure();
		sender.start();

		Message message = Message.nullMessage();

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertTrue(result.isSuccess());
		assertTrue(StringUtils.isEmpty(result.getResult().asString()));
	}

	@DatabaseTest
	public void testStoredProcedureInputAndOutputParametersXmlOutput(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		assumeFalse(Dbms.H2 == databaseUnderTest, "H2 does not support OUT parameters, skipping test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long id = insertRowWithMessageValue(value, databaseTestEnvironment);

		sender.setQuery("CALL GET_MESSAGE_AND_TYPE_BY_ID(?, ?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);

		Parameter inParam = new Parameter("id", String.valueOf(id));
		sender.addParameter(inParam);

		Parameter outParam1 = new Parameter("r1", null);
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		outParam1.setType(ParameterType.STRING);
		sender.addParameter(outParam1);

		Parameter outParam2 = new Parameter("r2", null);
		outParam2.setMode(Parameter.ParameterMode.OUTPUT);
		outParam2.setType(ParameterType.STRING);
		sender.addParameter(outParam2);

		sender.configure();
		sender.start();

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

		assumeFalse(Dbms.H2 == databaseUnderTest, "H2 does not support OUT parameters, skipping test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		insertRowWithMessageValue(value, databaseTestEnvironment);

		sender.setQuery("CALL COUNT_MESSAGES_BY_CONTENT(?, ?, ?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);

		Parameter inParam = new Parameter("message", value);
		inParam.setMode(Parameter.ParameterMode.INOUT);
		sender.addParameter(inParam);

		NumberParameter outParam1 = NumberParameterBuilder.create("r1");
		outParam1.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(outParam1);

		NumberParameter outParam2 = NumberParameterBuilder.create("r2");
		outParam2.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(outParam2);

		NumberParameter outParam3 = NumberParameterBuilder.create("r3");
		// Setting these makes it a type NUMBER (which translates to Number, Double, in the JDBC mappings, so we test that code-path)
		outParam3.setDecimalSeparator(".");
		outParam3.setGroupingSeparator(",");
		outParam3.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(outParam3);

		sender.configure();
		sender.start();

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
		assumeFalse(Set.of(Dbms.ORACLE, Dbms.POSTGRESQL).contains(databaseUnderTest), "Oracle, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
		}

		sender.setQuery("CALL GET_MESSAGES_BY_CONTENT(?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);

		sender.configure();
		sender.start();

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
		assumeFalse(Set.of(Dbms.ORACLE, Dbms.POSTGRESQL).contains(databaseUnderTest), "Oracle, PSQL not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
		}

		sender.setQuery("CALL GET_MESSAGES_BY_CONTENT(?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);

		sender.configure();
		sender.start();

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
		assumeFalse(Set.of(Dbms.H2, Dbms.ORACLE, Dbms.POSTGRESQL).contains(databaseUnderTest), "H2, PSQL, Oracle not supported for this test case");

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
		}

		sender.setQuery("CALL GET_MESSAGES_AND_COUNT(?, ?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);
		NumberParameter count = new NumberParameter();
		count.setName("count");
		count.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(count);

		sender.configure();
		sender.start();

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
		assumeTrue(Dbms.ORACLE == databaseUnderTest, "REFCURSOR not supported, skipping test");

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
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);

		Parameter cursorParam = new Parameter();
		cursorParam.setName("cursor1");
		cursorParam.setType(ParameterType.LIST);
		cursorParam.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(cursorParam);

		sender.configure();
		sender.start();

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
		assumeTrue(Dbms.ORACLE == databaseUnderTest, "REFCURSOR not supported, skipping test");

		// Arrange
		String value = UUID.randomUUID().toString();
		long[] ids = new long[5];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = insertRowWithMessageValue(value, databaseTestEnvironment);
		}

		sender.setQuery("CALL GET_MESSAGES_CURSOR(?,?,?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);
		NumberParameter countParam = new NumberParameter();
		countParam.setName("count");
		countParam.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(countParam);
		Parameter cursorParam = new Parameter();
    	cursorParam.setName("cursor1");
    	cursorParam.setType(ParameterType.LIST);
    	cursorParam.setMode(Parameter.ParameterMode.OUTPUT);
    	sender.addParameter(cursorParam);

		sender.configure();
		sender.start();

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
		assumeTrue(Dbms.MSSQL == databaseUnderTest);

		// Arrange
		String value = UUID.randomUUID().toString();
		sender.setQuery("CALL GET_MESSAGES_CURSOR(?,?,?)");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);

		Parameter parameter = new Parameter("content", value);
		sender.addParameter(parameter);
		NumberParameter countParam = new NumberParameter();
		countParam.setName("count");
		countParam.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(countParam);
		Parameter cursorParam = new Parameter();
    	cursorParam.setName("cursor1");
    	cursorParam.setType(ParameterType.LIST);
    	cursorParam.setMode(Parameter.ParameterMode.OUTPUT);
    	sender.addParameter(cursorParam);

		sender.configure();
		sender.start();

		Message message = Message.nullMessage();

		// Act // Assert
		SenderException exception = assertThrows(SenderException.class, () -> sender.sendMessage(message, session));

		assertThat(exception.getMessage(), containsString("REF_CURSOR is not supported"));
	}

	@DatabaseTest
	public void testCallFunction() throws Exception {
		assumeTrue(Dbms.ORACLE == databaseUnderTest, "CALL to custom function only tested on Oracle so far");

		// Arrange
		sender.setQuery("{ ? = call add_numbers(?, ?) }");
		sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
		sender.setScalar(true);

		// NB: All these parameters were originally INTEGER. Perhaps need new type IntegerParameter?
		NumberParameter resultParam = NumberParameterBuilder.create("result", 0);
		resultParam.setMode(Parameter.ParameterMode.OUTPUT);
		sender.addParameter(resultParam);
		NumberParameter p1 = NumberParameterBuilder.create("one", 1);
		sender.addParameter(p1);
		NumberParameter p2 = NumberParameterBuilder.create("two", 2);
		sender.addParameter(p2);

		sender.configure();
		sender.start();

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
			statement.setBlob(1, compressed ? new DeflaterInputStream(inputStream) : inputStream);
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
