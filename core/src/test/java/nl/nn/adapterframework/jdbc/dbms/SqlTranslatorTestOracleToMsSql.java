package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.junit.Test;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;

public class SqlTranslatorTestOracleToMsSql {

	private String convertQuery(QueryExecutionContext queryExecutionContext, boolean canModifyQueryExecutionContext) throws JdbcException, SQLException {
		SqlTranslator translator = new SqlTranslator("Oracle", "MS SQL");
		return translator.translate(queryExecutionContext.getQuery());
	}

	private String skipIrrelevantWhitespace(String query) {
		String result = query.replaceAll(" \\(", "(");
		result = result.replaceAll(", ", ",");
		return result;
	}

	@Test
	public void testNotConverted() throws JdbcException, SQLException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals("unchanged", skipIrrelevantWhitespace(query), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryInsertInto() throws JdbcException, SQLException {
		String query = "INSERT INTO IBISTEMP (tkey,tblob1) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB());";
		String expected = "INSERT INTO IBISTEMP(tkey, tblob1) VALUES(NEXT VALUE FOR SEQ_IBISTEMP, 0x);";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}


	@Test
	public void testSelectCurrentValueOfSequence() throws JdbcException, SQLException {
		String query = "SELECT SEQ_IBISTEMP.NEXTVAL FROM DuaL";
		String expected = "SELECT NEXT VALUE FOR SEQ_IBISTEMP";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testSelectForUpdate() throws JdbcException, SQLException {
		String query    = "SELECT tblob1 FROM ibistemp WHERE tkey=? FOR UPDATE;";
		String expected = "SELECT tblob1 FROM ibistemp WHERE tkey=? FOR UPDATE;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQuerySelect() throws JdbcException, SQLException {
		String query   = " SELECT FIELD1, FIELD2, DECODE(FIELD3,'Y','true','N','false',NULL,'true') AS FIELD3, LISTAGG(FIELD4, ' + ') WITHIN GROUP (ORDER BY FIELD4) AS FIELD4 FROM TABLE1 GROUP BY FIELD1, FIELD2, FIELD3, FIELD4 ORDER BY FIELD4, FIELD2  \n";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateSequence() throws JdbcException, SQLException {
		String query = "CREATE SEQUENCE SEQ_IBISSTORE INCREMENT BY 1 MAXVALUE 999999999999999999999999999 MINVALUE 1 CACHE 20 NOORDER;";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateTable() throws JdbcException, SQLException {
		String query = "CREATE TABLE TABLE1 (FIELD1 NUMBER(10, 0) NOT NULL, FIELD2 CHAR(2 CHAR), FIELD3 NUMBER(*, 0), FIELD4 VARCHAR2(35 CHAR), CONSTRAINT PK_TABLE1 PRIMARY KEY (FIELD1)) LOGGING NOCOMPRESS NOCACHE NOPARALLEL NOMONITORING";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateTableIbisStore() throws JdbcException, SQLException {
		String query = "CREATE TABLE IBISSTORE (MESSAGEKEY NUMBER(10) NOT NULL, TYPE CHAR(1 CHAR), SLOTID VARCHAR2(100 CHAR), HOST VARCHAR2(100 CHAR), MESSAGEID VARCHAR2(100 CHAR), CORRELATIONID VARCHAR2(256 CHAR), MESSAGEDATE TIMESTAMP(6), COMMENTS VARCHAR2(1000 CHAR), MESSAGE BLOB, EXPIRYDATE TIMESTAMP(6), LABEL VARCHAR2(100 CHAR), CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY));";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryDropSequence() throws JdbcException, SQLException {
		String query = "--------\n  --drop--\r\n--------\nDROP SEQUENCE SEQ_IBISSTORE";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateIndex() throws JdbcException, SQLException {
		String query = "CREATE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3)) NOLOGGING PARALLEL;";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateUniqueIndex() throws JdbcException, SQLException {
		String query = "CREATE UNIQUE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3))";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryAlterTable() throws JdbcException, SQLException {
		String query = "ALTER TABLE TABLE1 ADD CONSTRAINT FK_FIELD1 FOREIGN KEY (FIELD1) REFERENCES TABLE2 (FIELD1) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE VALIDATE;";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testSetDefineOff() throws JdbcException, SQLException {
		String query = "Set define off;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, result);
	}

	@Test
	public void testCreateOrReplaceTrigger() throws JdbcException, SQLException {
		String query = "create or replace TRIGGER TRIGGER1 AFTER DELETE ON FIELD1 FOR EACH ROW DECLARE BEGIN DELETE FROM TABEL2 WHERE FIELD3 = FIELD1; END;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, result);
	}

	@Test
	public void testExit() throws JdbcException, SQLException {
		String query = "exit;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, result);
	}

	@Test
	public void testConvertQuerySelectOneWhereForUpdate() throws JdbcException, SQLException {
		String query = " SELECT FIELD2 FROM TABLE1 WHERE FIELD1=? AND FIELD3 = ? FOR UPDATE";
		String expected = query;
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, true);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}
}
