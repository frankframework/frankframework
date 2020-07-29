package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.junit.Test;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;

public abstract class OracleToH2TranslatorTestBase {

	protected abstract String convertQuery(QueryExecutionContext queryExecutionContext, boolean canModifyQueryExecutionContext) throws JdbcException, SQLException;

	protected String skipIrrelevantWhitespace(String query) {
		String result = query.replaceAll(" \\(", "(");
		result = result.replaceAll(", ", ",");
		result = result.replaceAll(" ;", ";");
		result = result.replaceAll(" =", "=");
		result = result.replaceAll("= ", "=");
		result = result.replaceAll("[\\t ]+", " ");
		result = result.replaceAll("[ \\t\r]*[\\n][ \\t\r]*", "\n");
		return result.trim();
	}

	@Test
	public void testConvertQuerySelect() throws JdbcException, SQLException {
		String query   = " SELECT FIELD1, FIELD2, DECODE(FIELD3,'Y','true','N','false',NULL,'true') AS FIELD3, LISTAGG(FIELD4, ' + ') WITHIN GROUP (ORDER BY FIELD4) AS FIELD4 FROM TABLE1 GROUP BY FIELD1, FIELD2, FIELD3, FIELD4 ORDER BY FIELD4, FIELD2  \n";
		String expected = "SELECT FIELD1, FIELD2, DECODE(FIELD3, 'Y', 'true', 'N', 'false', NULL, 'true') AS FIELD3, group_concat(FIELD4 ORDER BY FIELD4 SEPARATOR ' + ') AS FIELD4 FROM TABLE1 GROUP BY FIELD1, FIELD2, FIELD3, FIELD4 ORDER BY FIELD4, FIELD2";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateSequence() throws JdbcException, SQLException {
		String query = "CREATE SEQUENCE SEQ_IBISSTORE INCREMENT BY 1 MAXVALUE 999999999999999999999999999 MINVALUE 1 CACHE 20 NOORDER;";
		String expected = "CREATE SEQUENCE SEQ_IBISSTORE INCREMENT BY 1 MAXVALUE 999999999999999999 MINVALUE 1 CACHE 20;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateTable() throws JdbcException, SQLException {
		String query = "CREATE TABLE TABLE1 (FIELD1 NUMBER(10, 0) NOT NULL, FIELD2 CHAR(2 CHAR), FIELD3 NUMBER(*, 0), FIELD4 VARCHAR2(35 CHAR), CONSTRAINT PK_TABLE1 PRIMARY KEY (FIELD1)) LOGGING NOCOMPRESS NOCACHE NOPARALLEL NOMONITORING";
		String expected = "CREATE TABLE TABLE1(FIELD1 NUMBER(10, 0) NOT NULL, FIELD2 CHAR(2 CHAR), FIELD3 NUMBER(38, 0), FIELD4 VARCHAR2(35 CHAR), CONSTRAINT PK_TABLE1 PRIMARY KEY(FIELD1))";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateTableIbisStore() throws JdbcException, SQLException {
		String query = "CREATE TABLE IBISSTORE (MESSAGEKEY NUMBER(10) NOT NULL, TYPE CHAR(1 CHAR), SLOTID VARCHAR2(100 CHAR), HOST VARCHAR2(100 CHAR), MESSAGEID VARCHAR2(100 CHAR), CORRELATIONID VARCHAR2(256 CHAR), MESSAGEDATE TIMESTAMP(6), COMMENTS VARCHAR2(1000 CHAR), MESSAGE BLOB, EXPIRYDATE TIMESTAMP(6), LABEL VARCHAR2(100 CHAR), CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY));";
		String expected = "CREATE TABLE IBISSTORE(MESSAGEKEY INT IDENTITY NOT NULL, TYPE CHAR(1 CHAR), SLOTID VARCHAR2(100 CHAR), HOST VARCHAR2(100 CHAR), MESSAGEID VARCHAR2(100 CHAR), CORRELATIONID VARCHAR2(256 CHAR), MESSAGEDATE TIMESTAMP(6), COMMENTS VARCHAR2(1000 CHAR), MESSAGE BLOB, EXPIRYDATE TIMESTAMP(6), LABEL VARCHAR2(100 CHAR), CONSTRAINT PK_IBISSTORE PRIMARY KEY(MESSAGEKEY));";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryDropSequence() throws JdbcException, SQLException {
		String query = "--------\n  --drop--\r\n--------\nDROP SEQUENCE SEQ_IBISSTORE";
		String expected = "--------" + System.lineSeparator() + " --drop--" + System.lineSeparator() + "--------" + System.lineSeparator() + "DROP SEQUENCE IF EXISTS SEQ_IBISSTORE";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateIndex() throws JdbcException, SQLException {
		String query = "CREATE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3)) NOLOGGING PARALLEL;";
		String expected = "CREATE INDEX FBIX_TABLE1 ON TABLE1(FIELD3);";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryCreateUniqueIndex() throws JdbcException, SQLException {
		String query = "CREATE UNIQUE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3))";
		String expected = "CREATE UNIQUE INDEX FBIX_TABLE1 ON TABLE1 (FIELD3)";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryAlterTable() throws JdbcException, SQLException {
		String query = "ALTER TABLE TABLE1 ADD CONSTRAINT FK_FIELD1 FOREIGN KEY (FIELD1) REFERENCES TABLE2 (FIELD1) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE VALIDATE;";
		String expected = "ALTER TABLE TABLE1 ADD CONSTRAINT FK_FIELD1 FOREIGN KEY(FIELD1) REFERENCES TABLE2(FIELD1) ON DELETE CASCADE;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testSetDefineOff() throws JdbcException, SQLException {
		String query = "Set define off;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, null, result);
	}

	@Test
	public void testIgnoreCreateOrReplaceTrigger() throws JdbcException, SQLException {
		String query = "create or replace TRIGGER TRIGGER1 AFTER DELETE ON FIELD1 FOR EACH ROW DECLARE BEGIN DELETE FROM TABEL2 WHERE FIELD3 = FIELD1; END;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, null, result);
	}

	@Test
	public void testIgnoreAlterTrigger() throws JdbcException, SQLException {
		String query = "ALTER TRIGGER TRIGGER1 ENABLE;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, null, result);
	}

	@Test
	public void testIgnoreAlterTableIbisStore() throws JdbcException, SQLException {
		String query = "ALTER TABLE IBISSTORE ADD (CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY));";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, null, result);
	}

	@Test
	public void testExit() throws JdbcException, SQLException {
		String query = "exit;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, null, result);
	}

	@Test
	public void testConvertQueryInsertInto() throws JdbcException, SQLException {
		String query = "INSERT INTO IBISTEMP (tkey,tblob1) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB());";
		String expected = "INSERT INTO IBISTEMP(tkey, tblob1) VALUES(SEQ_IBISTEMP.NEXTVAL, '');";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQuerySelectOneWhereForUpdate() throws JdbcException, SQLException {
		String query = " SELECT FIELD2 FROM TABLE1 WHERE FIELD1=? AND FIELD3 = ? FOR UPDATE";
		String expected = "SELECT FIELD2, FIELD1 FROM TABLE1 WHERE FIELD1=? AND FIELD3=? FOR UPDATE";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, true);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testConvertQueryUpdateSet() throws JdbcException, SQLException {
		String query = "UPDATE IBISTEMP SET tblob1=EMPTY_BLOB() WHERE tkey=?;";
		String expected = "UPDATE IBISTEMP SET tblob1='' WHERE tkey=?;";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query,  skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result));
	}

	@Test
	public void testNotConverted() throws JdbcException, SQLException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, null, null);
		String result = convertQuery(queryExecutionContext, false);
		assertEquals(query, query, result);
	}
}
