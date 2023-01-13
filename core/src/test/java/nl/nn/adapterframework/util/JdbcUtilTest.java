package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.SaxElementBuilder;
import nl.nn.adapterframework.xml.XmlWriter;

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
		connection.createStatement().execute("CREATE TABLE TEMP(TKEY INT PRIMARY KEY, TVARCHAR VARCHAR(100), TVARCHAR2 VARCHAR(100), TINT INT, TDATETIME DATETIME)");
	}

	@AfterEach
	public void closeDatabase() throws SQLException {
		if (connection != null) {
			connection.createStatement().execute("DROP TABLE TEMP");
			connection.close();
		}
	}


	@Test
	public void test() throws Exception {
		String query = "INSERT INTO TEMP (TKEY, TVARCHAR, TINT) VALUES (1, 'just a text', 1793)";
		JdbcUtil.executeStatement(connection, query);

		query = "INSERT INTO TEMP (TKEY, TVARCHAR, TINT) VALUES (2, 'just a second text', 96)";
		JdbcUtil.executeStatement(connection, query);

		query = "INSERT INTO TEMP (TKEY, TVARCHAR2, TDATETIME) VALUES (3, 'just a third text', PARSEDATETIME('2018-04-12 03:05:06', 'yyyy-MM-dd HH:mm:ss'))";
		JdbcUtil.executeStatement(connection, query);

		query = "INSERT INTO TEMP (TKEY, TVARCHAR) VALUES (?, ?)";
		JdbcUtil.executeStatement(connection, query, 4, "fourth text");

		query = "INSERT INTO TEMP (TKEY, TVARCHAR, TINT, TDATETIME) VALUES (?, ?, ?, ?)";
		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withValue("6").withType(ParameterType.INTEGER));
		params.add(ParameterBuilder.create().withValue("5th text"));
		params.add(ParameterBuilder.create().withValue("15092002").withType(ParameterType.INTEGER));
		params.add(ParameterBuilder.create().withValue("2018-04-12 03:05:06").withType(ParameterType.DATETIME));
		JdbcUtil.executeStatement(dbmsSupport, connection, query, ParameterBuilder.getPVL(params));

		query = "SELECT COUNT(*) FROM TEMP";
		int intResult = JdbcUtil.executeIntQuery(connection, query);
		assertEquals(5, intResult);

		query = "SELECT TDATETIME FROM TEMP WHERE TKEY = 3";
		String stringResult = JdbcUtil.executeStringQuery(connection, query);
		assertEquals("2018-04-12 03:05:06", stringResult);

		query = "SELECT TVARCHAR FROM TEMP WHERE TKEY = 4";
		stringResult = JdbcUtil.executeStringQuery(connection, query);
		assertEquals("fourth text", stringResult);

		stringResult = JdbcUtil.selectAllFromTable(dbmsSupport, connection, "TEMP", "TKEY");
		System.out.println(stringResult);
		MatchUtils.assertXmlEquals(TestFileUtils.getTestFile("/JdbcUtil/expected.xml"), stringResult);
		//compareXML("JdbcUtil/expected.xml", stringResult);

		query = "SELECT TVARCHAR2, TDATETIME FROM TEMP WHERE TKEY = ?";
		params = new ParameterList();
		params.add(ParameterBuilder.create().withValue("3").withType(ParameterType.INTEGER));
		List<Object> listResult = (List<Object>) JdbcUtil.executeQuery(dbmsSupport, connection, query, ParameterBuilder.getPVL(params));
		assertEquals("just a third text", listResult.get(0));
		assertEquals("2018-04-12 03:05:06.0", listResult.get(1).toString());

		query = "SELECT COUNT(*) FROM TEMP";
		long result = (Long)JdbcUtil.executeQuery(dbmsSupport, connection, query, null);
		assertEquals(5, result);
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
		return "<warnings>\n"+
				"<warning errorCode=\"111\" sqlState=\"SQLState 1\" cause=\"java.lang.NullPointerException\" message=\"warningReason1: tja\" />\n"+
				"<warning errorCode=\"222\" sqlState=\"SQLState 2\" cause=\"java.lang.NullPointerException\" message=\"warningReason2: och\" />\n"+
				"</warnings>";
	}
	
	private SQLWarning getWarnings() {
		SQLWarning warning1 = new SQLWarning("warningReason1", "SQLState 1", 111, new NullPointerException("tja"));
		SQLWarning warning2 = new SQLWarning("warningReason2", "SQLState 2", 222, new NullPointerException("och"));
		warning1.setNextWarning(warning2);
		return warning1;
	}
	
}
