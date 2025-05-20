package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.DbmsSupportFactory;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.parameters.DateParameter.DateFormatType;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterType;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.testutil.DateParameterBuilder;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;
import org.frankframework.testutil.ThrowingAfterCloseReader;
import org.frankframework.testutil.VirtualInputStream;
import org.frankframework.testutil.VirtualReader;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.SaxElementBuilder;
import org.frankframework.xml.XmlWriter;

public class JdbcUtilTest {
	private static final String H2_CONNECTION_STRING = "jdbc:h2:mem:test";

	private Connection connection;
	private IDbmsSupport dbmsSupport;

	@BeforeEach
	public void startDatabase() throws SQLException, JdbcException {
		connection = DriverManager.getConnection(H2_CONNECTION_STRING);
		dbmsSupport = new DbmsSupportFactory().getDbmsSupport(connection);
		if (dbmsSupport.isTablePresent(connection, "TEMP")) {
			connection.createStatement().execute("DROP TABLE TEMP");
		}
		connection.createStatement().execute("CREATE TABLE TEMP(TKEY INT PRIMARY KEY, TVARCHAR VARCHAR(100), TVARCHAR2 VARCHAR(100), TINT INT, TDATETIME DATETIME, TBLOB BLOB, TCLOB CLOB)");
	}

	@AfterEach
	public void closeDatabase() throws SQLException {
		if (connection != null) {
			connection.createStatement().execute("DROP TABLE TEMP");
			connection.close();
		}
	}


	@SuppressWarnings({"DataFlowIssue", "unchecked"})
	@Test
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
		query = "INSERT INTO TEMP (TKEY, TVARCHAR2, TDATETIME) VALUES (3, 'just a third text', PARSEDATETIME('2018-04-12 03:05:06', 'yyyy-MM-dd HH:mm:ss'))";

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
		JdbcTestUtil.executeStatement(dbmsSupport, connection, query, parameterValues, session);

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
		assertEquals("2018-04-12 03:05:06", stringResult);

		// Arrange
		query = "SELECT TVARCHAR FROM TEMP WHERE TKEY = 4";

		// Act
		stringResult = JdbcTestUtil.executeStringQuery(connection, query);

		// Assert
		assertEquals("fourth text", stringResult);

		// Assert
		stringResult = JdbcTestUtil.selectAllFromTable(dbmsSupport, connection, "TEMP", "TKEY");
		System.out.println(stringResult);
		MatchUtils.assertXmlEquals(TestFileUtils.getTestFile("/JdbcUtil/expected.xml"), stringResult);
		//compareXML("JdbcUtil/expected.xml", stringResult);

		// Arrange
		query = "SELECT TVARCHAR2, TDATETIME FROM TEMP WHERE TKEY = ?";
		params = new ParameterList();
		params.add(ParameterBuilder.create().withValue("3"));

		// Act
		List<Object> listResult = (List<Object>) JdbcTestUtil.executeQuery(dbmsSupport, connection, query, ParameterBuilder.getPVL(params), session);

		// Assert
		assertEquals("just a third text", listResult.get(0));
		assertEquals("2018-04-12 03:05:06.0", listResult.get(1).toString());

		// Arrange
		query = "SELECT COUNT(*) FROM TEMP";

		// Act
		long result = (Long) JdbcTestUtil.executeQuery(dbmsSupport, connection, query, null, session);

		// Assert
		assertEquals(5, result);
	}

	@Test
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
		JdbcTestUtil.executeStatement(dbmsSupport, connection, query, parameterValues, session);

		// Assert
		ParameterList resultParams = new ParameterList();
		resultParams.add(NumberParameterBuilder.create().withValue(1));

		Object result = JdbcTestUtil.executeQuery(dbmsSupport, connection, "SELECT TBLOB FROM TEMP WHERE TKEY = ?",
				ParameterBuilder.getPVL(resultParams), session);

		assertNotNull(result);

		Blob blob = (Blob) result;
		assertEquals(20_000, blob.length());
	}

	@Test
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
		JdbcTestUtil.executeStatement(dbmsSupport, connection, query, parameterValues, session);

		// Assert
		ParameterList resultParams = new ParameterList();
		resultParams.add(NumberParameterBuilder.create().withValue(1));

		List<Object> result = (List<Object>) JdbcTestUtil.executeQuery(dbmsSupport, connection, "SELECT TCLOB, TBLOB FROM TEMP WHERE TKEY = ?", ParameterBuilder.getPVL(resultParams), session);

		Clob clob = (Clob) result.get(0);
		Blob blob = (Blob) result.get(1);

		assertEquals(20_000, clob.length());
		assertEquals(20_000, blob.length());
	}

	@Test
	public void testWarningsToString() {
		String expected = getExpectedWarningXml();
		String actual = JdbcUtil.warningsToString(getWarnings());
		MatchUtils.assertXmlEquals(expected,actual);
	}

	@Test
	public void testWarningsToXml() throws SAXException {
		String expected = getExpectedWarningXml();
		XmlWriter writer = new XmlWriter();
		PrettyPrintFilter ppf = new PrettyPrintFilter(writer);
		try (SaxElementBuilder seb = new SaxElementBuilder(ppf)) {
			JdbcUtil.warningsToXml(getWarnings(), seb);
			MatchUtils.assertXmlEquals(expected,writer.toString());
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

}
