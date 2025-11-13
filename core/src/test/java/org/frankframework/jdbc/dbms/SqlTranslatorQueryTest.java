package org.frankframework.jdbc.dbms;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;

import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.StreamUtil;

@WithLiquibase(file = "Migrator/ChangelogSqlTranslatorTests.xml", tableName = DbmsSupportTest.TEST_TABLE)
public class SqlTranslatorQueryTest {

	private DatabaseTestEnvironment env;
	private IDbmsSupport dbmsSupport;

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) {
		this.env = env;
		dbmsSupport = env.getDbmsSupport();
	}

	@DatabaseTest
	void testInsertEmptyBlob() throws SQLException, JdbcException, IOException {
		// Arrange
		String originalQuery;
		if (Dbms.MSSQL == dbmsSupport.getDbms()) {
			// Inserting a "sequence-nextval" like substitute doesn't work in MS SQL with auto-increment columns
			// However we do want to test this SQL translation for all other databases
			originalQuery = "INSERT INTO BLOB_NOT_NULL(TBLOB) VALUES (EMPTY_BLOB())";
		} else {
			originalQuery = "INSERT INTO BLOB_NOT_NULL(TKEY, TBLOB) VALUES (SEQ_BLOB_NOT_NULL.NEXTVAL, EMPTY_BLOB())";
		}

		// Act
		String convertedQuery = dbmsSupport.convertQuery(originalQuery, "Oracle");

		// Execute query to test
		long key = runInsertQuery(convertedQuery, "TKEY");

		// Assert
		String result = getBlobValueAsString("SELECT TBLOB FROM BLOB_NOT_NULL WHERE TKEY = ?", key);
		assertEquals("", result);
	}

	@DatabaseTest
	void testInsertEmptyClob() throws SQLException, IOException, JdbcException {
		// Arrange
		String originalQuery;
		if (Dbms.MSSQL == dbmsSupport.getDbms()) {
			// Inserting a "sequence-nextval" like substitute doesn't work in MS SQL with auto-increment columns
			// However we do want to test this SQL translation for all other databases
			originalQuery = "INSERT INTO CLOB_NOT_NULL(TCLOB) VALUES (EMPTY_CLOB())";
		} else {
			originalQuery = "INSERT INTO CLOB_NOT_NULL(TKEY, TCLOB) VALUES (SEQ_CLOB_NOT_NULL.NEXTVAL, EMPTY_CLOB())";
		}

		// Act
		String convertedQuery = dbmsSupport.convertQuery(originalQuery, "Oracle");

		// Execute query to test
		long key = runInsertQuery(convertedQuery, "TKEY");

		// Assert
		String result = getClobValueAsString("SELECT TCLOB FROM CLOB_NOT_NULL WHERE TKEY = ?", key);
		assertEquals("", result);
	}

	private long runInsertQuery(String query, String keyField) throws SQLException {
		// Column name of generated key-field should be in lowercase for PostgreSQL
		String keyFieldToUse;
		if (dbmsSupport.getDbms() == Dbms.POSTGRESQL) {
			keyFieldToUse = keyField.toLowerCase();
		} else {
			keyFieldToUse = keyField;
		}
		try (Connection conn = env.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(query, new String[]{ keyFieldToUse })) {

			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (!generatedKeys.next()) {
				fail("No generated keys from insert statement");
			}
			return generatedKeys.getLong(1);
		}
	}

	private String getBlobValueAsString(final String selectQuery, final long id) throws SQLException, JdbcException, IOException {
		String getBlobQuery = dbmsSupport.convertQuery(selectQuery, "Oracle");
		try (Connection connection = env.getConnection(); PreparedStatement statement = connection.prepareStatement(getBlobQuery)) {
			statement.setLong(1, id);
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				fail("No data found for id = " + id);
			}
			if (dbmsSupport.getDbms() ==  Dbms.POSTGRESQL) {
				// Liquibase didn't make a true Blob column for Postgresql but a byte[] column
				return new String(rs.getBytes(1));
			} else {
				Blob blob = rs.getBlob(1);
				try (InputStream in = blob.getBinaryStream()) {
					return StreamUtil.streamToString(in);
				}
			}
		}
	}

	private String getClobValueAsString(final String selectQuery, final long id) throws SQLException, JdbcException, IOException {
		String getClobQuery = dbmsSupport.convertQuery(selectQuery, "Oracle");
		try (Connection connection = env.getConnection(); PreparedStatement statement = connection.prepareStatement(getClobQuery)) {
			statement.setLong(1, id);
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				fail("No data found for id = " + id);
			}
			if (dbmsSupport.getDbms() ==  Dbms.POSTGRESQL) {
				// Liquibase didn't make a true clob column for Postgresql but a text column
				return rs.getString(1);
			} else {
				Clob clob = rs.getClob(1);
				try (Reader in = clob.getCharacterStream()) {
					return StreamUtil.readerToString(in, "\n");
				}
			}
		}
	}
}
