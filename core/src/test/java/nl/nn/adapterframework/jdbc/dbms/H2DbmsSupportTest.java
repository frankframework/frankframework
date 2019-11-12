package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import nl.nn.adapterframework.jdbc.JdbcException;

import org.junit.Test;

public class H2DbmsSupportTest {

	@Test
	public void testConvertQueryOracleToH2() throws JdbcException, SQLException {
		String query = "DROP SEQUENCE SEQ_IBISSTORE";
		String expected = "DROP SEQUENCE IF EXISTS SEQ_IBISSTORE";
		String result = (new H2DbmsSupport()).convertQuery(null, query, "Oracle");
		assertEquals(expected, result);
	}

	@Test
	public void testConvertQueryH2ToH2() throws JdbcException, SQLException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		String expected = "SELECT COUNT(*) FROM IBISSTORE";
		String result = (new H2DbmsSupport()).convertQuery(null, query, "H2");
		assertEquals(expected, result);
	}
}