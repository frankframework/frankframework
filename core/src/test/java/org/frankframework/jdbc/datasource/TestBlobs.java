package org.frankframework.jdbc.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.apache.commons.io.input.ReaderInputStream;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.JdbcQuerySenderBase.QueryType;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.TxManagerTest;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.StreamUtil;

@WithLiquibase(tableName = TestBlobs.TABLE_NAME, file = "Migrator/ChangelogBlobTests.xml")
public class TestBlobs {

	protected static final String TABLE_NAME = "testBlobs_TABLE";
	boolean testBigBlobs = false;

	public static void getBigString(int numBlocks, int blockSize, Consumer<String> consumer) {
		String tenChars="0123456789";
		StringBuilder block = new StringBuilder(blockSize);
		for (int i=0; i<(blockSize+9)/10; i++) {
			block.append(tenChars);
		}
		for (int i=0; i<numBlocks; i++) {
			consumer.accept(block.toString());
		}
	}

	public static String getBigString(int numBlocks, int blockSize) {
		StringBuilder result = new StringBuilder(numBlocks*blockSize);
		getBigString(numBlocks, blockSize, s -> result.append(s));
		return result.toString();
	}

	public static int readStream(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[1000];
		int result = 0;
		int bytesRead=0;
		while (bytesRead>=0) {
			result +=bytesRead;
			bytesRead = inputStream.read(buffer);
		}
		return result;
	}

	public void testWriteAndReadBlobUsingSetBinaryStream(int numBlocks, int blockSize, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = getBigString(numBlocks, blockSize);
		String insertQuery = "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB) VALUES (20,?)";
		String selectQuery = "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=20";
		try (Connection conn = databaseTestEnvironment.getConnection(); PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
			stmt.setBinaryStream(1, new ByteArrayInputStream(blobContents.getBytes("UTF-8")));
			stmt.execute();
		}

		try (Connection conn = databaseTestEnvironment.getConnection(); PreparedStatement stmt = executeTranslatedQuery(conn, selectQuery, QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream blobStream = databaseTestEnvironment.getDbmsSupport().getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(blobStream, null, "UTF-8");
				assertEquals(blobContents, actual);
			}
		}
	}

	@TxManagerTest
	public void testWriteAndReadBlobUsingSetBinaryStream15MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testWriteAndReadBlobUsingSetBinaryStream(1000, 15000, databaseTestEnvironment);
	}

	@TxManagerTest
	public void testWriteAndReadBlobUsingSetBinaryStream20MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse(databaseTestEnvironment.getDataSourceName().equals("MariaDB"), "MariaDb cannot handle statement packets> 16M");
		testWriteAndReadBlobUsingSetBinaryStream(1000, 20000, databaseTestEnvironment);
	}

	public void testWriteAndReadBlobUsingSetBytes(int numBlocks, int blockSize, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String blobContents = getBigString(numBlocks, blockSize);
		String query = "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB) VALUES (20,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		try (Connection conn = databaseTestEnvironment.getConnection(); PreparedStatement stmt = conn.prepareStatement(translatedQuery)) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.execute();
		}

		try (Connection conn = databaseTestEnvironment.getConnection(); PreparedStatement stmt = executeTranslatedQuery(conn, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=20", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream blobStream = databaseTestEnvironment.getDbmsSupport().getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(blobStream, null, "UTF-8");
				assertEquals(blobContents, actual);
			}
		}
	}

	@TxManagerTest
	public void testWriteAndReadBlobUsingSetBytes7MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testWriteAndReadBlobUsingSetBytes(1000, 7000, databaseTestEnvironment);
	}

	@TxManagerTest
	public void testWriteAndReadBlobUsingBytes20MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse(databaseTestEnvironment.getDataSourceName().equals("MariaDB"), "MariaDb cannot handle statement packets> 16M");
		testWriteAndReadBlobUsingSetBytes(1000, 20000, databaseTestEnvironment);
	}

	public void testWriteAndReadBlobUsingDbmsSupport(int numOfBlocks, int blockSize, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String block = getBigString(1,blockSize);
		String query = "INSERT INTO " + TABLE_NAME + " (TKEY,TBLOB) VALUES (20,?)";
		String translatedQuery = databaseTestEnvironment.getDbmsSupport().convertQuery(query, "Oracle");
		try (Connection conn = databaseTestEnvironment.getConnection(); PreparedStatement stmt = conn.prepareStatement(translatedQuery)) {
			Object blobInsertHandle = databaseTestEnvironment.getDbmsSupport().getBlobHandle(stmt, 1);
			try (OutputStream blobStream = databaseTestEnvironment.getDbmsSupport().getBlobOutputStream(stmt, 1, blobInsertHandle)) {
				for (int i=0; i<numOfBlocks; i++) {
					blobStream.write(block.getBytes(StandardCharsets.UTF_8));
				}
			}
			databaseTestEnvironment.getDbmsSupport().applyBlobParameter(stmt, 1, blobInsertHandle);
			stmt.execute();
		}

		try (Connection conn = databaseTestEnvironment.getConnection(); PreparedStatement stmt = executeTranslatedQuery(conn, "SELECT TBLOB FROM " + TABLE_NAME + " WHERE TKEY=20", QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				try (InputStream blobStream = databaseTestEnvironment.getDbmsSupport().getBlobInputStream(resultSet, 1)) {
					int length = readStream(blobStream);
					assertEquals(blockSize*numOfBlocks, length);
				}
			}
		}
	}

	public void testWriteAndReadClobUsingDbmsSupport(int numOfBlocks, int blockSize, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		String block = getBigString(1,blockSize);
		String insertQuery = "INSERT INTO " + TABLE_NAME + " (TKEY,TCLOB) VALUES (20,?)";
		String selectQuery = "SELECT TCLOB FROM " + TABLE_NAME + " WHERE TKEY=20";
		IDbmsSupport dbmsSupport = databaseTestEnvironment.getDbmsSupport();
		try (Connection conn = databaseTestEnvironment.getConnection(); PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
			Object clobInsertHandle = dbmsSupport.getClobHandle(stmt, 1);
			try (Writer clobWriter = dbmsSupport.getClobWriter(stmt, 1, clobInsertHandle)) {
				for (int i=0; i<numOfBlocks; i++) {
					clobWriter.append(block);
				}
			}
			dbmsSupport.applyClobParameter(stmt, 1, clobInsertHandle);
			stmt.execute();
		}

		try (Connection conn = databaseTestEnvironment.getConnection(); PreparedStatement stmt = executeTranslatedQuery(conn, selectQuery, QueryType.SELECT, databaseTestEnvironment)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				try (Reader clobReader = dbmsSupport.getClobReader(resultSet, 1)) {
					int length = readStream(new ReaderInputStream(clobReader));
					assertEquals(blockSize*numOfBlocks, length);
				}
			}
		}

	}

	@TxManagerTest
	public void testWriteAndReadBlobUsingDbmsSupport15MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testWriteAndReadBlobUsingDbmsSupport(10000, 1500, databaseTestEnvironment);
	}

	@TxManagerTest
	public void testWriteAndReadBlobUsingDbmsSupport20MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse(databaseTestEnvironment.getDataSourceName().equals("MariaDB"), "MariaDb cannot handle statement packets> 16M");
		testWriteAndReadBlobUsingDbmsSupport(10000, 2000, databaseTestEnvironment);
	}

	@TxManagerTest
	public void testWriteAndReadBlobUsingDbmsSupport100MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse(databaseTestEnvironment.getDataSourceName().equals("MariaDB") || databaseTestEnvironment.getDataSourceName().equals("PostgreSQL"), "MariaDb cannot handle statement packets> 16M, PostgreSQL uses ByteArray");
		testWriteAndReadBlobUsingDbmsSupport(10000, 10000, databaseTestEnvironment);
	}

	@TxManagerTest
	public void testWriteAndReadClobUsingDbmsSupport15MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testWriteAndReadClobUsingDbmsSupport(10000, 1500, databaseTestEnvironment);
	}

	@TxManagerTest
	public void testWriteAndReadClobUsingDbmsSupport20MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse(databaseTestEnvironment.getDataSourceName().equals("MariaDB"), "MariaDb cannot handle statement packets> 16M");
		testWriteAndReadClobUsingDbmsSupport(10000, 2000, databaseTestEnvironment);
	}

	@TxManagerTest
	public void testWriteAndReadClobUsingDbmsSupport100MB(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse(databaseTestEnvironment.getDataSourceName().equals("MariaDB") || databaseTestEnvironment.getDataSourceName().equals("PostgreSQL"), "MariaDb cannot handle statement packets> 16M, PostgreSQL uses ByteArray");
		testWriteAndReadClobUsingDbmsSupport(10000, 10000, databaseTestEnvironment);
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
		JdbcTestUtil.executeStatement(connection, translatedQuery);
		return null;
	}
}
