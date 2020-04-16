package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;

import org.junit.Test;

public class H2DbmsSupportTest {

	@Test
	public void testConvertQueryOracleToH2() throws JdbcException, SQLException {
		String query = "DROP SEQUENCE SEQ_IBISSTORE";
		String expected = "DROP SEQUENCE IF EXISTS SEQ_IBISSTORE";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		(new H2DbmsSupport()).convertQuery(queryExecutionContext, "Oracle");
		assertEquals(expected, queryExecutionContext.getQuery());
	}

	@Test
	public void testConvertQueryH2ToH2() throws JdbcException, SQLException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		(new H2DbmsSupport()).convertQuery(queryExecutionContext, "H2");
		assertEquals(expected, queryExecutionContext.getQuery());
	}

	@Test
	public void testConvertMultipleQueriesOracleToH2() throws JdbcException, SQLException {
		String query = "--------\n  --drop--\r\n--------\nDROP SEQUENCE SEQ_IBISSTORE;\nselect count(*) from ibisstore;";
		String expected = "--------" + System.lineSeparator() + "--drop--" + System.lineSeparator() + "--------" + System.lineSeparator() + "DROP SEQUENCE IF EXISTS SEQ_IBISSTORE;" + "select count(*) from ibisstore;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		(new H2DbmsSupport()).convertQuery(queryExecutionContext, "Oracle");
		assertEquals(expected, queryExecutionContext.getQuery());
	}
}