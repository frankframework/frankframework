package org.frankframework.util;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.xml.sax.SAXException;

import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.parameters.DateParameter.DateFormatType;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterType;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.testutil.DateParameterBuilder;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;
import org.frankframework.testutil.ThrowingAfterCloseReader;
import org.frankframework.testutil.VirtualInputStream;
import org.frankframework.testutil.VirtualReader;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.SaxElementBuilder;
import org.frankframework.xml.XmlWriter;

@SuppressWarnings("SqlResolve")
@WithLiquibase(tableName = "TEMP", file = "JdbcUtil/JdbcUtilTest.xml")
public class JdbcUtilTest {
	private Connection connection;
	private IDbmsSupport dbmsSupport;
	private Dbms databaseUnderTest;

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) {
		databaseUnderTest = databaseTestEnvironment.getDbmsSupport().getDbms();
		dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		connection = databaseTestEnvironment.getConnection();
	}

	@AfterEach
	public void teardown() {
		CloseUtils.closeSilently(connection);
	}

	@SuppressWarnings({ "DataFlowIssue", "unchecked", "deprecation" })
	@DatabaseTest
	public void testExecuteStatementAndQuery() throws Exception {
		// Arrange
		String query = "INSERT INTO TEMP (TKEY, TVARCHAR, TINT) VALUES (1, 'just a text', 1793)";

		// Act
		JdbcTestUtil.executeStatement(connection, query);

		// Arrange
		query = "INSERT INTO TEMP (TKEY, TVARCHAR, TINT) VALUES (2, 'just a second text', 96)";

		// Act
		JdbcTestUtil.executeStatement(connection, query);

		// Arrange
		Date date = DateFormatUtils.parseAnyDate("2018-04-12 03:05:06");
		query = "INSERT INTO TEMP (TKEY, TVARCHAR2, TDATETIME) VALUES (3, 'just a third text', " + dbmsSupport.getDatetimeLiteral(date) + ")";

		// Act
		JdbcTestUtil.executeStatement(connection, query);

		// Arrange
		query = "INSERT INTO TEMP (TKEY, TVARCHAR) VALUES (?, ?)";

		// Act
		JdbcTestUtil.executeStatement(connection, query, 4, "fourth text");

		// Arrange
		query = "INSERT INTO TEMP (TKEY, TVARCHAR, TINT, TDATETIME, TCLOB, TBLOB) VALUES (?, ?, ?, ?, ?, ?)";
		ParameterList params = new ParameterList();
		params.add(NumberParameterBuilder.create().withValue(6));
		params.add(ParameterBuilder.create().withValue("5th text"));
		params.add(ParameterBuilder.create().withValue("15092002"));
		params.add(DateParameterBuilder.create().withValue("2018-04-12 03:05:06").withFormatType(DateFormatType.DATETIME));
		params.add(ParameterBuilder.create().withSessionKey("clobParam").withType(ParameterType.CHARACTER));
		params.add(ParameterBuilder.create().withSessionKey("blobParam").withType(ParameterType.BINARY));

		PipeLineSession session = new PipeLineSession();
		session.put("clobParam", new ThrowingAfterCloseReader(new StringReader("Reader on a String")));
		session.put("blobParam", new ThrowingAfterCloseInputStream(new ByteArrayInputStream("Stream on a String".getBytes(Charset.defaultCharset()))));

		params.configure();
		ParameterValueList parameterValues = params.getValues(Message.nullMessage(), session);

		// Act
		JdbcTestUtil.executeStatement(dbmsSupport, connection, query, parameterValues);

		// Arrange
		query = "SELECT COUNT(*) FROM TEMP";

		// Act
		int intResult = JdbcTestUtil.executeIntQuery(connection, query);

		// Assert
		assertEquals(5, intResult);

		// Arrange
		query = "SELECT TDATETIME FROM TEMP WHERE TKEY = 3";

		// Act
		String stringResult = JdbcTestUtil.executeStringQuery(connection, query);

		// Assert
		if (databaseUnderTest == Dbms.MYSQL) {
			assertEquals("2018-04-12T03:05:06", stringResult);
		} else {
			assertEquals("2018-04-12 03:05:06.0", stringResult);
		}
		// Arrange
		query = "SELECT TVARCHAR FROM TEMP WHERE TKEY = 4";

		// Act
		stringResult = JdbcTestUtil.executeStringQuery(connection, query);

		// Assert
		assertEquals("fourth text", stringResult);

		// Assert
		stringResult = JdbcTestUtil.selectAllFromTable(dbmsSupport, connection, "TEMP", "TKEY");
		System.out.println(stringResult);
		String expected = TestFileUtils.getTestFile("/JdbcUtil/expected.xml");
		// compareXML("JdbcUtil/expected.xml", stringResult);
		assertXmlEquals(ignoreFieldDefinitions(expected), ignoreFieldDefinitions(stringResult));

		// Arrange
		query = "SELECT TVARCHAR2, TDATETIME FROM TEMP WHERE TKEY = ?";
		params = new ParameterList();
		params.add(ParameterBuilder.create().withValue("3"));

		// Act
		List<Object> listResult = (List<Object>) JdbcTestUtil.executeQuery(dbmsSupport, connection, query, ParameterBuilder.getPVL(params));

		// Assert
		assertEquals("just a third text", listResult.getFirst());
		if (databaseUnderTest == Dbms.MYSQL) {
			assertEquals("2018-04-12T03:05:06", listResult.get(1).toString());
		} else {
			assertEquals("2018-04-12 03:05:06.0", listResult.get(1).toString());
		}

		// Arrange
		query = "SELECT COUNT(*) FROM TEMP";

		// Act
		Object result = JdbcTestUtil.executeQuery(dbmsSupport, connection, query, null);

		// Assert
		assertNumericValue(result, 5);
	}

	private String ignoreFieldDefinitions(String xml) {
		return xml.replaceAll("(?s)(?<=<fielddefinition>)(.*?)(?=</fielddefinition>)", "IGNORED");
	}

	@DatabaseTest
	public void testBytesCase() throws Exception {
		// Arrange
		String query = "INSERT INTO TEMP (TKEY, TBLOB) VALUES (?, ?)";

		ParameterList params = new ParameterList();
		params.add(NumberParameterBuilder.create().withValue(1));
		params.add(ParameterBuilder.create().withSessionKey("binaryParam").withType(ParameterType.BINARY));

		PipeLineSession session = new PipeLineSession();
		session.put("binaryParam", new ThrowingAfterCloseInputStream(new VirtualInputStream(20_000)));

		params.configure();
		ParameterValueList parameterValues = params.getValues(Message.nullMessage(), session);

		// Act
		JdbcTestUtil.executeStatement(dbmsSupport, connection, query, parameterValues);

		// Assert
		ParameterList resultParams = new ParameterList();
		resultParams.add(NumberParameterBuilder.create().withValue(1));

		Object result = JdbcTestUtil.executeQuery(dbmsSupport, connection, "SELECT TBLOB FROM TEMP WHERE TKEY = ?",
				ParameterBuilder.getPVL(resultParams));

		assertNotNull(result);
		assertBlobSize(result, 20_000);
	}

	@DatabaseTest
	@SuppressWarnings("unchecked")
	public void testLargeLobs() throws Exception {
		// Arrange
		String query = "INSERT INTO TEMP (TKEY, TCLOB, TBLOB) VALUES (?, ?, ?)";

		ParameterList params = new ParameterList();
		params.add(NumberParameterBuilder.create().withValue(1));
		params.add(ParameterBuilder.create().withSessionKey("clobParam").withType(ParameterType.CHARACTER));
		params.add(ParameterBuilder.create().withSessionKey("blobParam").withType(ParameterType.BINARY));

		PipeLineSession session = new PipeLineSession();
		session.put("clobParam", new ThrowingAfterCloseReader(new VirtualReader(20_000)));
		session.put("blobParam", new ThrowingAfterCloseInputStream(new VirtualInputStream(20_000)));

		params.configure();
		ParameterValueList parameterValues = params.getValues(Message.nullMessage(), session);

		// Act
		JdbcTestUtil.executeStatement(dbmsSupport, connection, query, parameterValues);

		// Assert
		ParameterList resultParams = new ParameterList();
		resultParams.add(NumberParameterBuilder.create().withValue(1));

		List<Object> result = (List<Object>) JdbcTestUtil.executeQuery(dbmsSupport, connection, "SELECT TCLOB, TBLOB FROM TEMP WHERE TKEY = ?", ParameterBuilder.getPVL(resultParams));

		assertNotNull(result);
		assertClobSize(result.getFirst(), 20_000);
		assertBlobSize(result.get(1), 20_000);
	}

	@SuppressWarnings("deprecation")
	@DatabaseTest
	public void testWarningsToString() {
		String expected = getExpectedWarningXml();
		String actual = JdbcUtil.warningsToString(getWarnings());
		assertXmlEquals(expected,actual);
	}

	@DatabaseTest
	public void testWarningsToXml() throws SAXException {
		String expected = getExpectedWarningXml();
		XmlWriter writer = new XmlWriter();
		PrettyPrintFilter ppf = new PrettyPrintFilter(writer);
		try (SaxElementBuilder seb = new SaxElementBuilder(ppf)) {
			JdbcUtil.warningsToXml(getWarnings(), seb);
			assertXmlEquals(expected,writer.toString());
		}
	}

	@DatabaseTest
	public void testSetParameterVarchar() throws Exception {
		// Arrange
		try (PreparedStatement statement = connection.prepareStatement("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES (1002, ?)")) {

			// Act // Assert
			assertDoesNotThrow(() -> JdbcUtil.setParameter(statement, 1, "1", dbmsSupport.isParameterTypeMatchSupported()));
			assertDoesNotThrow((ThrowingSupplier<Boolean>) statement::execute);
		}
	}

	@DatabaseTest
	public void testSetParameterInt() throws Exception {
		// Arrange
		try (PreparedStatement statement = connection.prepareStatement("INSERT INTO TEMP (TKEY, TINT) VALUES (1001, ?)")) {

			// Act // Assert
			assertDoesNotThrow(() -> JdbcUtil.setParameter(statement, 1, "1", dbmsSupport.isParameterTypeMatchSupported()));
			assertDoesNotThrow((ThrowingSupplier<Boolean>) statement::execute);
		}
	}

	@DatabaseTest
	public void testSetParameterBlob() throws Exception {
		// Arrange
		try (PreparedStatement statement = connection.prepareStatement("INSERT INTO TEMP (TKEY, TBLOB) VALUES (1003, ?)")) {

			// Act // Assert
			assertDoesNotThrow(() -> JdbcUtil.setParameter(statement, 1, "1", dbmsSupport.isParameterTypeMatchSupported()));
			assertDoesNotThrow((ThrowingSupplier<Boolean>) statement::execute);
		}
	}

	@DatabaseTest
	public void testSetParameterClob() throws Exception {
		// Arrange
		try (PreparedStatement statement = connection.prepareStatement("INSERT INTO TEMP (TKEY, TCLOB) VALUES (1004, ?)")) {

			// Act // Assert
			assertDoesNotThrow(() -> JdbcUtil.setParameter(statement, 1, "1", dbmsSupport.isParameterTypeMatchSupported()));
			assertDoesNotThrow((ThrowingSupplier<Boolean>) statement::execute);
		}
	}

	private String getExpectedWarningXml() {
		return """
				<warnings>
				<warning errorCode="111" sqlState="SQLState 1" cause="java.lang.NullPointerException" message="warningReason1: tja" />
				<warning errorCode="222" sqlState="SQLState 2" cause="java.lang.NullPointerException" message="warningReason2: och" />
				</warnings>\
				""";
	}

	private SQLWarning getWarnings() {
		SQLWarning warning1 = new SQLWarning("warningReason1", "SQLState 1", 111, new NullPointerException("tja"));
		SQLWarning warning2 = new SQLWarning("warningReason2", "SQLState 2", 222, new NullPointerException("och"));
		warning1.setNextWarning(warning2);
		return warning1;
	}

	private static void assertBlobSize(Object result, int expectedLength) throws SQLException {
		if (result instanceof Blob blob) {
			assertEquals(expectedLength, blob.length());
		} else if (result instanceof byte[] bytes) {
			assertEquals(expectedLength, bytes.length);
		} else {
			fail("result is not instance of Blob or byte[]: " + result);
		}
	}

	private static void assertClobSize(Object result, int expectedLength) throws SQLException {
		if (result instanceof Clob clob) {
			assertEquals(expectedLength, clob.length());
		} else if (result instanceof char[] chars) {
			assertEquals(expectedLength, chars.length);
		} else if (result instanceof String string) {
			assertEquals(expectedLength, string.length());
		} else {
			fail("result is not instance of Clob or char[]: " + result);
		}
	}

	private void assertNumericValue(Object result, long expectedValue) throws SQLException {
		if (result instanceof Number number) {
			assertEquals(expectedValue, number.longValue());
		} else {
			fail("result is not instance of Number: " + result);
		}
	}
}
