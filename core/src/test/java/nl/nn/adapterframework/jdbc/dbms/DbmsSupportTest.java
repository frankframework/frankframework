package nl.nn.adapterframework.jdbc.dbms;

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

import org.hamcrest.core.StringStartsWith;
import org.hamcrest.text.IsEmptyString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import lombok.Getter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.dbms.Dbms;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.dbms.TransactionalDbmsSupportAwareDataSourceProxy;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.junit.DatabaseTest;
import nl.nn.adapterframework.testutil.junit.DatabaseTestEnvironment;
import nl.nn.adapterframework.testutil.junit.WithLiquibase;
import nl.nn.adapterframework.util.DateFormatUtils;
import nl.nn.adapterframework.util.DbmsUtil;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Semaphore;
import nl.nn.adapterframework.util.StreamUtil;

@WithLiquibase(tableName = DbmsSupportTest.tableName, file = "Migrator/JdbcTestBaseQuery.xml")
public class DbmsSupportTest {
	private final boolean testPeekFindsRecordsWhenTheyAreAvailable = true;
	protected static final String tableName = "DST_TABLE";
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
		dataSource = transactionManagerType.getDataSource(dataSourceName);

		String dsInfo; //We can assume a connection has already been made by the URLDataSourceFactory to validate the DataSource/connectivity
		if (dataSource instanceof TransactionalDbmsSupportAwareDataSourceProxy) {
			dsInfo = ((TransactionalDbmsSupportAwareDataSourceProxy) dataSource).getTargetDataSource().toString();
		} else {
			dsInfo = dataSource.toString();
		}
		dataSourceInfo = parseDataSourceInfo(dsInfo);
	}

	@AfterEach
	public void tearDown(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		databaseTestEnvironment.close();
	}

	@DatabaseTest
	public void testGetDbmsSupport(DatabaseTestEnvironment databaseTestEnvironment) {
		assertNotNull(databaseTestEnvironment.getDbmsSupport());
	}

	@DatabaseTest
	public void testNameEqualsDbmsKey(DatabaseTestEnvironment databaseTestEnvironment) {
		assertEquals(dataSourceName, databaseTestEnvironment.getDbmsSupport().getDbmsName());
		assertEquals(dataSourceName, databaseTestEnvironment.getDbmsSupport().getDbms().getKey());
	}

	@DatabaseTest
	public void testTableLessSelect(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		assertEquals(4, DbmsUtil.executeIntQuery(databaseTestEnvironment.getConnection(), "SELECT 2+2 " + databaseTestEnvironment.getDbmsSupport().getFromForTablelessSelect()));
	}

	@DatabaseTest
	public void testTableLessSelectWithIntParam(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		assertEquals(4, DbmsUtil.executeIntQuery(databaseTestEnvironment.getConnection(), "SELECT 1+? " + databaseTestEnvironment.getDbmsSupport().getFromForTablelessSelect(), 3));
	}

//	@Test
//	public void testTableLessSelectWithStringParam() throws JdbcException {
//		assertEquals(3, JdbcUtil.executeIntQuery(connection,"SELECT ''||? "+dbmsSupport.getFromForTablelessSelect(), 3));
//	}

	@DatabaseTest
	public void testInsertSelect(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		JdbcUtil.executeStatement(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY, TINT) SELECT 11, 2+2 " + databaseTestEnvironment.getDbmsSupport().getFromForTablelessSelect() + " WHERE 1=1");
	}

	@DatabaseTest
	public void testIsTablePresent(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		assertTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), tableName), "Should have found existing table");
		assertFalse(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), "XXXX"));
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), tableName.toLowerCase()), "Should have found existing table");
		assertTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), tableName.toUpperCase()), "Should have found existing table");
	}

	@DatabaseTest
	public void testIsTablePresentInSchema(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		String schema = databaseTestEnvironment.getDbmsSupport().getSchema(databaseTestEnvironment.getConnection());
		assertTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), schema, tableName), "Should have found existing table in schema");
		assertFalse(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), "XXXX"));
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), schema, tableName.toLowerCase()), "Should have found existing table in schema");
		assertTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), schema, tableName.toUpperCase()), "Should have found existing table in schema");
	}

	@DatabaseTest
	public void testIsColumnPresent(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		assertTrue(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), tableName, "TINT"), "Should have found existing column");
		assertTrue(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), tableName, "tint"), "Should have found existing column");
		assertFalse(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), tableName, "XXXX"));
		assertFalse(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), "XXXX", "XXXX"));
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), tableName.toLowerCase(), "TINT"), "Should have found existing column");
		assertTrue(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), tableName.toUpperCase(), "TINT"), "Should have found existing column");
	}

	@DatabaseTest
	public void testIsColumnPresentInSchema(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		String schema = databaseTestEnvironment.getDbmsSupport().getSchema(databaseTestEnvironment.getConnection());
		assertTrue(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), schema, tableName, "TINT"), "Should have found existing column in schema [" + schema + "]");
		assertFalse(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), schema, tableName, "XXXX"));
		assertFalse(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), schema, "XXXX", "XXXX"));
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), schema, tableName.toLowerCase(), "TINT"), "Should have found existing column in schema [" + schema + "]");
		assertTrue(databaseTestEnvironment.getDbmsSupport().isColumnPresent(databaseTestEnvironment.getConnection(), schema, tableName.toUpperCase(), "TINT"), "Should have found existing column in schema [" + schema + "]");
	}

	@DatabaseTest
	public void testHasIndexOnColumn(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		String schema = databaseTestEnvironment.getDbmsSupport().getSchema(databaseTestEnvironment.getConnection());
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumn(databaseTestEnvironment.getConnection(), schema, tableName, "TKEY"), "Should have been index on primary key column");
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumn(databaseTestEnvironment.getConnection(), schema, tableName, "tkey"), "Should have been index on primary key column");
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumn(databaseTestEnvironment.getConnection(), schema, tableName, "tINT"), "Should have been index on column"); // also check first column of multi column index
		assertFalse(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumn(databaseTestEnvironment.getConnection(), schema, tableName, "TBOOLEAN"), "Should not have been index on column");
		assertFalse(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumn(databaseTestEnvironment.getConnection(), schema, tableName, "tboolean"), "Should not have been index on column");
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumn(databaseTestEnvironment.getConnection(), schema, tableName.toLowerCase(), "TKEY"), "Should have been index on primary key column");
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumn(databaseTestEnvironment.getConnection(), schema, tableName.toUpperCase(), "TKEY"), "Should have been index on primary key column");
	}

	@DatabaseTest
	public void testHasIndexOnColumns(DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		String schema = databaseTestEnvironment.getDbmsSupport().getSchema(databaseTestEnvironment.getConnection());
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
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumns(databaseTestEnvironment.getConnection(), schema, tableName, indexedColums), "Should have been index on columns");
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumns(databaseTestEnvironment.getConnection(), schema, tableName, indexedColumsUC), "Should have been index on columns");
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumns(databaseTestEnvironment.getConnection(), schema, tableName, indexedColumsLC), "Should have been index on columns");
		assertFalse(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumns(databaseTestEnvironment.getConnection(), schema, tableName, indexedColumsWrongOrder), "Should not have been index on columns");
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumns(databaseTestEnvironment.getConnection(), schema, tableName.toLowerCase(), indexedColums), "Should have been index on columns");
		assertTrue(databaseTestEnvironment.getDbmsSupport().hasIndexOnColumns(databaseTestEnvironment.getConnection(), schema, tableName.toUpperCase(), indexedColums), "Should have been index on columns");
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
		testGetTableColumns(tableName, databaseTestEnvironment);
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumns(tableName.toLowerCase(), databaseTestEnvironment);
		testGetTableColumns(tableName.toUpperCase(), databaseTestEnvironment);
	}

	public void testGetTableColumnsInSchema(String tableName, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String schema = databaseTestEnvironment.getDbmsSupport().getSchema(databaseTestEnvironment.getConnection());
		try (ResultSet rs = databaseTestEnvironment.getDbmsSupport().getTableColumns(databaseTestEnvironment.getConnection(), schema, tableName)) {
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
	public void testGetTableColumnsInSchema(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetTableColumnsInSchema(tableName, databaseTestEnvironment);
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumnsInSchema(tableName.toLowerCase(), databaseTestEnvironment);
		testGetTableColumnsInSchema(tableName.toUpperCase(), databaseTestEnvironment);
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
		testGetTableColumnsSpecific(tableName, "TINT", databaseTestEnvironment);
		testGetTableColumnsSpecific(tableName, "tint", databaseTestEnvironment);
		assumeThat(dataSourceName, not(anyOf(equalTo("MariaDB"), equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumnsSpecific(tableName.toLowerCase(), "TINT", databaseTestEnvironment);
		testGetTableColumnsSpecific(tableName.toUpperCase(), "TINT", databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetDateTimeLiteral(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		JdbcUtil.executeStatement(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + "(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (1,2,3," + databaseTestEnvironment.getDbmsSupport().getDateAndOffset(databaseTestEnvironment.getDbmsSupport().getDatetimeLiteral(new Date()), 4) + "," + databaseTestEnvironment.getDbmsSupport().getDatetimeLiteral(new Date()) + ")");
		Object result = JdbcUtil.executeQuery(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "SELECT " + databaseTestEnvironment.getDbmsSupport().getTimestampAsDate("TDATETIME") + " FROM " + tableName + " WHERE TKEY=1", null, new PipeLineSession());
		System.out.println("result:" + result);
	}

	@DatabaseTest
	public void testSysDate(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		JdbcUtil.executeStatement(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + "(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (2,'xxx',3," + databaseTestEnvironment.getDbmsSupport().getSysDate() + "," + databaseTestEnvironment.getDbmsSupport().getSysDate() + ")");
		Object result = JdbcUtil.executeQuery(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "SELECT " + databaseTestEnvironment.getDbmsSupport().getTimestampAsDate("TDATETIME") + " FROM " + tableName + " WHERE TKEY=2", null, new PipeLineSession());
		System.out.println("result:" + result);
	}

	@DatabaseTest
	public void testNumericAsDouble(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String number = "1234.5678";
		String query = "INSERT INTO " + tableName + "(TKEY, TNUMBER) VALUES (3,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		System.out.println("executing query [" + translatedQuery + "]");
		try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement(translatedQuery)) {
			stmt.setDouble(1, Double.parseDouble(number));
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TNUMBER FROM " + tableName + " WHERE TKEY=3", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
			}
		}
	}

	@DatabaseTest
	public void testNumericAsFloat(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeFalse(databaseTestEnvironment.getDbmsSupport().getDbms() == Dbms.POSTGRESQL); // This fails on PostgreSQL, precision of setFloat appears to be too low"
		float number = 1234.5677F;
		String query = "INSERT INTO " + tableName + "(TKEY, TNUMBER) VALUES (4,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		System.out.println("executing query [" + translatedQuery + "]");
		try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement(translatedQuery)) {
			stmt.setFloat(1, number);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TNUMBER FROM " + tableName + " WHERE TKEY=4", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertEquals(number, resultSet.getFloat(1), 0.01);
			}
		}
	}

	@DatabaseTest
	// test the alias functionality as used in JdbcTableListener.
	// Asserts that columns can be identified with and without alias.
	public void testSelectWithAlias(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String insertQuery = "INSERT INTO " + tableName + "(TKEY, TNUMBER, TVARCHAR) VALUES (5,5,'A')";
		String selectQuery = "SELECT TNUMBER FROM " + tableName + " t WHERE TKEY=5 AND t.TVARCHAR='A'";
		System.out.println("executing query [" + insertQuery + "]");
		try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement(insertQuery)) {
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), selectQuery, QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertEquals(5, resultSet.getInt(1));
			}
		}
	}

	@DatabaseTest
	public void testJdbcSetParameter(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String number = "1234.5678";
		String datetime = DateFormatUtils.now(DateFormatUtils.GENERIC_DATETIME_FORMATTER);
		String date = DateFormatUtils.now(DateFormatUtils.ISO_DATE_FORMATTER);

		assumeFalse(databaseTestEnvironment.getDbmsSupport().getDbmsName().equals("Oracle")); // This fails on Oracle, cannot set a non-integer number via setString()
		String query = "INSERT INTO " + tableName + "(TKEY, TNUMBER, TDATE, TDATETIME) VALUES (5,?,?,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		System.out.println("executing query [" + translatedQuery + "]");
		try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement(translatedQuery)) {
			JdbcUtil.setParameter(stmt, 1, number, databaseTestEnvironment.getDbmsSupport().isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 2, date, databaseTestEnvironment.getDbmsSupport().isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 3, datetime, databaseTestEnvironment.getDbmsSupport().isParameterTypeMatchRequired());
			//JdbcUtil.setParameter(stmt, 4, bool, dbmsSupport.isParameterTypeMatchRequired());
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TNUMBER, TDATE, TDATETIME FROM " + tableName + " WHERE TKEY=5", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
				assertEquals(date, resultSet.getString(2));
				assertThat(resultSet.getString(3), StringStartsWith.startsWith(datetime));
				//assertEquals(Boolean.parseBoolean(bool), resultSet.getBoolean(4));
			}
		}
	}


	@DatabaseTest
	public void testWriteAndReadClob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String clobContents = "Dit is de content van de clob";
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TCLOB) VALUES (10,EMPTY_CLOB())", QueryType.OTHER, databaseTestEnvironment);
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TCLOB FROM " + tableName + " WHERE TKEY=10 FOR UPDATE", QueryType.SELECT, true, databaseTestEnvironment)) {
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

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TCLOB FROM " + tableName + " WHERE TKEY=10", QueryType.SELECT, databaseTestEnvironment)) {
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
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TCLOB) VALUES (11,EMPTY_CLOB())", QueryType.OTHER, databaseTestEnvironment);

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TCLOB FROM " + tableName + " WHERE TKEY=11", QueryType.SELECT, databaseTestEnvironment)) {
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
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY) VALUES (11)", QueryType.OTHER, databaseTestEnvironment);

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TCLOB FROM " + tableName + " WHERE TKEY=11", QueryType.SELECT, databaseTestEnvironment)) {
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
		String query = "INSERT INTO " + tableName + " (TKEY,TCLOB) VALUES (12,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement(translatedQuery);) {
			stmt.setString(1, clobContents);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TCLOB FROM " + tableName + " WHERE TKEY=12", QueryType.SELECT, databaseTestEnvironment)) {
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

		JdbcUtil.executeStatement(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TCLOB) VALUES (13," + databaseTestEnvironment.getDbmsSupport().emptyClobValue() + ")");

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TCLOB FROM " + tableName + " WHERE TKEY=13", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(JdbcUtil.getClobAsString(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, false), IsEmptyString.isEmptyOrNullString());
			}
		}
	}


	@DatabaseTest
	public void testWriteAndReadBlob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = "Dit is de content van de blob";
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TBLOB) VALUES (20,EMPTY_BLOB())", QueryType.OTHER, databaseTestEnvironment);
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB FROM " + tableName + " WHERE TKEY=20 FOR UPDATE", QueryType.SELECT, true, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Object blobHandle = databaseTestEnvironment.getDbmsSupport().getBlobHandle(resultSet, 1);
				try (OutputStream out = databaseTestEnvironment.getDbmsSupport().getBlobOutputStream(resultSet, 1, blobHandle)) {
					out.write(blobContents.getBytes("UTF-8"));
				}
				databaseTestEnvironment.getDbmsSupport().updateBlob(resultSet, 1, blobHandle);
				resultSet.updateRow();
			}
		}
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB FROM " + tableName + " WHERE TKEY=20", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream blobStream = databaseTestEnvironment.getDbmsSupport().getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(blobStream, null, "UTF-8");
				assertEquals(blobContents, actual);
			}
		}

	}


	@DatabaseTest
	public void testWriteAndReadBlobCompressed(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = "Dit is de content van de blob";
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TBLOB) VALUES (21,EMPTY_BLOB())", QueryType.OTHER, databaseTestEnvironment);
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB FROM " + tableName + " WHERE TKEY=21 FOR UPDATE", QueryType.SELECT, true, databaseTestEnvironment)) {
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
		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB FROM " + tableName + " WHERE TKEY=21", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				String actual = JdbcUtil.getBlobAsString(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, "UTF-8", true, false, false);
				assertEquals(blobContents, actual);
			}
		}

	}

	@DatabaseTest
	public void testReadEmptyBlob(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TBLOB) VALUES (22,EMPTY_BLOB())", QueryType.OTHER, databaseTestEnvironment);

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB FROM " + tableName + " WHERE TKEY=22", QueryType.SELECT, databaseTestEnvironment)) {
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
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY) VALUES (23)", QueryType.OTHER, databaseTestEnvironment);

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB FROM " + tableName + " WHERE TKEY=23", QueryType.SELECT, databaseTestEnvironment)) {
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
		String query = "INSERT INTO " + tableName + " (TKEY,TBLOB) VALUES (24,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement(translatedQuery);) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB FROM " + tableName + " WHERE TKEY=24", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				String actual = JdbcUtil.getBlobAsString(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, "UTF-8", false, false, false);
				assertEquals(blobContents, actual);
			}
		}

	}

	@DatabaseTest
	public void testInsertEmptyBlobUsingDbmsSupport(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		JdbcUtil.executeStatement(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TBLOB) VALUES (25," + databaseTestEnvironment.getDbmsSupport().emptyBlobValue() + ")");

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB FROM " + tableName + " WHERE TKEY=25", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(JdbcUtil.getBlobAsString(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, "UTF-8", false, false, false), IsEmptyString.isEmptyOrNullString());
			}
		}
	}

	@DatabaseTest
	public void testReadBlobAndCLobUsingJdbcUtilGetValue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = "Dit is de content van de blob";
		String clobContents = "Dit is de content van de clob";
		String query = "INSERT INTO " + tableName + " (TKEY,TBLOB,TCLOB) VALUES (24,?,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement(translatedQuery);) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.setString(2, clobContents);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(databaseTestEnvironment.getConnection(), "SELECT TBLOB,TCLOB FROM " + tableName + " WHERE TKEY=24", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				ResultSetMetaData rsmeta = resultSet.getMetaData();
				resultSet.next();
				String actual1 = JdbcUtil.getValue(databaseTestEnvironment.getDbmsSupport(), resultSet, 1, rsmeta, "UTF-8", false, null, true, false, false);
				String actual2 = JdbcUtil.getValue(databaseTestEnvironment.getDbmsSupport(), resultSet, 2, rsmeta, "UTF-8", false, null, true, false, false);
				assertEquals(blobContents, actual1);
				assertEquals(clobContents, actual2);
			}
		}

	}

	@DatabaseTest
	public void testBooleanHandling(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TINT,TBOOLEAN) VALUES (30,99," + databaseTestEnvironment.getDbmsSupport().getBooleanValue(false) + ")", QueryType.OTHER, databaseTestEnvironment);
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TINT,TBOOLEAN) VALUES (31,99," + databaseTestEnvironment.getDbmsSupport().getBooleanValue(true) + ")", QueryType.OTHER, databaseTestEnvironment);

		assertEquals(30, DbmsUtil.executeIntQuery(databaseTestEnvironment.getConnection(), "SELECT TKEY FROM " + tableName + " WHERE TINT=99 AND TBOOLEAN=" + databaseTestEnvironment.getDbmsSupport().getBooleanValue(false)));
		assertEquals(31, DbmsUtil.executeIntQuery(databaseTestEnvironment.getConnection(), "SELECT TKEY FROM " + tableName + " WHERE TINT=99 AND TBOOLEAN=" + databaseTestEnvironment.getDbmsSupport().getBooleanValue(true)));

	}

	private boolean peek(String query, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		try (Connection peekConnection = databaseTestEnvironment.getConnection()) {
			return !JdbcUtil.isQueryResultEmpty(peekConnection, query);
		}
	}

	@DatabaseTest
	public void testQueueHandling(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TINT) VALUES (40,100)", QueryType.OTHER, databaseTestEnvironment);

		String selectQuery = "SELECT TKEY FROM " + tableName + " WHERE TINT=100";
		assertEquals(40, DbmsUtil.executeIntQuery(databaseTestEnvironment.getConnection(), selectQuery));

		String readQueueQuery = databaseTestEnvironment.getDbmsSupport().prepareQueryTextForWorkQueueReading(1, selectQuery);
		String peekQueueQuery = databaseTestEnvironment.getDbmsSupport().prepareQueryTextForWorkQueuePeeking(1, selectQuery);

		// test that peek and read find records when they are available
		assertEquals(40, DbmsUtil.executeIntQuery(databaseTestEnvironment.getConnection(), peekQueueQuery));
		assertEquals(40, DbmsUtil.executeIntQuery(databaseTestEnvironment.getConnection(), readQueueQuery));
		assertEquals(40, DbmsUtil.executeIntQuery(databaseTestEnvironment.getConnection(), peekQueueQuery));

		ReadNextRecordConcurrentlyTester nextRecordTester = null;
		Semaphore actionFinished = null;
		try (Connection workConn1 = databaseTestEnvironment.getConnection()) {
			workConn1.setAutoCommit(false);
			try (Statement stmt1 = workConn1.createStatement()) {
				stmt1.setFetchSize(1);
				try (ResultSet rs1 = stmt1.executeQuery(readQueueQuery)) {
					assertTrue(rs1.next());
					assertEquals(40, rs1.getInt(1));            // find the first record
					if (testPeekShouldSkipRecordsAlreadyLocked)
						assertFalse(peek(peekQueueQuery, databaseTestEnvironment), "Peek should skip records already locked, but it found one");    // assert no more records found

					if (databaseTestEnvironment.getDbmsSupport().hasSkipLockedFunctionality()) {
						try (Connection workConn2 = databaseTestEnvironment.getConnection()) {
							workConn2.setAutoCommit(false);
							try (Statement stmt2 = workConn2.createStatement()) {
								stmt2.setFetchSize(1);
								try (ResultSet rs2 = stmt2.executeQuery(readQueueQuery)) {
									if (rs2.next()) { // shouldn't find record in QueueReading mode either
										fail("readQueueQuery [" + readQueueQuery + "] should not have found record [" + rs2.getString(1) + "] that is already locked");
									}
								}
							}
							workConn2.commit();
						}

						// insert another record
						executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TINT) VALUES (41,100)", QueryType.OTHER, databaseTestEnvironment);
						if (testPeekFindsRecordsWhenTheyAreAvailable)
							assertTrue(peek(peekQueueQuery, databaseTestEnvironment), "second record should have been seen by peek query");// assert that record is seen

						try (Connection workConn2 = databaseTestEnvironment.getConnection()) {
							workConn2.setAutoCommit(false);
							try (Statement stmt2 = workConn2.createStatement()) {
								stmt2.setFetchSize(1);
								try (ResultSet rs2 = stmt2.executeQuery(readQueueQuery)) {
									assertTrue(rs2.next());
									assertEquals(41, rs2.getInt(1));    // find the second record
								}
							}
							workConn2.rollback();
						}
					} else {
						// Next best behaviour for DBMSes that have no skip lock functionality (like MariaDB):
						// another thread must find the next record when the thread that has the current record moves it out of the way

						executeTranslatedQuery(databaseTestEnvironment.getConnection(), "INSERT INTO " + tableName + " (TKEY,TINT) VALUES (41,100)", QueryType.OTHER, databaseTestEnvironment);

						actionFinished = new Semaphore();
						nextRecordTester = new ReadNextRecordConcurrentlyTester((ThrowingSupplier<Connection, SQLException>) databaseTestEnvironment.getConnection(), readQueueQuery);
						nextRecordTester.setActionDone(actionFinished);
						nextRecordTester.start();

						Thread.sleep(500);

						executeTranslatedQuery(workConn1, "UPDATE " + tableName + " SET TINT=101  WHERE TKEY=40", QueryType.OTHER, databaseTestEnvironment);

						workConn1.commit();

					}
				}
			}
			workConn1.commit();
			if (nextRecordTester != null) {
				actionFinished.acquire();
				assertTrue(nextRecordTester.isPassed(), "Did not read next record");
			}
		}
	}

	private class ReadNextRecordConcurrentlyTester extends ConcurrentJdbcActionTester {

		private String query;
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
		try (Connection connection = databaseTestEnvironment.getConnection()) {
			try (PreparedStatement stmt = connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM " + tableName)) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 9; i++) {
						assertEquals(i == 8, databaseTestEnvironment.getDbmsSupport().isBlobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}

			}
		}
	}

	@DatabaseTest
	public void testIsClobType(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException {
		try (Connection connection = databaseTestEnvironment.getConnection()) {
			try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM " + tableName)) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 9; i++) {
						assertEquals(i == 9, databaseTestEnvironment.getDbmsSupport().isClobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}

			}
		}
	}

	@DatabaseTest
	public void testIsBlobTypeIbisTemp(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		try (Connection connection = databaseTestEnvironment.getConnection()) {
			assumeTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), "IBISTEMP"));
			try (PreparedStatement stmt = databaseTestEnvironment.getConnection().prepareStatement("SELECT TKEY, TVARCHAR, TNUMBER, TDATE, TTIMESTAMP, TBLOB, TCLOB FROM IBISTEMP")) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 7; i++) {
						assertEquals(i == 6, databaseTestEnvironment.getDbmsSupport().isBlobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}

			}
		}
	}

	@DatabaseTest
	public void testIsClobTypeIbisTemp(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		try (Connection connection = databaseTestEnvironment.getConnection()) {
			assumeTrue(databaseTestEnvironment.getDbmsSupport().isTablePresent(connection, "IBISTEMP"));
			try (PreparedStatement stmt = connection.prepareStatement("SELECT TKEY, TVARCHAR, TNUMBER, TDATE, TTIMESTAMP, TBLOB, TCLOB FROM IBISTEMP")) {
				try (ResultSet rs = stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i = 1; i <= 7; i++) {
						assertEquals(i == 7, databaseTestEnvironment.getDbmsSupport().isClobType(rsmeta, i), "column type name [" + rsmeta.getColumnTypeName(i) + "] precision [" + rsmeta.getPrecision(i) + "] column type [" + rsmeta.getColumnType(i) + "]");
					}
				}
			}
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

	protected PreparedStatement executeTranslatedQuery(Connection connection, String query, QueryType queryType, DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		return executeTranslatedQuery(connection, query, queryType, false, databaseTestEnvironment);
	}

	protected PreparedStatement executeTranslatedQuery(Connection connection, String query, QueryType queryType, boolean selectForUpdate, DatabaseTestEnvironment databaseTestEnvironment) throws JdbcException, SQLException {
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");

		if (queryType == QueryType.SELECT) {
			if (!selectForUpdate) {
				return connection.prepareStatement(translatedQuery);
			}
			return connection.prepareStatement(translatedQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}
		JdbcUtil.executeStatement(connection, translatedQuery);
		return null;
	}
}
