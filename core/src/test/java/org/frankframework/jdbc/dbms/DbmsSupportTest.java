package org.frankframework.jdbc.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.AbstractJdbcQuerySender.QueryType;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.StreamUtil;

@Log4j2
@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = DbmsSupportTest.TEST_TABLE)
public class DbmsSupportTest {
	private final boolean testPeekFindsRecordsWhenTheyAreAvailable = true;
	private DatabaseTestEnvironment env;
	private IDbmsSupport dbmsSupport;

	public static final String TEST_TABLE = "Temp"; // use mixed case tablename for testing
	private static final String ROW_VERSION_TEST_TABLE_NAME = "TestTable";

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) {
		this.env = env;
		dbmsSupport = env.getDbmsSupport();
	}

	@DatabaseTest
	public void testGetDbmsSupport() {
		assertNotNull(dbmsSupport);
	}

	@DatabaseTest
	public void testTableLessSelect() throws Exception {
		try (Connection connection = env.getConnection()) {
			assertEquals(4, JdbcTestUtil.executeIntQuery(connection, "SELECT 2+2 " + dbmsSupport.getFromForTablelessSelect()));
		}
	}

	@DatabaseTest
	public void testTableLessSelectWithIntParam() throws Exception {
		try (Connection connection = env.getConnection()) {
			assertEquals(4, JdbcTestUtil.executeIntQuery(connection, "SELECT 1+? " + dbmsSupport.getFromForTablelessSelect(), 3));
		}
	}

	@DatabaseTest
	public void testInsertSelect() throws Exception {
		try (Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(connection, "INSERT INTO " + TEST_TABLE + " (TKEY, TINT) SELECT 11, 2+2 " + dbmsSupport.getFromForTablelessSelect() + " WHERE 1=1");
		}
	}

	@DatabaseTest
	public void testIsTablePresent() throws Exception {
		try (Connection connection = env.getConnection()) {
			assertTrue(dbmsSupport.isTablePresent(connection, TEST_TABLE));
			assertFalse(dbmsSupport.isTablePresent(connection, "XXXX"));
			assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
			assertTrue(dbmsSupport.isTablePresent(connection, TEST_TABLE.toLowerCase()), "Should have found existing table");
			assertTrue(dbmsSupport.isTablePresent(connection, TEST_TABLE.toUpperCase()), "Should have found existing table");
		}
	}

	@DatabaseTest
	public void testIsTablePresentInSchema() throws Exception {
		try (Connection connection = env.getConnection()) {
			String schema = dbmsSupport.getSchema(connection);
			assertTrue(dbmsSupport.isTablePresent(connection, schema, TEST_TABLE));
			assertFalse(dbmsSupport.isTablePresent(connection, "XXXX"));
			assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
			assertTrue(dbmsSupport.isTablePresent(connection, schema, TEST_TABLE.toLowerCase()));
			assertTrue(dbmsSupport.isTablePresent(connection, schema, TEST_TABLE.toUpperCase()));
		}
	}

	@DatabaseTest
	public void testIsColumnPresent() throws Exception {
		try (Connection connection = env.getConnection()) {
			assertTrue(dbmsSupport.isColumnPresent(connection, TEST_TABLE, "TINT"));
			assertTrue(dbmsSupport.isColumnPresent(connection, TEST_TABLE, "tint"));
			assertFalse(dbmsSupport.isColumnPresent(connection, TEST_TABLE, "XXXX"));
			assertFalse(dbmsSupport.isColumnPresent(connection, "XXXX", "XXXX"));
			assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
			assertTrue(dbmsSupport.isColumnPresent(connection, TEST_TABLE.toLowerCase(), "TINT"));
			assertTrue(dbmsSupport.isColumnPresent(connection, TEST_TABLE.toUpperCase(), "TINT"));
		}
	}

	@DatabaseTest
	public void testIsColumnPresentInSchema() throws Exception {
		try (Connection connection = env.getConnection()) {
			String schema = dbmsSupport.getSchema(connection);
			assertTrue(dbmsSupport.isColumnPresent(connection, schema, TEST_TABLE, "TINT"));
			assertFalse(dbmsSupport.isColumnPresent(connection, schema, TEST_TABLE, "XXXX"));
			assertFalse(dbmsSupport.isColumnPresent(connection, schema, "XXXX", "XXXX"));
			assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
			assertTrue(dbmsSupport.isColumnPresent(connection, schema, TEST_TABLE.toLowerCase(), "TINT"));
			assertTrue(dbmsSupport.isColumnPresent(connection, schema, TEST_TABLE.toUpperCase(), "TINT"));
		}
	}

	@DatabaseTest
	public void testHasIndexOnColumn() throws Exception {
		try (Connection connection = env.getConnection()) {
			String schema = dbmsSupport.getSchema(connection);
			assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "TKEY"));
			assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "tkey"));
			assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "tINT")); // also check first column of multi column index
			assertFalse(dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "TBOOLEAN"));
			assertFalse(dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "tboolean"));
			assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
			assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE.toLowerCase(), "TKEY"));
			assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE.toUpperCase(), "TKEY"));
		}
	}

	@DatabaseTest
	public void testHasIndexOnColumns() throws Exception {
		try (Connection connection = env.getConnection()) {
			String schema = dbmsSupport.getSchema(connection);
			List<String> indexedColums = new ArrayList<>();
			indexedColums.add("tINT");
			indexedColums.add("tDATE");
			List<String> indexedColumsUC = new ArrayList<>();
			indexedColumsUC.add("tINT");
			indexedColumsUC.add("tDATE");
			List<String> indexedColumsLC = new ArrayList<>();
			indexedColumsLC.add("tINT");
			indexedColumsLC.add("tDATE");
			List<String> indexedColumsWrongOrder = new ArrayList<>();
			indexedColumsWrongOrder.add("tDATE");
			indexedColumsWrongOrder.add("tINT");
			assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE, indexedColums));
			assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE, indexedColumsUC));
			assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE, indexedColumsLC));
			assertFalse(dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE, indexedColumsWrongOrder));
			assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
			assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE.toLowerCase(), indexedColums));
			assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE.toUpperCase(), indexedColums));
		}
	}

	public void testGetTableColumns(String tableName) throws Exception {
		try (Connection connection = env.getConnection(); ResultSet rs = dbmsSupport.getTableColumns(connection, tableName)) {
			while (rs.next()) {
				String tablename = rs.getString("TABLE_NAME");
				String columnName = rs.getString("COLUMN_NAME");
				int datatype = rs.getInt("DATA_TYPE");
				int columnSize = rs.getInt("COLUMN_SIZE");

				if ("TINT".equalsIgnoreCase(columnName)) {
					return;
				}
			}
			fail("Column TINT not found");
		}
	}

	@DatabaseTest
	public void testGetTableColumns() throws Exception {
		testGetTableColumns(TEST_TABLE);
		assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumns(TEST_TABLE.toLowerCase());
		testGetTableColumns(TEST_TABLE.toUpperCase());
	}

	public void testGetTableColumnsInSchema(String tableName) throws Exception {
		try (Connection connection = env.getConnection()) {
			String schema = dbmsSupport.getSchema(connection);
			try (ResultSet rs = dbmsSupport.getTableColumns(connection, schema, tableName)) {
				while (rs.next()) {
					String tablename = rs.getString("TABLE_NAME");
					String columnName = rs.getString("COLUMN_NAME");
					int datatype = rs.getInt("DATA_TYPE");
					int columnSize = rs.getInt("COLUMN_SIZE");

					if ("TINT".equalsIgnoreCase(columnName)) {
						return;
					}
				}
				fail("Column TINT not found");
			}
		}
	}

	@DatabaseTest
	public void testGetTableColumnsInSchema() throws Exception {
		testGetTableColumnsInSchema(TEST_TABLE);
		assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumnsInSchema(TEST_TABLE.toLowerCase());
		testGetTableColumnsInSchema(TEST_TABLE.toUpperCase());
	}

	public void testGetTableColumnsSpecific(String tableName, String columNamePattern) throws Exception {
		try (Connection connection = env.getConnection(); ResultSet rs = dbmsSupport.getTableColumns(connection, null, tableName, columNamePattern)) {
			boolean foundTINT = false;
			boolean foundTCHAR = false;
			while (rs.next()) {
				String tablename = rs.getString("TABLE_NAME");
				String columnName = rs.getString("COLUMN_NAME");
				int datatype = rs.getInt("DATA_TYPE");
				int columnSize = rs.getInt("COLUMN_SIZE");

				if ("TINT".equalsIgnoreCase(columnName)) {
					foundTINT = true;
				}
				if ("TCHAR".equalsIgnoreCase(columnName)) {
					foundTCHAR = true;
				}
			}
			assertTrue(foundTINT);
			assertFalse(foundTCHAR);
		}
	}

	@DatabaseTest
	public void testGetTableColumnsSpecific() throws Exception {
		testGetTableColumnsSpecific(TEST_TABLE, "TINT");
		testGetTableColumnsSpecific(TEST_TABLE, "tint");
		assumeFalse(dbmsSupport.getDbms() == Dbms.MARIADB || dbmsSupport.getDbms() == Dbms.MYSQL); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumnsSpecific(TEST_TABLE.toLowerCase(), "TINT");
		testGetTableColumnsSpecific(TEST_TABLE.toUpperCase(), "TINT");
	}

	@DatabaseTest
	public void testGetDateTimeLiteral() throws Exception {
		try (Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(connection, "INSERT INTO " + TEST_TABLE + "(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (1,2,3," + dbmsSupport.getDateAndOffset(dbmsSupport.getDatetimeLiteral(new Date()), 4) + "," + dbmsSupport.getDatetimeLiteral(new Date()) + ")");
			Object result = JdbcTestUtil.executeQuery(dbmsSupport, connection, "SELECT " + dbmsSupport.getTimestampAsDate("TDATETIME") + " FROM " + TEST_TABLE + " WHERE TKEY=1", null, new PipeLineSession());
		}
	}

	@DatabaseTest
	public void testSysDate() throws Exception {
		try (Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(connection, "INSERT INTO " + TEST_TABLE + "(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (2,'xxx',3," + dbmsSupport.getSysDate() + "," + dbmsSupport.getSysDate() + ")");
			Object result = JdbcTestUtil.executeQuery(dbmsSupport, connection, "SELECT " + dbmsSupport.getTimestampAsDate("TDATETIME") + " FROM " + TEST_TABLE + " WHERE TKEY=2", null, new PipeLineSession());
		}
	}

	@DatabaseTest
	public void testNumericAsDouble() throws Exception {
		String number = "1234.5678";
		String query = "INSERT INTO " + TEST_TABLE + "(TKEY, TNUMBER) VALUES (3,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		log.debug("executing query [" + translatedQuery + "]");

		try (Connection connection = env.getConnection(); PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			stmt.setDouble(1, Double.parseDouble(number));
			stmt.execute();
		}

		try (Connection connection = env.getConnection(); PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER FROM " + TEST_TABLE + " WHERE TKEY=3", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertTrue(resultSet.getString(1).startsWith(number));
			}
		}
	}

	@DatabaseTest
	public void testNumericAsFloat() throws Exception {
		assumeFalse(dbmsSupport.getDbms() == Dbms.POSTGRESQL); // This fails on PostgreSQL, precision of setFloat appears to be too low"
		float number = 1234.5677F;
		String query = "INSERT INTO " + TEST_TABLE + "(TKEY, TNUMBER) VALUES (4,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");

		try (Connection connection = env.getConnection()) {
			try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
				stmt.setFloat(1, number);
				stmt.execute();
			}

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER FROM " + TEST_TABLE + " WHERE TKEY=4", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					assertEquals(number, resultSet.getFloat(1), 0.01);
				}
			}
		}
	}

	@DatabaseTest
	// test the alias functionality as used in JdbcTableListener.
	// Asserts that columns can be identified with and without alias.
	public void testSelectWithAlias() throws Exception {
		String insertQuery = "INSERT INTO " + TEST_TABLE + "(TKEY, TNUMBER, TVARCHAR) VALUES (5,5,'A')";
		String selectQuery = "SELECT TNUMBER FROM " + TEST_TABLE + " t WHERE TKEY=5 AND t.TVARCHAR='A'";

		try (Connection connection = env.getConnection()) {
			try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
				stmt.execute();
			}

			try (PreparedStatement stmt = executeTranslatedQuery(connection, selectQuery, QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					assertEquals(5, resultSet.getInt(1));
				}
			}
		}
	}

	@DatabaseTest
	public void testJdbcSetParameter() throws Exception {
		String number = "1234.5678";
		String datetime = DateFormatUtils.now(DateFormatUtils.GENERIC_DATETIME_FORMATTER);
		String date = DateFormatUtils.now(DateFormatUtils.ISO_DATE_FORMATTER);

		assumeFalse("Oracle".equals(dbmsSupport.getDbmsName())); // This fails on Oracle, cannot set a non-integer number via setString()
		String query = "INSERT INTO " + TEST_TABLE + "(TKEY, TNUMBER, TDATE, TDATETIME) VALUES (5,?,?,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");

		try (Connection connection = env.getConnection(); PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			JdbcUtil.setParameter(stmt, 1, number, dbmsSupport.isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 2, date, dbmsSupport.isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 3, datetime, dbmsSupport.isParameterTypeMatchRequired());
			stmt.execute();
		}

		try (Connection connection = env.getConnection(); PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER, TDATE, TDATETIME FROM " + TEST_TABLE + " WHERE TKEY=5", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertTrue(resultSet.getString(1).startsWith(number));
				assertEquals(date, resultSet.getString(2));
				assertTrue(resultSet.getString(3).startsWith(datetime));
			}
		}
	}


	@DatabaseTest
	public void testWriteAndReadClob() throws Exception {
		try (Connection connection = env.getConnection()) {
			String clobContents = "Dit is de content van de clob";
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TCLOB) VALUES (10,EMPTY_CLOB())", QueryType.OTHER);
			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM " + TEST_TABLE + " WHERE TKEY=10 FOR UPDATE", QueryType.SELECT, true)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					Object clobHandle = dbmsSupport.getClobHandle(resultSet, 1);
					try (Writer writer = dbmsSupport.getClobWriter(resultSet, 1, clobHandle)) {
						writer.append(clobContents);
					}
					dbmsSupport.updateClob(resultSet, 1, clobHandle);
					resultSet.updateRow();
				}
			}

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM " + TEST_TABLE + " WHERE TKEY=10", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
					String actual = StreamUtil.readerToString(clobReader, null);
					assertEquals(clobContents, actual);
				}
			}
		}
	}

	@DatabaseTest
	public void testReadEmptyClob() throws Exception {
		try (Connection connection = env.getConnection()) {
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TCLOB) VALUES (11,EMPTY_CLOB())", QueryType.OTHER);

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM " + TEST_TABLE + " WHERE TKEY=11", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
					String actual = StreamUtil.readerToString(clobReader, null);
					assertEquals("", actual);
				}
			}
		}
	}

	@DatabaseTest
	public void testReadNullClob() throws Exception {
		try (Connection connection = env.getConnection()) {
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY) VALUES (11)", QueryType.OTHER);

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM " + TEST_TABLE + " WHERE TKEY=11", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					assertNull(dbmsSupport.getClobReader(resultSet, 1));
					assertTrue(resultSet.wasNull());
				}
			}
		}
	}


	@DatabaseTest
	public void testWriteClobInOneStep() throws Exception {
		try (Connection connection = env.getConnection()) {
			String clobContents = "Dit is de content van de clob";
			String query = "INSERT INTO " + TEST_TABLE + " (TKEY,TCLOB) VALUES (12,?)";
			String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
			try (PreparedStatement stmt = connection.prepareStatement(translatedQuery);) {
				stmt.setString(1, clobContents);
				stmt.execute();
			}

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM " + TEST_TABLE + " WHERE TKEY=12", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
					String actual = StreamUtil.readerToString(clobReader, null);
					assertEquals(clobContents, actual);
				}
			}
		}
	}

	@DatabaseTest
	public void testWriteAndReadBlob() throws Exception {
		try (Connection connection = env.getConnection()) {
			String blobContents = "Dit is de content van de blob";
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TBLOB) VALUES (20,EMPTY_BLOB())", QueryType.OTHER);
			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM " + TEST_TABLE + " WHERE TKEY=20 FOR UPDATE", QueryType.SELECT, true)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					Object blobHandle = dbmsSupport.getBlobHandle(resultSet, 1);
					try (OutputStream out = dbmsSupport.getBlobOutputStream(resultSet, 1, blobHandle)) {
						out.write(blobContents.getBytes("UTF-8"));
					}
					dbmsSupport.updateBlob(resultSet, 1, blobHandle);
					resultSet.updateRow();
				}
			}
			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM " + TEST_TABLE + " WHERE TKEY=20", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					InputStream blobStream = dbmsSupport.getBlobInputStream(resultSet, 1);
					String actual = StreamUtil.streamToString(blobStream, null, "UTF-8");
					assertEquals(blobContents, actual);
				}
			}
		}
	}


	@DatabaseTest
	public void testWriteAndReadBlobCompressed() throws Exception {
		try (Connection connection = env.getConnection()) {
			String blobContents = "Dit is de content van de blob";
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TBLOB) VALUES (21,EMPTY_BLOB())", QueryType.OTHER);
			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM " + TEST_TABLE + " WHERE TKEY=21 FOR UPDATE", QueryType.SELECT, true)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					Object blobHandle = dbmsSupport.getBlobHandle(resultSet, 1);

					try (OutputStream blobOutputStream = JdbcUtil.getBlobOutputStream(dbmsSupport, blobHandle, resultSet, 1, true)) {
						blobOutputStream.write(blobContents.getBytes("UTF-8"));
					}
					dbmsSupport.updateBlob(resultSet, 1, blobHandle);
					resultSet.updateRow();
				}
			}
			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM " + TEST_TABLE + " WHERE TKEY=21", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					String actual = JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", true, false, false);
					assertEquals(blobContents, actual);
				}
			}
		}
	}

	@DatabaseTest
	public void testReadEmptyBlob() throws Exception {
		try (Connection connection = env.getConnection()) {
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TBLOB) VALUES (22,EMPTY_BLOB())", QueryType.OTHER);

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM " + TEST_TABLE + " WHERE TKEY=22", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					InputStream inputStream = dbmsSupport.getBlobInputStream(resultSet, 1);
					String actual = StreamUtil.streamToString(inputStream, null, null);
					assertEquals("", actual);
				}
			}
		}
	}

	@DatabaseTest
	public void testReadNullBlob() throws Exception {
		try (Connection connection = env.getConnection()) {
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY) VALUES (23)", QueryType.OTHER);

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM " + TEST_TABLE + " WHERE TKEY=23", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					assertNull(dbmsSupport.getClobReader(resultSet, 1));
					assertTrue(resultSet.wasNull());
				}
			}
		}
	}

	@DatabaseTest
	public void testWriteBlobInOneStep() throws Exception {
		try (Connection connection = env.getConnection()) {
			String blobContents = "Dit is de content van de blob";
			String query = "INSERT INTO " + TEST_TABLE + " (TKEY,TBLOB) VALUES (24,?)";
			String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
			try (PreparedStatement stmt = connection.prepareStatement(translatedQuery);) {
				stmt.setBytes(1, blobContents.getBytes("UTF-8"));
				stmt.execute();
			}

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM " + TEST_TABLE + " WHERE TKEY=24", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					resultSet.next();
					String actual = JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", false, false, false);
					assertEquals(blobContents, actual);
				}
			}
		}
	}

	@DatabaseTest
	public void testReadBlobAndCLobUsingJdbcUtilGetValue() throws Exception {
		try (Connection connection = env.getConnection()) {
			String blobContents = "Dit is de content van de blob";
			String clobContents = "Dit is de content van de clob";
			String query = "INSERT INTO " + TEST_TABLE + " (TKEY,TBLOB,TCLOB) VALUES (24,?,?)";
			String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
			try (PreparedStatement stmt = connection.prepareStatement(translatedQuery);) {
				stmt.setBytes(1, blobContents.getBytes("UTF-8"));
				stmt.setString(2, clobContents);
				stmt.execute();
			}

			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB,TCLOB FROM " + TEST_TABLE + " WHERE TKEY=24", QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = resultSet.getMetaData();
					resultSet.next();
					String actual1 = JdbcUtil.getValue(dbmsSupport, resultSet, 1, rsmeta, "UTF-8", false, null, true, false, false);
					String actual2 = JdbcUtil.getValue(dbmsSupport, resultSet, 2, rsmeta, "UTF-8", false, null, true, false, false);
					assertEquals(blobContents, actual1);
					assertEquals(clobContents, actual2);
				}
			}
		}
	}

	@DatabaseTest
	public void testBooleanHandling() throws Exception {
		try (Connection connection = env.getConnection()) {
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TBOOLEAN) VALUES (30,99," + dbmsSupport.getBooleanValue(false) + ")", QueryType.OTHER);
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TBOOLEAN) VALUES (31,99," + dbmsSupport.getBooleanValue(true) + ")", QueryType.OTHER);

			assertEquals(30, JdbcTestUtil.executeIntQuery(connection, "SELECT TKEY FROM " + TEST_TABLE + " WHERE TINT=99 AND TBOOLEAN=" + dbmsSupport.getBooleanValue(false)));
			assertEquals(31, JdbcTestUtil.executeIntQuery(connection, "SELECT TKEY FROM " + TEST_TABLE + " WHERE TINT=99 AND TBOOLEAN=" + dbmsSupport.getBooleanValue(true)));
		}
	}

	private boolean peek(String query) throws Exception {
		try (Connection peekConnection = env.getConnection()) {
			return !JdbcUtil.isQueryResultEmpty(peekConnection, query);
		}
	}

	@DatabaseTest
	public void testQueueHandling() throws Exception {
		assumeTrue(dbmsSupport.hasSkipLockedFunctionality(), "This test works only when locked records can be skipped, not supported for DBMS [" + dbmsSupport.getDbmsName() + "]");
		try (Connection connection = env.getConnection()) {
			executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (40,100)", QueryType.OTHER);

			String selectQuery = "SELECT TKEY FROM " + TEST_TABLE + " WHERE TINT=100";
			assertEquals(40, JdbcTestUtil.executeIntQuery(connection, selectQuery));

			String readQueueQuery = dbmsSupport.prepareQueryTextForWorkQueueReading(1, selectQuery);
			String peekQueueQuery = dbmsSupport.prepareQueryTextForWorkQueuePeeking(1, selectQuery);

			// test that peek and read find records when they are available
			assertEquals(40, JdbcTestUtil.executeIntQuery(connection, peekQueueQuery));
			assertEquals(40, JdbcTestUtil.executeIntQuery(connection, readQueueQuery));
			assertEquals(40, JdbcTestUtil.executeIntQuery(connection, peekQueueQuery));

			try (Connection lockingReadConnection = env.getConnection()) {
				lockingReadConnection.setAutoCommit(false);
				try (Statement stmt1 = lockingReadConnection.createStatement()) {
					stmt1.setFetchSize(1);
					log.debug("Read queue using query [" + readQueueQuery + "]");
					try (ResultSet rs1 = stmt1.executeQuery(readQueueQuery)) {
						assertTrue(rs1.next());
						assertEquals(40, rs1.getInt(1));            // find the first record

						try (Connection workConn2 = env.getConnection()) {
							workConn2.setAutoCommit(false);
							try (Statement stmt2 = workConn2.createStatement()) {
								stmt2.setFetchSize(1);
								try (ResultSet rs2 = stmt2.executeQuery(readQueueQuery)) {
									if (rs2.next()) { // shouldn't find record in QueueReading mode either
										fail("readQueueQuery [" + readQueueQuery + "] should not have found record [" + rs2.getString(1) + "] that is already locked");
									}
								}
							} finally {
								workConn2.commit();
							}
						}

						// insert another record
						executeTranslatedQuery(connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (41,100)", QueryType.OTHER);
						if (testPeekFindsRecordsWhenTheyAreAvailable)
							assertTrue(peek(peekQueueQuery), "second record should have been seen by peek query");// assert that record is seen

						try (Connection workConn2 = env.getConnection()) {
							workConn2.setAutoCommit(false);
							try (Statement stmt2 = workConn2.createStatement()) {
								stmt2.setFetchSize(1);
								try (ResultSet rs2 = stmt2.executeQuery(readQueueQuery)) {
									assertTrue(rs2.next());
									assertEquals(41, rs2.getInt(1));    // find the second record
								}
							} finally {
								workConn2.rollback();
							}
						}
					}
				} finally {
					lockingReadConnection.commit();
				}
			}
		}
	}

	@DatabaseTest
	public void testIsBlobType() throws SQLException {
		try (Connection connection = env.getConnection()) {
			try (PreparedStatement stmt = connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM " + TEST_TABLE)) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 9; i++) {
						assertEquals(i == 8, dbmsSupport.isBlobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}

			}
		}
	}


	@DatabaseTest
	public void testIsClobType() throws SQLException {
		try (Connection connection = env.getConnection()) {
			try (PreparedStatement stmt = connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM " + TEST_TABLE)) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 9; i++) {
						assertEquals(i == 9, dbmsSupport.isClobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}

			}
		}
	}

	@DatabaseTest
	public void testSkipLockedSupportPresent() {
		// We expect this test to run against a MariaDB version 10.6 or later and so it should support "skip locked" when running these tests
		boolean expectSkipLockedSupport = dbmsSupport.getDbms() != Dbms.H2;

		assertEquals(expectSkipLockedSupport, dbmsSupport.hasSkipLockedFunctionality());
	}

	protected PreparedStatement executeTranslatedQuery(Connection connection, String query, QueryType queryType) throws JdbcException, SQLException {
		return executeTranslatedQuery(connection, query, queryType, false);
	}

	protected PreparedStatement executeTranslatedQuery(Connection connection, String query, QueryType queryType, boolean selectForUpdate) throws JdbcException, SQLException {
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");

		log.debug("executing translated query [{}]", translatedQuery);
		if (queryType==QueryType.SELECT) {
			if(!selectForUpdate) {
				return connection.prepareStatement(translatedQuery);
			}
			return connection.prepareStatement(translatedQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}
		JdbcTestUtil.executeStatement(connection, translatedQuery);
		return null;
	}

	@DatabaseTest
	@WithLiquibase(file = "Migrator/AddTableForDatabaseContext.xml", tableName = ROW_VERSION_TEST_TABLE_NAME)
	public void testRowVersionTimestamp() throws SQLException, JdbcException, IOException {
		assumeTrue(dbmsSupport.getDbms() == Dbms.MSSQL, "This test is only relevant for MSSQL");

		try (Connection connection = env.getConnection()) {
			try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT * FROM " + ROW_VERSION_TEST_TABLE_NAME, QueryType.SELECT)) {
				try (ResultSet resultSet = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = resultSet.getMetaData();
					resultSet.next();
					String actual2 = JdbcUtil.getValue(dbmsSupport, resultSet, 3, rsmeta, "UTF-8", false, null, true, false, false);

					assertEquals(rsmeta.getColumnTypeName(3), "timestamp");
					assertNotNull(actual2);
				}
			}
		}
	}
}
