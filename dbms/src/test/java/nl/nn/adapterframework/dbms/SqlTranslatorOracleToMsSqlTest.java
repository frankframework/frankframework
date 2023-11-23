package nl.nn.adapterframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
public class SqlTranslatorOracleToMsSqlTest {

	private String convertQuery(String query) throws JdbcException {
		SqlTranslator translator = new SqlTranslator("Oracle", "MS SQL");
		return translator.translate(query);
	}

	private String skipIrrelevantWhitespace(String query) {
		String result = query.replaceAll(" \\(", "(");
		result = result.replace(", ", ",");
		return result;
	}

	@Test
	public void testNotConverted() throws JdbcException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(query), skipIrrelevantWhitespace(result), "unchanged");
	}

	@Test
	public void testConvertQueryInsertInto() throws JdbcException {
		String query = "INSERT INTO IBISTEMP (tkey,tblob1) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB());";
		String expected = "INSERT INTO IBISTEMP(tkey, tblob1) VALUES(NEXT VALUE FOR SEQ_IBISTEMP, 0x);";
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}


	@Test
	public void testSelectCurrentValueOfSequence() throws JdbcException {
		String query = "SELECT SEQ_IBISTEMP.NEXTVAL FROM DuaL";
		String expected = "SELECT NEXT VALUE FOR SEQ_IBISTEMP";
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testSelectForUpdate() throws JdbcException {
		String query    = "SELECT tblob1 FROM ibistemp WHERE tkey=? FOR UPDATE;";
		String expected = "SELECT tblob1 FROM ibistemp WHERE tkey=? FOR UPDATE;";
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testConvertQuerySelect() throws JdbcException {
		String query   = " SELECT FIELD1, FIELD2, DECODE(FIELD3,'Y','true','N','false',NULL,'true') AS FIELD3, LISTAGG(FIELD4, ' + ') WITHIN GROUP (ORDER BY FIELD4) AS FIELD4 FROM TABLE1 GROUP BY FIELD1, FIELD2, FIELD3, FIELD4 ORDER BY FIELD4, FIELD2  \n";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testConvertQueryCreateSequence() throws JdbcException {
		String query = "CREATE SEQUENCE SEQ_IBISSTORE INCREMENT BY 1 MAXVALUE 999999999999999999999999999 MINVALUE 1 CACHE 20 NOORDER;";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testConvertQueryCreateTable() throws JdbcException {
		String query = "CREATE TABLE TABLE1 (FIELD1 NUMBER(10, 0) NOT NULL, FIELD2 CHAR(2 CHAR), FIELD3 NUMBER(*, 0), FIELD4 VARCHAR2(35 CHAR), CONSTRAINT PK_TABLE1 PRIMARY KEY (FIELD1)) LOGGING NOCOMPRESS NOCACHE NOPARALLEL NOMONITORING";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testConvertQueryCreateTableIbisStore() throws JdbcException {
		String query = "CREATE TABLE IBISSTORE (MESSAGEKEY NUMBER(10) NOT NULL, TYPE CHAR(1 CHAR), SLOTID VARCHAR2(100 CHAR), HOST VARCHAR2(100 CHAR), MESSAGEID VARCHAR2(100 CHAR), CORRELATIONID VARCHAR2(256 CHAR), MESSAGEDATE TIMESTAMP(6), COMMENTS VARCHAR2(1000 CHAR), MESSAGE BLOB, EXPIRYDATE TIMESTAMP(6), LABEL VARCHAR2(100 CHAR), CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY));";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testConvertQueryDropSequence() throws JdbcException {
		String query = "--------\n  --drop--\r\n--------\nDROP SEQUENCE SEQ_IBISSTORE";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testConvertQueryCreateIndex() throws JdbcException {
		String query = "CREATE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3)) NOLOGGING PARALLEL;";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testConvertQueryCreateUniqueIndex() throws JdbcException {
		String query = "CREATE UNIQUE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3))";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testConvertQueryAlterTable() throws JdbcException {
		String query = "ALTER TABLE TABLE1 ADD CONSTRAINT FK_FIELD1 FOREIGN KEY (FIELD1) REFERENCES TABLE2 (FIELD1) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE VALIDATE;";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}

	@Test
	public void testSetDefineOff() throws JdbcException {
		String query = "Set define off;";
		String result = convertQuery(query);
		assertEquals(query, result);
	}

	@Test
	public void testCreateOrReplaceTrigger() throws JdbcException {
		String query = "create or replace TRIGGER TRIGGER1 AFTER DELETE ON FIELD1 FOR EACH ROW DECLARE BEGIN DELETE FROM TABEL2 WHERE FIELD3 = FIELD1; END;";
		String result = convertQuery(query);
		assertEquals(query, result);
	}

	@Test
	public void testExit() throws JdbcException {
		String query = "exit;";
		String result = convertQuery(query);
		assertEquals(query, result);
	}

	@Test
	public void testConvertQuerySelectOneWhereForUpdate() throws JdbcException {
		String query = " SELECT FIELD2 FROM TABLE1 WHERE FIELD1=? AND FIELD3 = ? FOR UPDATE";
		String expected = query;
		String result = convertQuery(query);
		assertEquals(skipIrrelevantWhitespace(expected), skipIrrelevantWhitespace(result), query);
	}
}
