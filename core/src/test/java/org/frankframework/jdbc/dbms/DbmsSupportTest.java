package org.frankframework.jdbc.dbms;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.jdbc.JdbcQuerySenderBase.QueryType;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.DbmsUtil;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.hamcrest.core.StringStartsWith;
import org.hamcrest.text.IsEmptyString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import lombok.Getter;

@WithLiquibase(tableName = DbmsSupportTest.TABLE_NAME, file = "Migrator/ChangelogBlobTests.xml")
public class DbmsSupportTest {
	private final boolean testPeekFindsRecordsWhenTheyAreAvailable = true;
	protected static final String TABLE_NAME = "DST_TABLE";
	protected static Logger log = LogUtil.getLogger(DbmsSupportTest.class);
	protected Properties dataSourceInfo;
	private DataSource dataSource;
	protected boolean testPeekShouldSkipRecordsAlreadyLocked = false;

	@DatabaseTest.Parameter(0)
	private TransactionManagerType transactionManagerType;

	@DatabaseTest.Parameter(1)
	private String dataSourceName;

	private TestConfiguration getConfiguration() {
		return transactionManagerType.getConfigurationContext(dataSourceName);
	}

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		databaseTestEnvironment.getConnection().setAutoCommit(true);
	}

	@AfterEach
	public void tearDown(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		databaseTestEnvironment.getConnection().close();
	}

	@DatabaseTest
	public void testGetDbmsSupport(DatabaseTestEnvironment databaseTestEnvironment) {
		assertNotNull(databaseTestEnvironment.getDbmsSupport());
	}

	@DatabaseTest
	public void testNameEqualsDbmsKey(DatabaseTestEnvironment databaseTestEnvironment) {
		assertEquals(databaseTestEnvironment.getDataSourceName(), databaseTestEnvironment.getDbmsSupport().getDbmsName());
		assertEquals(databaseTestEnvironment.getDataSourceName(), databaseTestEnvironment.getDbmsSupport().getDbms().getKey());
	}

	@DatabaseTest
	public void testTableLessSelect(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		Connection connection = databaseTestEnvironment.getConnection();
		assertEquals(4, DbmsUtil.executeIntQuery(connection, "SELECT 2+2 " + databaseTestEnvironment.getDbmsSupport().getFromForTablelessSelect()));
		connection.close();
	}

	@DatabaseTest
	public void testTableLessSelectWithIntParam(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		Connection connection = databaseTestEnvironment.getConnection();
		assertEquals(4, DbmsUtil.executeIntQuery(connection, "SELECT 1+? " + databaseTestEnvironment.getDbmsSupport().getFromForTablelessSelect(), 3));
		connection.close();
	}

	@DatabaseTest
	public void testInsertSelect(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		Connection connection = databaseTestEnvironment.getConnection();
		JdbcUtil.executeStatement(connection, "INSERT INTO " + TABLE_NAME + " (TKEY, TINT) SELECT 11, 2+2 " + databaseTestEnvironment.getDbmsSupport().getFromForTablelessSelect() + " WHERE 1=1");
		connection.close();
	}

	@DatabaseTest
	public void testIsTablePresent(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		Connection connection = databaseTestEnvironment.getConnection();
		assertTrue(dbmsSupport.isTablePresent(connection, TABLE_NAME), "Should have found existing table");
		assertFalse(dbmsSupport.isTablePresent(connection, "XXXX"));
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(dbmsSupport.isTablePresent(connection, TABLE_NAME.toLowerCase()), "Should have found existing table");
		assertTrue(dbmsSupport.isTablePresent(connection, TABLE_NAME.toUpperCase()), "Should have found existing table");
		connection.close();
	}

	@DatabaseTest
	public void testIsTablePresentInSchema(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		Connection connection = databaseTestEnvironment.getConnection();
		String schema = dbmsSupport.getSchema(connection);
		assertTrue(dbmsSupport.isTablePresent(connection, schema, TABLE_NAME), "Should have found existing table in schema");
		assertFalse(dbmsSupport.isTablePresent(connection, "XXXX"));
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(dbmsSupport.isTablePresent(connection, schema, TABLE_NAME.toLowerCase()), "Should have found existing table in schema");
		assertTrue(dbmsSupport.isTablePresent(connection, schema, TABLE_NAME.toUpperCase()), "Should have found existing table in schema");
		connection.close();
	}

	@DatabaseTest
	public void testIsColumnPresent(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		Connection connection = databaseTestEnvironment.getConnection();
		assertTrue(dbmsSupport.isColumnPresent(connection, TABLE_NAME, "TINT"), "Should have found existing column");
		assertTrue(dbmsSupport.isColumnPresent(connection, TABLE_NAME, "tint"), "Should have found existing column");
		assertFalse(dbmsSupport.isColumnPresent(connection, TABLE_NAME, "XXXX"));
		assertFalse(dbmsSupport.isColumnPresent(connection, "XXXX", "XXXX"));
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(dbmsSupport.isColumnPresent(connection, TABLE_NAME.toLowerCase(), "TINT"), "Should have found existing column");
		assertTrue(dbmsSupport.isColumnPresent(connection, TABLE_NAME.toUpperCase(), "TINT"), "Should have found existing column");
		connection.close();
	}

	@DatabaseTest
	public void testIsColumnPresentInSchema(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		Connection connection = databaseTestEnvironment.getConnection();
		String schema = dbmsSupport.getSchema(connection);
		assertTrue(dbmsSupport.isColumnPresent(connection, schema, TABLE_NAME, "TINT"), "Should have found existing column in schema [" + schema + "]");
		assertFalse(dbmsSupport.isColumnPresent(connection, schema, TABLE_NAME, "XXXX"));
		assertFalse(dbmsSupport.isColumnPresent(connection, schema, "XXXX", "XXXX"));
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(dbmsSupport.isColumnPresent(connection, schema, TABLE_NAME.toLowerCase(), "TINT"), "Should have found existing column in schema [" + schema + "]");
		assertTrue(dbmsSupport.isColumnPresent(connection, schema, TABLE_NAME.toUpperCase(), "TINT"), "Should have found existing column in schema [" + schema + "]");
		connection.close();
	}

	@DatabaseTest
	public void testHasIndexOnColumn(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		Connection connection = databaseTestEnvironment.getConnection();
		String schema = dbmsSupport.getSchema(connection);
		assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TABLE_NAME, "TKEY"), "Should have been index on primary key column");
		assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TABLE_NAME, "tkey"), "Should have been index on primary key column");
		assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TABLE_NAME, "tINT"), "Should have been index on column"); // also check first column of multi column index
		assertFalse(dbmsSupport.hasIndexOnColumn(connection, schema, TABLE_NAME, "TBOOLEAN"), "Should not have been index on column");
		assertFalse(dbmsSupport.hasIndexOnColumn(connection, schema, TABLE_NAME, "tboolean"), "Should not have been index on column");
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TABLE_NAME.toLowerCase(), "TKEY"), "Should have been index on primary key column");
		assertTrue(dbmsSupport.hasIndexOnColumn(connection, schema, TABLE_NAME.toUpperCase(), "TKEY"), "Should have been index on primary key column");
		connection.close();
	}

	@DatabaseTest
	public void testHasIndexOnColumns(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		Connection connection = databaseTestEnvironment.getConnection();
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
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
		assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TABLE_NAME, indexedColums), "Should have been index on columns");
		assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TABLE_NAME, indexedColumsUC), "Should have been index on columns");
		assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TABLE_NAME, indexedColumsLC), "Should have been index on columns");
		assertFalse(dbmsSupport.hasIndexOnColumns(connection, schema, TABLE_NAME, indexedColumsWrongOrder), "Should not have been index on columns");
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TABLE_NAME.toLowerCase(), indexedColums), "Should have been index on columns");
		assertTrue(dbmsSupport.hasIndexOnColumns(connection, schema, TABLE_NAME.toUpperCase(), indexedColums), "Should have been index on columns");
		connection.close();
	}

	public void testGetTableColumns(String tableName, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		try (ResultSet rs = databaseTestEnvironment.getDbmsSupport().getTableColumns(databaseTestEnvironment.getConnection(), tableName)) {
			while (rs.next()) {
				String tablename = rs.getString("TABLE_NAME");
				String columnName = rs.getString("COLUMN_NAME");
				int datatype = rs.getInt("DATA_TYPE");
				int columnSize = rs.getInt("COLUMN_SIZE");

				if (columnName.equalsIgnoreCase("TINT")) {
					return;
				}
			}
			fail("Column TINT not found");
		}
	}

	@DatabaseTest
	public void testGetTableColumns(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetTableColumns(TABLE_NAME, databaseTestEnvironment);
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumns(TABLE_NAME.toLowerCase(), databaseTestEnvironment);
		testGetTableColumns(TABLE_NAME.toUpperCase(), databaseTestEnvironment);
	}

	public void testGetTableColumnsInSchema(String tableName, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		Connection connection = databaseTestEnvironment.getConnection();
		String schema = databaseTestEnvironment.getDbmsSupport().getSchema(connection);
		try (ResultSet rs = databaseTestEnvironment.getDbmsSupport().getTableColumns(connection, schema, tableName)) {
			while (rs.next()) {
				String tablename = rs.getString("TABLE_NAME");
				String columnName = rs.getString("COLUMN_NAME");
				int datatype = rs.getInt("DATA_TYPE");
				int columnSize = rs.getInt("COLUMN_SIZE");

				if (columnName.equalsIgnoreCase("TINT")) {
					connection.close();
					return;
				}
			}
			connection.close();
			fail("Column TINT not found");
		}
		connection.close();
	}

	@DatabaseTest
	public void testGetTableColumnsInSchema(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetTableColumnsInSchema(TABLE_NAME, databaseTestEnvironment);
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumnsInSchema(TABLE_NAME.toLowerCase(), databaseTestEnvironment);
		testGetTableColumnsInSchema(TABLE_NAME.toUpperCase(), databaseTestEnvironment);
	}

	public void testGetTableColumnsSpecific(String tableName, String columNamePattern, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		try (ResultSet rs = databaseTestEnvironment.getDbmsSupport().getTableColumns(databaseTestEnvironment.getConnection(),
				null, tableName, columNamePattern)) {
			boolean foundTINT = false;
			boolean foundTCHAR = false;
			while (rs.next()) {
				String tablename = rs.getString("TABLE_NAME");
				String columnName = rs.getString("COLUMN_NAME");
				int datatype = rs.getInt("DATA_TYPE");
				int columnSize = rs.getInt("COLUMN_SIZE");

				if (columnName.equalsIgnoreCase("TINT")) {
					foundTINT = true;
				}
				if (columnName.equalsIgnoreCase("TCHAR")) {
					foundTCHAR = true;
				}
			}
			assertTrue(foundTINT);
			assertFalse(foundTCHAR);
		}
	}

	@DatabaseTest
	public void testGetTableColumnsSpecific(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetTableColumnsSpecific(TABLE_NAME, "TINT", databaseTestEnvironment);
		testGetTableColumnsSpecific(TABLE_NAME, "tint", databaseTestEnvironment);
		assumeThat(databaseTestEnvironment.getDataSourceName(), not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumnsSpecific(TABLE_NAME.toLowerCase(), "TINT", databaseTestEnvironment);
		testGetTableColumnsSpecific(TABLE_NAME.toUpperCase(), "TINT", databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetDateTimeLiteral(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		Connection connection = databaseTestEnvironment.getConnection();
		JdbcUtil.executeStatement(connection, "INSERT INTO " + TABLE_NAME + "(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (1,2,3," + databaseTestEnvironment.getDbmsSupport().getDateAndOffset(databaseTestEnvironment.getDbmsSupport().getDatetimeLiteral(new Date()), 4) + "," + databaseTestEnvironment.getDbmsSupport().getDatetimeLiteral(new Date()) + ")");
		Object result = JdbcUtil.executeQuery(databaseTestEnvironment.getDbmsSupport(), connection, "SELECT " + databaseTestEnvironment.getDbmsSupport().getTimestampAsDate("TDATETIME") + " FROM " + TABLE_NAME + " WHERE TKEY=1", null, new PipeLineSession());
		System.out.println("result:" + result);
		connection.close();
	}

	@DatabaseTest
	public void testSysDate(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		Connection connection = databaseTestEnvironment.getConnection();
		JdbcUtil.executeStatement(connection, "INSERT INTO " + TABLE_NAME + "(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (2,'xxx',3," + databaseTestEnvironment.getDbmsSupport().getSysDate() + "," + databaseTestEnvironment.getDbmsSupport().getSysDate() + ")");
		Object result = JdbcUtil.executeQuery(databaseTestEnvironment.getDbmsSupport(), connection, "SELECT " + databaseTestEnvironment.getDbmsSupport().getTimestampAsDate("TDATETIME") + " FROM " + TABLE_NAME + " WHERE TKEY=2", null, new PipeLineSession());
		System.out.println("result:" + result);
		connection.close();
	}

	@DatabaseTest
	public void testNumericAsDouble(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String number = "1234.5678";
		String query = "INSERT INTO " + TABLE_NAME + "(TKEY, TNUMBER) VALUES (3,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		System.out.println("executing query [" + translatedQuery + "]");
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			stmt.setDouble(1, Double.parseDouble(number));
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TNUMBER FROM " + TABLE_NAME + " WHERE TKEY=3", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
			}
		}
		connection.close();
	}

	@DatabaseTest
	public void testNumericAsFloat(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(databaseTestEnvironment.getDbmsSupport().getDbms() == Dbms.POSTGRESQL); // This fails on PostgreSQL, precision of setFloat appears to be too low"
		float number = 1234.5677F;
		String query = "INSERT INTO " + TABLE_NAME + "(TKEY, TNUMBER) VALUES (4,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		System.out.println("executing query [" + translatedQuery + "]");
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			stmt.setFloat(1, number);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TNUMBER FROM " + TABLE_NAME + " WHERE TKEY=4", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertEquals(number, resultSet.getFloat(1), 0.01);
			}
		}
		connection.close();
	}

	@DatabaseTest
	// test the alias functionality as used in JdbcTableListener.
	// Asserts that columns can be identified with and without alias.
	public void testSelectWithAlias(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String insertQuery = "INSERT INTO " + TABLE_NAME + "(TKEY, TNUMBER, TVARCHAR) VALUES (5,5,'A')";
		String selectQuery = "SELECT TNUMBER FROM " + TABLE_NAME + " t WHERE TKEY=5 AND t.TVARCHAR='A'";
		System.out.println("executing query [" + insertQuery + "]");
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, selectQuery, QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertEquals(5, resultSet.getInt(1));
			}
		}
		connection.close();
	}

	@DatabaseTest
	public void testJdbcSetParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String number = "1234.5678";
		String datetime = DateFormatUtils.now(DateFormatUtils.GENERIC_DATETIME_FORMATTER);
		String date = DateFormatUtils.now(DateFormatUtils.ISO_DATE_FORMATTER);

		assumeFalse(databaseTestEnvironment.getDbmsSupport().getDbmsName().equals("Oracle")); // This fails on Oracle, cannot set a non-integer number via setString()
		String query = "INSERT INTO " + TABLE_NAME + "(TKEY, TNUMBER, TDATE, TDATETIME) VALUES (5,?,?,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		System.out.println("executing query [" + translatedQuery + "]");
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			JdbcUtil.setParameter(stmt, 1, number, databaseTestEnvironment.getDbmsSupport().isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 2, date, databaseTestEnvironment.getDbmsSupport().isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 3, datetime, databaseTestEnvironment.getDbmsSupport().isParameterTypeMatchRequired());
			//JdbcUtil.setParameter(stmt, 4, bool, dbmsSupport.isParameterTypeMatchRequired());
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TNUMBER, TDATE, TDATETIME FROM " + TABLE_NAME + " WHERE TKEY=5", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
				assertEquals(date, resultSet.getString(2));
				assertThat(resultSet.getString(3), StringStartsWith.startsWith(datetime));
				//assertEquals(Boolean.parseBoolean(bool), resultSet.getBoolean(4));
			}
		}
		connection.close();
	}

	@DatabaseTest
	public void testWriteAndReadClob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String clobContents = "Dit is de content van de clob";
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TCLOB) VALUES (10,EMPTY_CLOB())", QueryType.OTHER);
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TCLOB FROM " + TABLE_NAME + " WHERE TKEY=10 FOR UPDATE", QueryType.SELECT, true)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Object clobHandle = databaseTestEnvironment.getDbmsSupport().getClobHandle(resultSet, 1);
				try (Writer writer = databaseTestEnvironment.getDbmsSupport().getClobWriter(resultSet, 1, clobHandle)) {
					writer.append(clobContents);
				}
				databaseTestEnvironment.getDbmsSupport().updateClob(resultSet, 1, clobHandle);
				resultSet.updateRow();
			}
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TCLOB FROM " + TABLE_NAME + " WHERE TKEY=10", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = databaseTestEnvironment.getDbmsSupport().getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals(clobContents, actual);
			}
		}
	}

	@DatabaseTest
	public void testReadEmptyClob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TCLOB) VALUES (11,EMPTY_CLOB())", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TCLOB FROM " + TABLE_NAME + " WHERE TKEY=11", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = databaseTestEnvironment.getDbmsSupport().getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals("", actual);
			}
		}
	}

	@DatabaseTest
	public void testReadNullClob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY) VALUES (11)", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TCLOB FROM " + TABLE_NAME + " WHERE TKEY=11", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertNull(databaseTestEnvironment.getDbmsSupport().getClobReader(resultSet, 1));
				assertTrue(resultSet.wasNull());
			}
		}
	}

	@DatabaseTest
	public void testWriteClobInOneStep(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String clobContents = "Dit is de content van de clob";
		String query = "INSERT INTO " + TABLE_NAME + " (TKEY,TCLOB) VALUES (12,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement(translatedQuery);) {
			stmt.setString(1, clobContents);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TCLOB FROM " + TABLE_NAME + " WHERE TKEY=12", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = databaseTestEnvironment.getDbmsSupport().getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals(clobContents, actual);
			}
		}

	}

	@DatabaseTest
	public void testInsertEmptyClobUsingDbmsSupport(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		Connection connection = databaseTestEnvironment.getConnection();
		JdbcUtil.executeStatement(connection, "INSERT INTO " + TABLE_NAME + " (TKEY,TCLOB) VALUES (13," + databaseTestEnvironment.getDbmsSupport().emptyClobValue() + ")");

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TCLOB FROM " + TABLE_NAME + " WHERE TKEY=13", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(JdbcUtil.getClobAsString(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, false), IsEmptyString.isEmptyOrNullString());
				connection.close();
			}
			connection.close();
		}
		connection.close();
	}

	@DatabaseTest
	public void testWriteAndReadBlob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = "Dit is de content van de blob";
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB) VALUES (20,EMPTY_BLOB())", QueryType.OTHER);
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=20 FOR UPDATE", QueryType.SELECT, true)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Object blobHandle = dbmsSupport.getBlobHandle(resultSet, 1);
				try (OutputStream out = dbmsSupport.getBlobOutputStream(resultSet, 1, blobHandle)) {
					out.write(blobContents.getBytes(StandardCharsets.UTF_8));
				}
				dbmsSupport.updateBlob(resultSet, 1, blobHandle);
				resultSet.updateRow();
			}
		}
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=20", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream blobStream = dbmsSupport.getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(blobStream, null, "UTF-8");
				assertEquals(blobContents, actual);
			}
		}
	}

	@DatabaseTest
	public void testWriteAndReadBlobCompressed(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = "Dit is de content van de blob";
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB) VALUES (21,EMPTY_BLOB())", QueryType.OTHER);
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=21 FOR UPDATE", QueryType.SELECT, true)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Object blobHandle = databaseTestEnvironment.getDbmsSupport().getBlobHandle(resultSet, 1);

				try (OutputStream blobOutputStream = JdbcUtil.getBlobOutputStream(databaseTestEnvironment.getDbmsSupport(), blobHandle, resultSet, 1, true)) {
					blobOutputStream.write(blobContents.getBytes("UTF-8"));
				}
				databaseTestEnvironment.getDbmsSupport().updateBlob(resultSet, 1, blobHandle);
				resultSet.updateRow();
			}
		}
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=21", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				String actual = JdbcUtil.getBlobAsString(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, "UTF-8", true, false, false);
				assertEquals(blobContents, actual);
			}
		}
	}

	@DatabaseTest
	public void testReadEmptyBlob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB) VALUES (22,EMPTY_BLOB())", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=22", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream inputStream = databaseTestEnvironment.getDbmsSupport().getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(inputStream, null, null);
				assertEquals("", actual);
			}
		}
	}

	@DatabaseTest
	public void testReadNullBlob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY) VALUES (23)", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=23", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertNull(databaseTestEnvironment.getDbmsSupport().getClobReader(resultSet, 1));
				assertTrue(resultSet.wasNull());
			}
		}
	}

	@DatabaseTest
	public void testWriteBlobInOneStep(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = "Dit is de content van de blob";
		String query = "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB) VALUES (24,?)";
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery);) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.execute();
			connection.close();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=24", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				String actual = JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", false, false, false);
				assertEquals(blobContents, actual);
				connection.close();
			}
		}
		connection.close();
	}

	@DatabaseTest
	public void testInsertEmptyBlobUsingDbmsSupport(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		Connection connection = databaseTestEnvironment.getConnection();
		JdbcUtil.executeStatement(connection, "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB) VALUES (25," + databaseTestEnvironment.getDbmsSupport().emptyBlobValue() + ")");

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=25", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(JdbcUtil.getBlobAsString(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, "UTF-8", false, false, false), IsEmptyString.isEmptyOrNullString());
				connection.close();
			}
			connection.close();
		}
		connection.close();
	}

	@DatabaseTest
	public void testReadBlobAndCLobUsingJdbcUtilGetValue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = "Dit is de content van de blob";
		String clobContents = "Dit is de content van de clob";
		String query = "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB,TCLOB) VALUES (24,?,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		Connection connection = databaseTestEnvironment.getConnection();
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery);) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.setString(2, clobContents);
			stmt.execute();
			connection.close();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment, "SELECT TBLOB,TCLOB FROM " + TABLE_NAME + " WHERE TKEY=24", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				ResultSetMetaData rsmeta = resultSet.getMetaData();
				resultSet.next();
				String actual1 = JdbcUtil.getValue(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, rsmeta, "UTF-8", false, null, true, false, false);
				String actual2 = JdbcUtil.getValue(databaseTestEnvironment.getDbmsSupport(), resultSet, 2, rsmeta, "UTF-8", false, null, true, false, false);
				assertEquals(blobContents, actual1);
				assertEquals(clobContents, actual2);
				connection.close();
			}
		}
		connection.close();
	}

	@DatabaseTest
	public void testBooleanHandling(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TINT,TBOOLEAN) VALUES (30,99," + databaseTestEnvironment.getDbmsSupport().getBooleanValue(false) + ")", QueryType.OTHER);
		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TINT,TBOOLEAN) VALUES (31,99," + databaseTestEnvironment.getDbmsSupport().getBooleanValue(true) + ")", QueryType.OTHER);

		Connection connection = databaseTestEnvironment.getConnection();
		assertEquals(30, DbmsUtil.executeIntQuery(connection, "SELECT TKEY FROM " + TABLE_NAME + " WHERE TINT=99 AND TBOOLEAN=" + databaseTestEnvironment.getDbmsSupport().getBooleanValue(false)));
		assertEquals(31, DbmsUtil.executeIntQuery(connection, "SELECT TKEY FROM " + TABLE_NAME + " WHERE TINT=99 AND TBOOLEAN=" + databaseTestEnvironment.getDbmsSupport().getBooleanValue(true)));
		connection.close();
	}

	private boolean peek(String query, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		Connection connection = databaseTestEnvironment.getConnection();
		try (Connection peekConnection = connection) {
			return !JdbcUtil.isQueryResultEmpty(peekConnection, query);
		} finally {
			connection.close();
		}
	}

//	@DatabaseTest
//	public void testQueueHandling(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
//		executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (40,100)", QueryType.OTHER);
//
//		String selectQuery = "SELECT TKEY FROM " + TABLE_NAME + " WHERE TINT=100";
//		Connection connection = databaseTestEnvironment.getConnection();
//		assertEquals(40, DbmsUtil.executeIntQuery(connection, selectQuery));
//
//		String readQueueQuery = databaseTestEnvironment.getDbmsSupport().prepareQueryTextForWorkQueueReading(1, selectQuery);
//		String peekQueueQuery = databaseTestEnvironment.getDbmsSupport().prepareQueryTextForWorkQueuePeeking(1, selectQuery);
//
//		// test that peek and read find records when they are available
//		assertEquals(40, DbmsUtil.executeIntQuery(connection, peekQueueQuery));
//		assertEquals(40, DbmsUtil.executeIntQuery(connection, readQueueQuery));
//		assertEquals(40, DbmsUtil.executeIntQuery(connection, peekQueueQuery));
//		connection.close();
//
//		ReadNextRecordConcurrentlyTester nextRecordTester = null;
//		Semaphore actionFinished = null;
//		Connection workingConn1 = databaseTestEnvironment.getConnection();
//		try (Connection workConn1 = workingConn1) {
//			workConn1.setAutoCommit(false);
//			try (Statement stmt1 = workConn1.createStatement()) {
//				stmt1.setFetchSize(1);
//				log.debug("Read queue using query [" + readQueueQuery + "]");
//				try (ResultSet rs1 = stmt1.executeQuery(readQueueQuery)) {
//					assertTrue(rs1.next());
//					assertEquals(40, rs1.getInt(1));            // find the first record
//					if (testPeekShouldSkipRecordsAlreadyLocked)
//						assertFalse(peek(peekQueueQuery, databaseTestEnvironment), "Peek should skip records already locked, but it found one");    // assert no more records found
//
//					if (databaseTestEnvironment.getDbmsSupport().hasSkipLockedFunctionality()) {
//						Connection workingConn2 = databaseTestEnvironment.getConnection();
//						try (Connection workConn2 = workingConn2) {
//							workConn2.setAutoCommit(false);
//							try (Statement stmt2 = workConn2.createStatement()) {
//								stmt2.setFetchSize(1);
//								try (ResultSet rs2 = stmt2.executeQuery(readQueueQuery)) {
//									if (rs2.next()) { // shouldn't find record in QueueReading mode either
//										fail("readQueueQuery [" + readQueueQuery + "] should not have found record [" + rs2.getString(1) + "] that is already locked");
//									}
//								}
//							}
//							workConn2.commit();
//							workingConn2.close();
//						}
//						workingConn2.close();
//
//						// insert another record
//						executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (41,100)", QueryType.OTHER);
//						if (testPeekFindsRecordsWhenTheyAreAvailable)
//							assertTrue(peek(peekQueueQuery, databaseTestEnvironment), "second record should have been seen by peek query");// assert that record is seen
//
//						Connection workingConn3 = databaseTestEnvironment.getConnection();
//						try (Connection workConn3 = workingConn3) {
//							workConn3.setAutoCommit(false);
//							try (Statement stmt2 = workConn3.createStatement()) {
//								stmt2.setFetchSize(1);
//								try (ResultSet rs2 = stmt2.executeQuery(readQueueQuery)) {
//									assertTrue(rs2.next());
//									assertEquals(41, rs2.getInt(1));    // find the second record
//								}
//							}
//							workConn3.rollback();
//							workingConn3.close();
//						}
//						workingConn3.close();
//					} else {
//						// Next best behaviour for DBMSes that have no skip lock functionality (like MariaDB):
//						// another thread must find the next record when the thread that has the current record moves it out of the way
//
//						executeTranslatedQuery(databaseTestEnvironment, "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (41,100)", QueryType.OTHER);
//
//						actionFinished = new Semaphore();
//						nextRecordTester = new ReadNextRecordConcurrentlyTester((ThrowingSupplier<Connection, SQLException>) connection, readQueueQuery);
//						nextRecordTester.setActionDone(actionFinished);
//						nextRecordTester.start();
//
//						Thread.sleep(500);
//
//						executeTranslatedQuery(databaseTestEnvironment, "UPDATE " + TABLE_NAME + " SET TINT=101  WHERE TKEY=40", QueryType.OTHER);
//
//						workConn1.commit();
//					}
//				}
//			}
//			workConn1.commit();
//			if (nextRecordTester != null) {
//				actionFinished.acquire();
//				assertTrue(nextRecordTester.isPassed(), "Did not read next record");
//			}
//			workingConn1.close();
//		}
//	}

	private class ReadNextRecordConcurrentlyTester extends ConcurrentJdbcActionTester {

		private final String query;
		private @Getter int numRowsUpdated = -1;
		private @Getter boolean passed = false;

		public ReadNextRecordConcurrentlyTester(ThrowingSupplier<Connection, SQLException> connectionSupplier, String query) {
			super(connectionSupplier);
			this.query = query;
		}

		@Override
		public void initAction(Connection conn) throws SQLException {
			conn.setAutoCommit(false);
		}

		@Override
		public void action(Connection conn) throws SQLException {
			try (Statement stmt2 = conn.createStatement()) {
				stmt2.setFetchSize(1);
				try (ResultSet rs2 = stmt2.executeQuery(query)) {
					assertTrue(rs2.next());
					assertEquals(41, rs2.getInt(1));    // find the second record
				}
			}
			passed = true;
		}

		@Override
		public void finalizeAction(Connection conn) throws SQLException {
			conn.rollback();
		}
	}

	@DatabaseTest
	public void testIsBlobType(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException {
		Connection connection = databaseTestEnvironment.getConnection();
		try (connection) {
			try (PreparedStatement stmt = connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM " + TABLE_NAME)) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 9; i++) {
						assertEquals(i == 8, databaseTestEnvironment.getDbmsSupport().isBlobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}
			}
		} finally {
			connection.close();
		}
	}

	@DatabaseTest
	public void testIsClobType(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException {
		Connection connection = databaseTestEnvironment.getConnection();
		try (connection) {
			try (PreparedStatement stmt = connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM " + TABLE_NAME)) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 9; i++) {
						assertEquals(i == 9, databaseTestEnvironment.getDbmsSupport().isClobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}
			}
		} finally {
			connection.close();
		}
	}

	@DatabaseTest
	public void testIsBlobTypeIbisTemp(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		Connection connection = databaseTestEnvironment.getConnection();
		assumeTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(connection, TABLE_NAME));
		try (PreparedStatement stmt = connection.prepareStatement("SELECT TKEY, TVARCHAR, TNUMBER, TDATE, TTIMESTAMP, TBLOB, TCLOB FROM " + TABLE_NAME)) {
			try (ResultSet rs = stmt.executeQuery()) {
				ResultSetMetaData rsmeta = rs.getMetaData();
				for (int i = 1; i <= 7; i++) {
					assertEquals(i == 6, databaseTestEnvironment.getDbmsSupport().isBlobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
				}
			}
		} finally {
			connection.close();
		}
	}

	@DatabaseTest
	public void testIsClobTypeIbisTemp(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		Connection connection = databaseTestEnvironment.getConnection();
		try (connection) {
			assumeTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(connection, TABLE_NAME));
			try (PreparedStatement stmt = connection.prepareStatement("SELECT TKEY, TVARCHAR, TNUMBER, TDATE, TTIMESTAMP, TBLOB, TCLOB FROM " + TABLE_NAME)) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 7; i++) {
						assertEquals(i == 7, databaseTestEnvironment.getDbmsSupport().isClobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}
			}
		} finally {
			connection.close();
		}
	}

	private Properties parseDataSourceInfo(String dsInfo) {
		Properties props = new Properties();
		String[] parts = dsInfo.split("\\] ");
		for (String part : parts) {
			String[] kvPair = part.split(" \\[");
			String key = kvPair[0];
			String value = (kvPair.length == 1) ? "" : kvPair[1];
			if (!props.containsKey(key)) {
				props.put(key, value);
			}
		}
		return props;
	}

	protected PreparedStatement executeTranslatedQuery(DatabaseTestEnvironment databaseTestEnvironment, String query, QueryType queryType) throws JdbcException, SQLException {
		return executeTranslatedQuery(databaseTestEnvironment, query, queryType, false);
	}

	protected PreparedStatement executeTranslatedQuery(DatabaseTestEnvironment databaseTestEnvironment, String query, QueryType queryType, boolean selectForUpdate) throws JdbcException, SQLException {
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");

		Connection connection = databaseTestEnvironment.getConnection();
		if (queryType == QueryType.SELECT) {
			if (!selectForUpdate) {
				return connection.prepareStatement(translatedQuery);
			}
			return connection.prepareStatement(translatedQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}
		JdbcUtil.executeStatement(connection, translatedQuery);
		connection.close();
		return null;
	}
}
