package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.junit.Test;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;

public class OracleToMsSqlTranslatorTest {

	@Test
	public void testNotConverted() throws JdbcException, SQLException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = OracleToMSSQLTranslator.convertQuery(queryExecutionContext, false);
		assertEquals(query, result);
	}

	@Test
	public void testConvertQueryInsertInto() throws JdbcException, SQLException {
		String query = "INSERT INTO IBISTEMP (tkey,tblob1) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB());";
		String expected = "INSERT INTO IBISTEMP(tkey, tblob1) VALUES(NEXT VALUE FOR SEQ_IBISTEMP, 0x);";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = OracleToMSSQLTranslator.convertQuery(queryExecutionContext, false);
		assertEquals(expected, result);
	}

	@Test
	public void testSelectCurrentValueOfSequence() throws JdbcException, SQLException {
		String query = "SELECT SEQ_IBISTEMP.NEXTVAL FROM DuaL";
		String expected = "SELECT NEXT VALUE FOR SEQ_IBISTEMP";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = OracleToMSSQLTranslator.convertQuery(queryExecutionContext, false);
		assertEquals(expected, result);
	}

	@Test
	public void testSelectForUpdate() throws JdbcException, SQLException {
		String query    = "SELECT tblob1 FROM ibistemp WHERE tkey=? FOR UPDATE;";
		String expected = "SELECT tblob1 FROM ibistemp WHERE tkey=? FOR UPDATE;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = OracleToMSSQLTranslator.convertQuery(queryExecutionContext, false);
		assertEquals(expected, result);
	}

}
