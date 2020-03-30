package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.parameters.SimpleParameter;

public class JdbcUtilTest {
	private static final String H2_CONNECTION_STRING = "jdbc:h2:mem:test";

	private Connection connection;

	@Before
	public void startDatabase() throws SQLException {
		connection = DriverManager.getConnection(H2_CONNECTION_STRING);
		connection.createStatement().execute(
				"CREATE TABLE TEMP(TKEY INT PRIMARY KEY, TVARCHAR VARCHAR(100), TVARCHAR2 VARCHAR(100), TINT INT, TDATETIME DATETIME)");
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
		ParameterValueList params = new ParameterValueList();
		params.add(new SimpleParameter(null, Parameter.TYPE_INTEGER, new Integer(6)));
		params.add(new SimpleParameter(null, null, "5th text"));
		params.add(new SimpleParameter(null, Parameter.TYPE_INTEGER, new Integer(15092002)));
		params.add(new SimpleParameter(null, Parameter.TYPE_DATETIME, DateUtils.parseToDate("2018-04-12 03:05:06", "yyyy-MM-dd HH:mm:ss")));
		JdbcUtil.executeStatement(connection, query, params);

		query = "SELECT COUNT(*) FROM TEMP";
		int intResult = JdbcUtil.executeIntQuery(connection, query);
		assertEquals(5, intResult);

		query = "SELECT TDATETIME FROM TEMP WHERE TKEY = 3";
		String stringResult = JdbcUtil.executeStringQuery(connection, query);
		assertEquals("2018-04-12 03:05:06.0", stringResult);

		query = "SELECT TVARCHAR FROM TEMP WHERE TKEY = 4";
		stringResult = JdbcUtil.executeStringQuery(connection, query);
		assertEquals("fourth text", stringResult);

		stringResult = JdbcUtil.selectAllFromTable(connection, "TEMP", "TKEY");
		compareXML("JdbcUtil/expected.xml", stringResult);

		query = "SELECT TVARCHAR2, TDATETIME FROM TEMP WHERE TKEY = ?";
		params = new ParameterValueList();
		params.add(new SimpleParameter(null, Parameter.TYPE_INTEGER, new Integer(3)));
		List<Object> listResult = (List<Object>) JdbcUtil.executeQuery(connection, query, params);
		assertEquals("just a third text", listResult.get(0));
		assertEquals("2018-04-12 03:05:06.0", listResult.get(1).toString());

		query = "SELECT COUNT(*) FROM TEMP";
		intResult = (Integer) JdbcUtil.executeQuery(connection, query, null);
		assertEquals(5, intResult);
	}

	@After
	public void closeDatabase() throws SQLException {
		if (connection != null) {
			connection.createStatement().execute("DROP ALL OBJECTS");
			connection.close();
		}
	}

	private void compareXML(String expectedFile, String result)
			throws SAXException, IOException {
		URL expectedUrl = ClassUtils.getResourceURL(this, expectedFile);
		if (expectedUrl == null) {
			throw new IOException("cannot find resource [" + expectedUrl + "]");
		}
		String expected = Misc.resourceToString(expectedUrl);
		Diff diff = XMLUnit.compareXML(expected, result);
		assertTrue(diff.toString(), diff.identical());
	}
}
