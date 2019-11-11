package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import nl.nn.adapterframework.jdbc.JdbcException;

import org.junit.Test;

public class OracleToH2TranslatorTest {

	@Test
	public void testConvertQuerySelect() throws JdbcException, SQLException {
		String query = " SELECT FIELD1, FIELD2, DECODE(FIELD3,'Y','true','N','false',NULL,'true') AS FIELD3, LISTAGG(FIELD4, ' + ') WITHIN GROUP (ORDER BY FIELD4) AS FIELD4 FROM TABLE1 \n\t  \n  GROUP BY FIELD1, FIELD2, FIELD3, FIELD4 ORDER BY FIELD4, FIELD2  \n";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals("SELECT FIELD1, FIELD2, DECODE(FIELD3, 'Y', 'true', 'N', 'false', NULL, 'true') AS FIELD3, group_concat(FIELD4 ORDER BY FIELD4 SEPARATOR ' + ') AS FIELD4 FROM TABLE1 GROUP BY FIELD1, FIELD2, FIELD3, FIELD4 ORDER BY FIELD4, FIELD2", convertedQuery);
	}

	@Test
	public void testConvertQueryCreateSequence() throws JdbcException, SQLException {
		String query = "CREATE SEQUENCE SEQ_IBISSTORE INCREMENT BY 1 MAXVALUE 999999999999999999999999999 MINVALUE 1 CACHE 20;";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals("CREATE SEQUENCE SEQ_IBISSTORE INCREMENT BY 1 MAXVALUE 999999999999999999 MINVALUE 1 CACHE 20;", convertedQuery);
	}

	@Test
	public void testConvertQueryCreateTable() throws JdbcException, SQLException {
		String query = "CREATE TABLE TABLE1 (FIELD1 NUMBER(10, 0) NOT NULL, FIELD2 CHAR(2 CHAR), FIELD3 NUMBER(*, 0), FIELD4 VARCHAR2(35 CHAR), CONSTRAINT PK_TABLE1 PRIMARY KEY (FIELD1))";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals("CREATE TABLE TABLE1(FIELD1 NUMBER(10, 0) NOT NULL, FIELD2 CHAR(2 CHAR), FIELD3 NUMBER(38, 0), FIELD4 VARCHAR2(35 CHAR), CONSTRAINT PK_TABLE1 PRIMARY KEY(FIELD1))", convertedQuery);
	}

	@Test
	public void testConvertQueryCreateTableIbisStore() throws JdbcException, SQLException {
		String query = "CREATE TABLE IBISSTORE (MESSAGEKEY NUMBER(10) NOT NULL, TYPE CHAR(1 CHAR), SLOTID VARCHAR2(100 CHAR), HOST VARCHAR2(100 CHAR), MESSAGEID VARCHAR2(100 CHAR), CORRELATIONID VARCHAR2(256 CHAR), MESSAGEDATE TIMESTAMP(6), COMMENTS VARCHAR2(1000 CHAR), MESSAGE BLOB, EXPIRYDATE TIMESTAMP(6), LABEL VARCHAR2(100 CHAR), CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY));";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals("CREATE TABLE IBISSTORE(MESSAGEKEY INT IDENTITY NOT NULL, TYPE CHAR(1 CHAR), SLOTID VARCHAR2(100 CHAR), HOST VARCHAR2(100 CHAR), MESSAGEID VARCHAR2(100 CHAR), CORRELATIONID VARCHAR2(256 CHAR), MESSAGEDATE TIMESTAMP(6), COMMENTS VARCHAR2(1000 CHAR), MESSAGE BLOB, EXPIRYDATE TIMESTAMP(6), LABEL VARCHAR2(100 CHAR), CONSTRAINT PK_IBISSTORE PRIMARY KEY(MESSAGEKEY));", convertedQuery);
	}

	@Test
	public void testConvertQueryDropSequence() throws JdbcException, SQLException {
		String query = "DROP SEQUENCE SEQ_IBISSTORE";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals("DROP SEQUENCE IF EXISTS SEQ_IBISSTORE", convertedQuery);
	}

	@Test
	public void testConvertQueryCreateIndex() throws JdbcException, SQLException {
		String query = "CREATE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3));";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals("CREATE INDEX FBIX_TABLE1 ON TABLE1(FIELD3);", convertedQuery);
	}

	@Test
	public void testConvertQueryAlterTable() throws JdbcException, SQLException {
		String query = "ALTER TABLE TABLE1 ADD CONSTRAINT FK_FIELD1 FOREIGN KEY (FIELD1) REFERENCES TABLE2 (FIELD1) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE VALIDATE;";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals("ALTER TABLE TABLE1 ADD CONSTRAINT FK_FIELD1 FOREIGN KEY(FIELD1) REFERENCES TABLE2(FIELD1) ON DELETE CASCADE;", convertedQuery);
	}

	@Test
	public void testSetDefineOff() throws JdbcException, SQLException {
		String query = "Set define off;";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals(null, convertedQuery);
	}

	@Test
	public void testIgnoreCreateOrReplaceTrigger() throws JdbcException, SQLException {
		String query = "create or replace TRIGGER TRIGGER1 AFTER DELETE ON FIELD1 FOR EACH ROW DECLARE BEGIN DELETE FROM TABEL2 WHERE FIELD3 = FIELD1; END;";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals(null, convertedQuery);

	}

	@Test
	public void testIgnoreAlterTrigger() throws JdbcException, SQLException {
		String query = "ALTER TRIGGER TRIGGER1 ENABLE;";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals(null, convertedQuery);

	}

	@Test
	public void testNotConverted() throws JdbcException, SQLException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		String convertedQuery = OracleToH2Translator.convertQuery(null, query);
		assertEquals(query, convertedQuery);

	}
}
