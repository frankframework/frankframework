package nl.nn.adapterframework.jdbc.dbms;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

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

import org.hamcrest.core.StringStartsWith;
import org.hamcrest.text.IsEmptyString;
import org.junit.Test;

import lombok.Getter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.dbms.Dbms;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.util.DateFormatUtils;
import nl.nn.adapterframework.util.DbmsUtil;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Semaphore;
import nl.nn.adapterframework.util.StreamUtil;

public class DbmsSupportTest extends JdbcTestBase {
	private final boolean testPeekFindsRecordsWhenTheyAreAvailable = true;

	@Test
	public void testGetDbmsSupport() {
		assertNotNull(dbmsSupport);
	}

	@Test
	public void testNameEqualsDbmsKey() {
		assertEquals(productKey, dbmsSupport.getDbmsName());
		assertEquals(productKey, dbmsSupport.getDbms().getKey());
	}

	@Test
	public void testTableLessSelect() throws JdbcException {
		assertEquals(4, DbmsUtil.executeIntQuery(connection,"SELECT 2+2 "+dbmsSupport.getFromForTablelessSelect()));
	}

	@Test
	public void testTableLessSelectWithIntParam() throws JdbcException {
		assertEquals(4, DbmsUtil.executeIntQuery(connection,"SELECT 1+? "+dbmsSupport.getFromForTablelessSelect(), 3));
	}

//	@Test
//	public void testTableLessSelectWithStringParam() throws JdbcException {
//		assertEquals(3, JdbcUtil.executeIntQuery(connection,"SELECT ''||? "+dbmsSupport.getFromForTablelessSelect(), 3));
//	}

	@Test
	public void testInsertSelect() throws JdbcException {
		JdbcUtil.executeStatement(connection,"INSERT INTO "+TEST_TABLE+" (TKEY, TINT) SELECT 11, 2+2 "+dbmsSupport.getFromForTablelessSelect()+" WHERE 1=1");
	}

	@Test
	public void testIsTablePresent() throws JdbcException {
		assertTrue("Should have found existing table", dbmsSupport.isTablePresent(connection, TEST_TABLE));
		assertFalse(dbmsSupport.isTablePresent(connection, "XXXX"));
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue("Should have found existing table", dbmsSupport.isTablePresent(connection, TEST_TABLE.toLowerCase()));
		assertTrue("Should have found existing table", dbmsSupport.isTablePresent(connection, TEST_TABLE.toUpperCase()));
	}

	@Test
	public void testIsTablePresentInSchema() throws JdbcException {
		String schema = dbmsSupport.getSchema(connection);
		assertTrue("Should have found existing table in schema", dbmsSupport.isTablePresent(connection, schema, TEST_TABLE));
		assertFalse(dbmsSupport.isTablePresent(connection, "XXXX"));
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue("Should have found existing table in schema", dbmsSupport.isTablePresent(connection, schema, TEST_TABLE.toLowerCase()));
		assertTrue("Should have found existing table in schema", dbmsSupport.isTablePresent(connection, schema, TEST_TABLE.toUpperCase()));
	}

	@Test
	public void testIsColumnPresent() throws JdbcException {
		assertTrue("Should have found existing column", dbmsSupport.isColumnPresent(connection, TEST_TABLE, "TINT"));
		assertTrue("Should have found existing column", dbmsSupport.isColumnPresent(connection, TEST_TABLE, "tint"));
		assertFalse(dbmsSupport.isColumnPresent(connection, TEST_TABLE, "XXXX"));
		assertFalse(dbmsSupport.isColumnPresent(connection, "XXXX", "XXXX"));
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue("Should have found existing column", dbmsSupport.isColumnPresent(connection, TEST_TABLE.toLowerCase(), "TINT"));
		assertTrue("Should have found existing column", dbmsSupport.isColumnPresent(connection, TEST_TABLE.toUpperCase(), "TINT"));
	}

	@Test
	public void testIsColumnPresentInSchema() throws JdbcException {
		String schema = dbmsSupport.getSchema(connection);
		assertTrue("Should have found existing column in schema ["+schema+"]", dbmsSupport.isColumnPresent(connection, schema, TEST_TABLE, "TINT"));
		assertFalse(dbmsSupport.isColumnPresent(connection, schema, TEST_TABLE, "XXXX"));
		assertFalse(dbmsSupport.isColumnPresent(connection, schema, "XXXX", "XXXX"));
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue("Should have found existing column in schema ["+schema+"]", dbmsSupport.isColumnPresent(connection, schema, TEST_TABLE.toLowerCase(), "TINT"));
		assertTrue("Should have found existing column in schema ["+schema+"]", dbmsSupport.isColumnPresent(connection, schema, TEST_TABLE.toUpperCase(), "TINT"));
	}

	@Test
	public void testHasIndexOnColumn() throws JdbcException {
		String schema = dbmsSupport.getSchema(connection);
		assertTrue("Should have been index on primary key column", dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "TKEY"));
		assertTrue("Should have been index on primary key column", dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "tkey"));
		assertTrue("Should have been index on column", dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "tINT")); // also check first column of multi column index
		assertFalse("Should not have been index on column", dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "TBOOLEAN"));
		assertFalse("Should not have been index on column", dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE, "tboolean"));
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue("Should have been index on primary key column", dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE.toLowerCase(), "TKEY"));
		assertTrue("Should have been index on primary key column", dbmsSupport.hasIndexOnColumn(connection, schema, TEST_TABLE.toUpperCase(), "TKEY"));
	}

	@Test
	public void testHasIndexOnColumns() throws JdbcException {
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
		assertTrue("Should have been index on columns", dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE, indexedColums));
		assertTrue("Should have been index on columns", dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE, indexedColumsUC));
		assertTrue("Should have been index on columns", dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE, indexedColumsLC));
		assertFalse("Should not have been index on columns", dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE, indexedColumsWrongOrder));
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		assertTrue("Should have been index on columns", dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE.toLowerCase(), indexedColums));
		assertTrue("Should have been index on columns", dbmsSupport.hasIndexOnColumns(connection, schema, TEST_TABLE.toUpperCase(), indexedColums));
	}

	public void testGetTableColumns(String tableName) throws Exception {
		try (ResultSet rs = dbmsSupport.getTableColumns(connection, tableName)) {
			while(rs.next()) {
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

	@Test
	public void testGetTableColumns() throws Exception {
		testGetTableColumns(TEST_TABLE);
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumns(TEST_TABLE.toLowerCase());
		testGetTableColumns(TEST_TABLE.toUpperCase());
	}

	public void testGetTableColumnsInSchema(String tableName) throws Exception {
		String schema = dbmsSupport.getSchema(connection);
		try (ResultSet rs = dbmsSupport.getTableColumns(connection, schema, tableName)) {
			while(rs.next()) {
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

	@Test
	public void testGetTableColumnsInSchema() throws Exception {
		testGetTableColumnsInSchema(TEST_TABLE);
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumnsInSchema(TEST_TABLE.toLowerCase());
		testGetTableColumnsInSchema(TEST_TABLE.toUpperCase());
	}

	public void testGetTableColumnsSpecific(String tableName, String columNamePattern) throws Exception {
		try (ResultSet rs = dbmsSupport.getTableColumns(connection, null, tableName, columNamePattern)) {
			boolean foundTINT = false;
			boolean foundTCHAR = false;
			while(rs.next()) {
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

	@Test
	public void testGetTableColumnsSpecific() throws Exception {
		testGetTableColumnsSpecific(TEST_TABLE, "TINT");
		testGetTableColumnsSpecific(TEST_TABLE, "tint");
		assumeThat(productKey, not(anyOf(equalTo("MariaDB"),equalTo("MySQL")))); // MariaDB and MySQL require exact case for table name parameters
		testGetTableColumnsSpecific(TEST_TABLE.toLowerCase(), "TINT");
		testGetTableColumnsSpecific(TEST_TABLE.toUpperCase(), "TINT");
	}
	@Test
	public void testGetDateTimeLiteral() throws Exception {
		JdbcUtil.executeStatement(connection, "INSERT INTO "+TEST_TABLE+"(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (1,2,3,"+dbmsSupport.getDateAndOffset(dbmsSupport.getDatetimeLiteral(new Date()),4)+","+dbmsSupport.getDatetimeLiteral(new Date())+")");
		Object result = JdbcUtil.executeQuery(dbmsSupport, connection, "SELECT "+dbmsSupport.getTimestampAsDate("TDATETIME")+" FROM "+TEST_TABLE+" WHERE TKEY=1", null, new PipeLineSession());
		System.out.println("result:"+result);
	}

	@Test
	public void testSysDate() throws Exception {
		JdbcUtil.executeStatement(connection, "INSERT INTO "+TEST_TABLE+"(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (2,'xxx',3,"+dbmsSupport.getSysDate()+","+dbmsSupport.getSysDate()+")");
		Object result = JdbcUtil.executeQuery(dbmsSupport, connection, "SELECT "+dbmsSupport.getTimestampAsDate("TDATETIME")+" FROM "+TEST_TABLE+" WHERE TKEY=2", null, new PipeLineSession());
		System.out.println("result:"+result);
	}

	@Test
	public void testNumericAsDouble() throws Exception {
		String number = "1234.5678";
		String query = "INSERT INTO "+TEST_TABLE+"(TKEY, TNUMBER) VALUES (3,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		System.out.println("executing query ["+translatedQuery+"]");
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			stmt.setDouble(1, Double.parseDouble(number));
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER FROM "+TEST_TABLE+" WHERE TKEY=3", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
			}
		}
	}
	@Test
	public void testNumericAsFloat() throws Exception {
		assumeFalse(dbmsSupport.getDbms()== Dbms.POSTGRESQL); // This fails on PostgreSQL, precision of setFloat appears to be too low"
		float number = 1234.5677F;
		String query = "INSERT INTO " + TEST_TABLE + "(TKEY, TNUMBER) VALUES (4,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		System.out.println("executing query ["+translatedQuery+"]");
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			stmt.setFloat(1, number);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER FROM "+TEST_TABLE+" WHERE TKEY=4", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertEquals(number, resultSet.getFloat(1), 0.01);
			}
		}
	}

	@Test
	// test the alias functionality as used in JdbcTableListener.
	// Asserts that columns can be identified with and without alias.
	public void testSelectWithAlias() throws Exception {
		String insertQuery = "INSERT INTO "+TEST_TABLE+"(TKEY, TNUMBER, TVARCHAR) VALUES (5,5,'A')";
		String selectQuery = "SELECT TNUMBER FROM "+TEST_TABLE+" t WHERE TKEY=5 AND t.TVARCHAR='A'";
		System.out.println("executing query ["+insertQuery+"]");
		try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, selectQuery, QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertEquals(5,resultSet.getInt(1));
			}
		}
	}

	@Test
	public void testJdbcSetParameter() throws Exception {
		String number = "1234.5678";
		String datetime = DateFormatUtils.now(DateFormatUtils.GENERIC_DATETIME_FORMATTER);
		String date = DateFormatUtils.now(DateFormatUtils.ISO_DATE_FORMATTER);

		assumeFalse(dbmsSupport.getDbmsName().equals("Oracle")); // This fails on Oracle, cannot set a non-integer number via setString()
		String query = "INSERT INTO " + TEST_TABLE + "(TKEY, TNUMBER, TDATE, TDATETIME) VALUES (5,?,?,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		System.out.println("executing query ["+translatedQuery+"]");
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			JdbcUtil.setParameter(stmt, 1, number, dbmsSupport.isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 2, date, dbmsSupport.isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 3, datetime, dbmsSupport.isParameterTypeMatchRequired());
			//JdbcUtil.setParameter(stmt, 4, bool, dbmsSupport.isParameterTypeMatchRequired());
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER, TDATE, TDATETIME FROM "+TEST_TABLE+" WHERE TKEY=5", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
				assertEquals(date, resultSet.getString(2));
				assertThat(resultSet.getString(3), StringStartsWith.startsWith(datetime));
				//assertEquals(Boolean.parseBoolean(bool), resultSet.getBoolean(4));
			}
		}
	}


	@Test
	public void testWriteAndReadClob() throws Exception {
		String clobContents = "Dit is de content van de clob";
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TCLOB) VALUES (10,EMPTY_CLOB())", QueryType.OTHER);
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM "+TEST_TABLE+" WHERE TKEY=10 FOR UPDATE", QueryType.SELECT, true)) {
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

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM "+TEST_TABLE+" WHERE TKEY=10", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals(clobContents, actual);
			}
		}

	}

	@Test
	public void testReadEmptyClob() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TCLOB) VALUES (11,EMPTY_CLOB())", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM "+TEST_TABLE+" WHERE TKEY=11", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals("", actual);
			}
		}
	}

	@Test
	public void testReadNullClob() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY) VALUES (11)", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM "+TEST_TABLE+" WHERE TKEY=11", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertNull(dbmsSupport.getClobReader(resultSet, 1));
				assertTrue(resultSet.wasNull());
			}
		}
	}


	@Test
	public void testWriteClobInOneStep() throws Exception {
		String clobContents = "Dit is de content van de clob";
		String query = "INSERT INTO " + TEST_TABLE + " (TKEY,TCLOB) VALUES (12,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery);) {
			stmt.setString(1, clobContents);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM "+TEST_TABLE+" WHERE TKEY=12", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals(clobContents, actual);
			}
		}

	}

	@Test
	public void testInsertEmptyClobUsingDbmsSupport() throws Exception {

		JdbcUtil.executeStatement(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TCLOB) VALUES (13,"+dbmsSupport.emptyClobValue()+")");

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM "+TEST_TABLE+" WHERE TKEY=13", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(JdbcUtil.getClobAsString(dbmsSupport, resultSet, 1, false), IsEmptyString.isEmptyOrNullString() );
			}
		}
	}


	@Test
	public void testWriteAndReadBlob() throws Exception {
		String blobContents = "Dit is de content van de blob";
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TBLOB) VALUES (20,EMPTY_BLOB())", QueryType.OTHER);
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=20 FOR UPDATE", QueryType.SELECT, true)) {
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
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=20", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream blobStream = dbmsSupport.getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(blobStream, null, "UTF-8");
				assertEquals(blobContents, actual);
			}
		}

	}


	@Test
	public void testWriteAndReadBlobCompressed() throws Exception {
		String blobContents = "Dit is de content van de blob";
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TBLOB) VALUES (21,EMPTY_BLOB())", QueryType.OTHER);
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=21 FOR UPDATE", QueryType.SELECT, true)) {
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
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=21", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				String actual = JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", true, false, false);
				assertEquals(blobContents, actual);
			}
		}

	}

	@Test
	public void testReadEmptyBlob() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TBLOB) VALUES (22,EMPTY_BLOB())", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=22", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream inputStream = dbmsSupport.getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(inputStream, null, null);
				assertEquals("", actual);
			}
		}
	}

	@Test
	public void testReadNullBlob() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY) VALUES (23)", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=23", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertNull(dbmsSupport.getClobReader(resultSet, 1));
				assertTrue(resultSet.wasNull());
			}
		}
	}

	@Test
	public void testWriteBlobInOneStep() throws Exception {
		String blobContents = "Dit is de content van de blob";
		String query = "INSERT INTO " + TEST_TABLE + " (TKEY,TBLOB) VALUES (24,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery);) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=24", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				String actual = JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", false, false, false);
				assertEquals(blobContents, actual);
			}
		}

	}

	@Test
	public void testInsertEmptyBlobUsingDbmsSupport() throws Exception {

		JdbcUtil.executeStatement(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TBLOB) VALUES (25,"+dbmsSupport.emptyBlobValue()+")");

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=25", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", false, false, false), IsEmptyString.isEmptyOrNullString() );
			}
		}

	}

	@Test
	public void testReadBlobAndCLobUsingJdbcUtilGetValue() throws Exception {
		String blobContents = "Dit is de content van de blob";
		String clobContents = "Dit is de content van de clob";
		String query = "INSERT INTO " + TEST_TABLE + " (TKEY,TBLOB,TCLOB) VALUES (24,?,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery);) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.setString(2, clobContents);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB,TCLOB FROM "+TEST_TABLE+" WHERE TKEY=24", QueryType.SELECT)) {
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



	@Test
	public void testBooleanHandling() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT,TBOOLEAN) VALUES (30,99,"+dbmsSupport.getBooleanValue(false)+")", QueryType.OTHER);
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT,TBOOLEAN) VALUES (31,99,"+dbmsSupport.getBooleanValue(true)+")", QueryType.OTHER);

		assertEquals(30, DbmsUtil.executeIntQuery(connection, "SELECT TKEY FROM "+TEST_TABLE+" WHERE TINT=99 AND TBOOLEAN="+dbmsSupport.getBooleanValue(false)));
		assertEquals(31, DbmsUtil.executeIntQuery(connection, "SELECT TKEY FROM "+TEST_TABLE+" WHERE TINT=99 AND TBOOLEAN="+dbmsSupport.getBooleanValue(true)));

	}

	private boolean peek(String query) throws Exception {
		try (Connection peekConnection=getConnection()) {
			return !JdbcUtil.isQueryResultEmpty(peekConnection, query);
		}
	}

	@Test
	public void testQueueHandling() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (40,100)", QueryType.OTHER);

		String selectQuery="SELECT TKEY FROM "+TEST_TABLE+" WHERE TINT=100";
		assertEquals(40, DbmsUtil.executeIntQuery(connection, selectQuery));

		String readQueueQuery = dbmsSupport.prepareQueryTextForWorkQueueReading(1, selectQuery);
		String peekQueueQuery = dbmsSupport.prepareQueryTextForWorkQueuePeeking(1, selectQuery);

		// test that peek and read find records when they are available
		assertEquals(40, DbmsUtil.executeIntQuery(connection, peekQueueQuery));
		assertEquals(40, DbmsUtil.executeIntQuery(connection, readQueueQuery));
		assertEquals(40, DbmsUtil.executeIntQuery(connection, peekQueueQuery));

		ReadNextRecordConcurrentlyTester nextRecordTester = null;
		Semaphore actionFinished = null;
		try (Connection workConn1=getConnection()) {
			workConn1.setAutoCommit(false);
			try (Statement stmt1= workConn1.createStatement()) {
				stmt1.setFetchSize(1);
				log.debug("Read queue using query ["+readQueueQuery+"]");
				try (ResultSet rs1=stmt1.executeQuery(readQueueQuery)) {
					assertTrue(rs1.next());
					assertEquals(40,rs1.getInt(1));			// find the first record
					if (testPeekShouldSkipRecordsAlreadyLocked) assertFalse("Peek should skip records already locked, but it found one", peek(peekQueueQuery));	// assert no more records found

					if (dbmsSupport.hasSkipLockedFunctionality()) {
						try (Connection workConn2=getConnection()) {
							workConn2.setAutoCommit(false);
							try (Statement stmt2= workConn2.createStatement()) {
								stmt2.setFetchSize(1);
								try (ResultSet rs2=stmt2.executeQuery(readQueueQuery)) {
									if (rs2.next()) { // shouldn't find record in QueueReading mode either
										fail("readQueueQuery ["+readQueueQuery+"] should not have found record ["+rs2.getString(1)+"] that is already locked");
									}
								}
							}
							workConn2.commit();
						}

						// insert another record
						executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (41,100)", QueryType.OTHER);
						if (testPeekFindsRecordsWhenTheyAreAvailable) assertTrue("second record should have been seen by peek query", peek(peekQueueQuery));// assert that record is seen

						try (Connection workConn2=getConnection()) {
							workConn2.setAutoCommit(false);
							try (Statement stmt2= workConn2.createStatement()) {
								stmt2.setFetchSize(1);
								try (ResultSet rs2=stmt2.executeQuery(readQueueQuery)) {
									assertTrue(rs2.next());
									assertEquals(41,rs2.getInt(1));	// find the second record
								}
							}
							workConn2.rollback();
						}
					} else {
						// Next best behaviour for DBMSes that have no skip lock functionality (like MariaDB):
						// another thread must find the next record when the thread that has the current record moves it out of the way

						executeTranslatedQuery(connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (41,100)", QueryType.OTHER);

						actionFinished = new Semaphore();
						nextRecordTester = new ReadNextRecordConcurrentlyTester(this::getConnection, readQueueQuery);
						nextRecordTester.setActionDone(actionFinished);
						nextRecordTester.start();

						Thread.sleep(500);

						executeTranslatedQuery(workConn1, "UPDATE "+TEST_TABLE+" SET TINT=101  WHERE TKEY=40", QueryType.OTHER);

						workConn1.commit();

					}
				}
			}
			workConn1.commit();
			if (nextRecordTester!=null) {
				actionFinished.acquire();
				assertTrue("Did not read next record", nextRecordTester.isPassed());
			}
		}
	}

	private class ReadNextRecordConcurrentlyTester extends ConcurrentJdbcActionTester {

		private String query;
		private @Getter int numRowsUpdated=-1;
		private @Getter boolean passed = false;

		public ReadNextRecordConcurrentlyTester(ThrowingSupplier<Connection,SQLException> connectionSupplier, String query) {
			super(connectionSupplier);
			this.query = query;
		}

		@Override
		public void initAction(Connection conn) throws SQLException {
			conn.setAutoCommit(false);
		}

		@Override
		public void action(Connection conn) throws SQLException {
			try (Statement stmt2= connection.createStatement()) {
				stmt2.setFetchSize(1);
				try (ResultSet rs2=stmt2.executeQuery(query)) {
					assertTrue(rs2.next());
					assertEquals(41,rs2.getInt(1));	// find the second record
				}
			}
			passed = true;
		}

		@Override
		public void finalizeAction(Connection conn) throws SQLException {
			conn.rollback();
		}
	}

	@Test
	public void testIsBlobType() throws SQLException {
		try (Connection connection=getConnection()) {
			try (PreparedStatement stmt= connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM "+TEST_TABLE)) {
				try (ResultSet rs=stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i=1;i<=9;i++) {
						assertEquals("column type name ["+rsmeta.getColumnTypeName(i)+"] precision ["+rsmeta.getPrecision(i)+"] column type ["+rsmeta.getColumnType(i)+"]", i==8, dbmsSupport.isBlobType(rsmeta, i));
					}
				}

			}
		}
	}



	@Test
	public void testIsClobType() throws SQLException {
		try (Connection connection=getConnection()) {
			try (PreparedStatement stmt= connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM "+TEST_TABLE)) {
				try (ResultSet rs=stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i=1;i<=9;i++) {
						assertEquals("column type name ["+rsmeta.getColumnTypeName(i)+"] precision ["+rsmeta.getPrecision(i)+"] column type ["+rsmeta.getColumnType(i)+"]", i==9, dbmsSupport.isClobType(rsmeta, i));
					}
				}

			}
		}
	}

	@Test
	public void testIsBlobTypeIbisTemp() throws Exception {
		try (Connection connection=getConnection()) {
			assumeTrue(dbmsSupport.isTablePresent(connection, "IBISTEMP"));
			try (PreparedStatement stmt= connection.prepareStatement("SELECT TKEY, TVARCHAR, TNUMBER, TDATE, TTIMESTAMP, TBLOB, TCLOB FROM IBISTEMP")) {
				try (ResultSet rs=stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i=1;i<=7;i++) {
						assertEquals("column type name ["+rsmeta.getColumnTypeName(i)+"] precision ["+rsmeta.getPrecision(i)+"] column type ["+rsmeta.getColumnType(i)+"]", i==6, dbmsSupport.isBlobType(rsmeta, i));
					}
				}

			}
		}
	}

	@Test
	public void testIsClobTypeIbisTemp() throws Exception {
		try (Connection connection=getConnection()) {
			assumeTrue(dbmsSupport.isTablePresent(connection, "IBISTEMP"));
			try (PreparedStatement stmt= connection.prepareStatement("SELECT TKEY, TVARCHAR, TNUMBER, TDATE, TTIMESTAMP, TBLOB, TCLOB FROM IBISTEMP")) {
				try (ResultSet rs=stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i=1;i<=7;i++) {
						assertEquals("column type name ["+rsmeta.getColumnTypeName(i)+"] precision ["+rsmeta.getPrecision(i)+"] column type ["+rsmeta.getColumnType(i)+"]", i==7, dbmsSupport.isClobType(rsmeta, i));
					}
				}

			}
		}
	}

}
