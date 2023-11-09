package nl.nn.adapterframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SqlTranslatorTest {

	public static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of("ORACLE", "H2", "SELECT COUNT(*) FROM IBISSTORE", "SELECT COUNT(*) FROM IBISSTORE"),
				Arguments.of("ORACLE", "MS SQL", "SELECT COUNT(*) FROM IBISSTORE", "SELECT COUNT(*) FROM IBISSTORE"),
				Arguments.of("Oracle", "Oracle", "SELECT COUNT(*) FROM IBISSTORE", "SELECT COUNT(*) FROM IBISSTORE"),
				Arguments.of("Oracle", null, null, "java.lang.IllegalArgumentException"),
//				Arguments.of("not-a-db", "MS SQL", null, "java.lang.IllegalArgumentException"),
//				Arguments.of("", "MS SQL", null, "java.lang.IllegalArgumentException"),
				Arguments.of("oracle", "MS SQL", "INSERT INTO IBISTEMP (tkey,tblob) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB());", "INSERT INTO IBISTEMP (tkey,tblob) VALUES (NEXT VALUE FOR SEQ_IBISTEMP,0x);"),
				Arguments.of("Oracle", "MS SQL", "SELECT SEQ_IBISTEMP.NEXTVAL FROM DuaL", "SELECT NEXT VALUE FOR SEQ_IBISTEMP"),
				Arguments.of("Oracle", "MySQL", "SELECT tkey, tblob FROM IBISTEMP FETCH FIRST 2 ROWS ONLY;", "SELECT tkey, tblob FROM IBISTEMP LIMIT 2;"),
				Arguments.of("Oracle", "MySQL", "INSERT INTO IBISTEMP (tkey,tblob,tdate,ttimestamp) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB(),SYSDATE, SYSTIMESTAMP);", "INSERT INTO IBISTEMP (tkey,tblob,tdate,ttimestamp) VALUES (NULL,'',SYSDATE(), CURRENT_TIMESTAMP());"),
//				Arguments.of("postgresql", "Oracle", "SELECT tkey, tblob FROM IBISTEMP LIMIT 2;", "SELECT tkey, tblob FROM IBISTEMP FETCH FIRST 2 ROWS ONLY;"),
//				Arguments.of("Oracle", "postgresql", "INSERT INTO IBISTEMP (tkey,tblob,tdate,ttimestamp) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB(),SYSDATE, SYSTIMESTAMP);", "INSERT INTO IBISTEMP (tkey,tblob,tdate,ttimestamp) VALUES (NEXTVAL('SEQ_IBISTEMP'),NULL,CURRENT_DATE, CURRENT_TIMESTAMP);"),
				Arguments.of("Oracle", "H2", "INSERT INTO IBISTEMP (tkey,tblob,tdate,ttimestamp) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB(),SYSDATE, SYSTIMESTAMP);", "INSERT INTO IBISTEMP (tkey,tblob,tdate,ttimestamp) VALUES (NEXT VALUE FOR SEQ_IBISTEMP,'',CURRENT_DATE, CURRENT_TIMESTAMP);"),
				Arguments.of("Oracle", "MS SQL", "SELECT SEQ_IBISDATA.CURRVAL FROM DUAL", "SELECT (SELECT current_value FROM sys.sequences WHERE name = 'SEQ_IBISDATA')"),
//				Arguments.of("Oracle", "H2", "SELECT FIELD1, FIELD2, DECODE(FIELD3,'Y','true','N','false',NULL,'true') AS FIELD3, LISTAGG(FIELD4, ' + ') WITHIN GROUP (ORDER BY FIELD4) AS FIELD4 FROM TABLE1  GROUP BY FIELD1, FIELD2, FIELD3, FIELD4 ORDER BY FIELD4, FIELD2", "SELECT FIELD1, FIELD2, DECODE(FIELD3, 'Y', 'true', 'N', 'false', NULL, 'true') AS FIELD3, group_concat(FIELD4 ORDER BY FIELD4 SEPARATOR ' + ') AS FIELD4 FROM TABLE1 GROUP BY FIELD1, FIELD2, FIELD3, FIELD4 ORDER BY FIELD4, FIELD2"),
//				Arguments.of("Oracle", "H2", "CREATE SEQUENCE SEQ_IBISSTORE INCREMENT BY 1 MAXVALUE 999999999999999999999999999 MINVALUE 1 CACHE 20 NOORDER;", "CREATE SEQUENCE SEQ_IBISSTORE INCREMENT BY 1 MAXVALUE 999999999999999999 MINVALUE 1 CACHE 20;"),
//				Arguments.of("Oracle", "H2", "CREATE TABLE TABLE1 (FIELD1 NUMBER(10, 0) NOT NULL, FIELD2 CHAR(2 CHAR), FIELD3 NUMBER(*, 0), FIELD4 VARCHAR2(35 CHAR), CONSTRAINT PK_TABLE1 PRIMARY KEY (FIELD1)) LOGGING NOCOMPRESS NOCACHE NOPARALLEL NOMONITORING", "CREATE TABLE TABLE1(FIELD1 NUMBER(10, 0) NOT NULL, FIELD2 CHAR(2 CHAR), FIELD3 NUMBER(38, 0), FIELD4 VARCHAR2(35 CHAR), CONSTRAINT PK_TABLE1 PRIMARY KEY(FIELD1))"),
//				Arguments.of("Oracle", "H2", "CREATE TABLE IBISSTORE (MESSAGEKEY NUMBER(10) NOT NULL, TYPE CHAR(1 CHAR), SLOTID VARCHAR2(100 CHAR), HOST VARCHAR2(100 CHAR), MESSAGEID VARCHAR2(100 CHAR), CORRELATIONID VARCHAR2(256 CHAR), MESSAGEDATE TIMESTAMP(6), COMMENTS VARCHAR2(1000 CHAR), MESSAGE BLOB, EXPIRYDATE TIMESTAMP(6), LABEL VARCHAR2(100 CHAR), CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY));", "CREATE TABLE IBISSTORE(MESSAGEKEY INT IDENTITY NOT NULL, TYPE CHAR(1 CHAR), SLOTID VARCHAR2(100 CHAR), HOST VARCHAR2(100 CHAR), MESSAGEID VARCHAR2(100 CHAR), CORRELATIONID VARCHAR2(256 CHAR), MESSAGEDATE TIMESTAMP(6), COMMENTS VARCHAR2(1000 CHAR), MESSAGE BLOB, EXPIRYDATE TIMESTAMP(6), LABEL VARCHAR2(100 CHAR), CONSTRAINT PK_IBISSTORE PRIMARY KEY(MESSAGEKEY));"),
//				Arguments.of("Oracle", "H2", "CREATE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3)) NOLOGGING PARALLEL;", "CREATE INDEX FBIX_TABLE1 ON TABLE1(FIELD3);"),
//				Arguments.of("Oracle", "H2", "CREATE UNIQUE INDEX FBIX_TABLE1 ON TABLE1 (LOWER(FIELD3))", "CREATE UNIQUE INDEX FBIX_TABLE1 ON TABLE1(FIELD3)"),
//				Arguments.of("Oracle", "H2", "ALTER TABLE TABLE1 ADD CONSTRAINT FK_FIELD1 FOREIGN KEY (FIELD1) REFERENCES TABLE2 (FIELD1) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE VALIDATE;", "ALTER TABLE TABLE1 ADD CONSTRAINT FK_FIELD1 FOREIGN KEY(FIELD1) REFERENCES TABLE2(FIELD1) ON DELETE CASCADE;"),
				Arguments.of("Oracle", "MariaDB", "SELECT tkey, tblob FROM IBISTEMP FETCH FIRST 2 ROWS ONLY;", "SELECT tkey, tblob FROM IBISTEMP LIMIT 2;"),
				Arguments.of("Oracle", "MariaDB", "INSERT INTO IBISTEMP (tkey,tblob,tdate,ttimestamp) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB(),SYSDATE, SYSTIMESTAMP);", "INSERT INTO IBISTEMP (tkey,tblob,tdate,ttimestamp) VALUES (NULL,'',SYSDATE(), CURRENT_TIMESTAMP());")
		);
	}

	@ParameterizedTest()
	@MethodSource("data")
	public void test(String source, String target, String query, String expected) throws Throwable {
		try {
			SqlTranslator translator = new SqlTranslator(source, target);
			String out = translator.translate(query);

			System.out.println("IN : " + query);
			System.out.println("OUT: " + out);
			assertEquals(expected, out, query);
		} catch (Throwable t) {
			if (checkExceptionClass(t, expected)) {
				assertTrue(true);
			} else {
				throw t;
			}
		}
	}


	/**
	 * Recursively check if the exception thrown is equal to the exception expected.
	 *
	 * @param t Throwable to be checked.
	 * @param c Class to be checked
	 * @return True if one of the causes of the exception is the given class, false otherwise.
	 * @throws Throwable Input t when class is not found.
	 */
	private boolean checkExceptionClass(Throwable t, String c) throws Throwable {
		try {
			return checkExceptionClass(t, Class.forName(c));
		} catch (ClassNotFoundException e) {
			if (c.equalsIgnoreCase("success"))
				return false;
			throw t;
		}
	}

	/**
	 * Recursively check if the exception thrown is equal to the exception expected.
	 *
	 * @param t Throwable to be checked.
	 * @param c Class to be checked
	 * @return True if one of the causes of the exception is the given class, false otherwise.
	 */
	private boolean checkExceptionClass(Throwable t, Class c) {
		if (c.isInstance(t)) {
			return true;
		} else if (t.getCause() != null) {
			return checkExceptionClass(t.getCause(), c);
		}
		return false;
	}
}
