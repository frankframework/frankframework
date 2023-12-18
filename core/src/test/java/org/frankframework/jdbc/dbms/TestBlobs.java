package org.frankframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;

import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Ignore;
import org.junit.Test;

import org.frankframework.jdbc.JdbcQuerySenderBase.QueryType;
import org.frankframework.jdbc.JdbcTestBase;
import org.frankframework.util.StreamUtil;

@Ignore("Tests for Blobs take too much time and memory to test regularly")
public class TestBlobs extends JdbcTestBase {

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

	public void testWriteAndReadBlobUsingSetBinaryStream(int numBlocks, int blockSize) throws Exception {
		String blobContents = getBigString(numBlocks, blockSize);
		String insertQuery = "INSERT INTO "+TEST_TABLE+" (TKEY,TBLOB) VALUES (20,?)";
		String selectQuery = "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=20";
//		String deleteQuery = "DELETE FROM IBISCONFIG WHERE NAME='X'";
//		String insertQuery = "INSERT INTO IBISCONFIG (NAME,VERSION,CONFIG) VALUES ('X','X',?)";
//		String selectQuery = "SELECT CONFIG FROM IBISCONFIG WHERE NAME='X'";
//		JdbcUtil.executeStatement(dbmsSupport, connection, deleteQuery, null);
		try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
			stmt.setBinaryStream(1, new ByteArrayInputStream(blobContents.getBytes("UTF-8")));
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, selectQuery, QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream blobStream = dbmsSupport.getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(blobStream, null, "UTF-8");
				assertEquals(blobContents, actual);
			}
		}
	}
	@Test
	public void testWriteAndReadBlobUsingSetBinaryStream15MB() throws Exception {
		testWriteAndReadBlobUsingSetBinaryStream(1000,15000);
	}

	@Test
	public void testWriteAndReadBlobUsingSetBinaryStream20MB() throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse("MariaDb cannot handle statement packets> 16M", productKey.equals("MariaDB"));
		testWriteAndReadBlobUsingSetBinaryStream(1000,20000);
	}

	public void testWriteAndReadBlobUsingSetBytes(int numBlocks, int blockSize) throws Exception {
		String blobContents = getBigString(numBlocks, blockSize);
		String query = "INSERT INTO "+TEST_TABLE+" (TKEY,TBLOB) VALUES (20,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.execute();
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
	public void testWriteAndReadBlobUsingSetBytes7MB() throws Exception {
		testWriteAndReadBlobUsingSetBytes(1000,7000);
	}

	@Test
	public void testWriteAndReadBlobUsingBytes20MB() throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse("MariaDb cannot handle statement packets> 16M", productKey.equals("MariaDB"));
		testWriteAndReadBlobUsingSetBytes(1000,20000);
	}

	public void testWriteAndReadBlobUsingDbmsSupport(int numOfBlocks, int blockSize) throws Exception {
		String block = getBigString(1,blockSize);
		String query = "INSERT INTO "+TEST_TABLE+" (TKEY,TBLOB) VALUES (20,?)";
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");
		try (PreparedStatement stmt = connection.prepareStatement(translatedQuery)) {
			Object blobInsertHandle = dbmsSupport.getBlobHandle(stmt, 1);
			try (OutputStream blobStream = dbmsSupport.getBlobOutputStream(stmt, 1, blobInsertHandle)) {
				for (int i=0; i<numOfBlocks; i++) {
					blobStream.write(block.getBytes(StandardCharsets.UTF_8));
				}
			}
			dbmsSupport.applyBlobParameter(stmt, 1, blobInsertHandle);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM "+TEST_TABLE+" WHERE TKEY=20", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				try (InputStream blobStream = dbmsSupport.getBlobInputStream(resultSet, 1)) {
					int length = readStream(blobStream);
					assertEquals(blockSize*numOfBlocks, length);
				}
			}
		}

	}

	public void testWriteAndReadClobUsingDbmsSupport(int numOfBlocks, int blockSize) throws Exception {
		String block = getBigString(1,blockSize);
		String insertQuery = "INSERT INTO "+TEST_TABLE+" (TKEY,TCLOB) VALUES (20,?)";
		String selectQuery = "SELECT TCLOB FROM "+TEST_TABLE+" WHERE TKEY=20";
		try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
			Object clobInsertHandle = dbmsSupport.getClobHandle(stmt, 1);
			try (Writer clobWriter = dbmsSupport.getClobWriter(stmt, 1, clobInsertHandle)) {
				for (int i=0; i<numOfBlocks; i++) {
					clobWriter.append(block);
				}
			}
			dbmsSupport.applyClobParameter(stmt, 1, clobInsertHandle);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, selectQuery, QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				try (Reader clobReader = dbmsSupport.getClobReader(resultSet, 1)) {
					int length = readStream(new ReaderInputStream(clobReader));
					assertEquals(blockSize*numOfBlocks, length);
				}
			}
		}

	}

	@Test
	public void testWriteAndReadBlobUsingDbmsSupport15MB() throws Exception {
		testWriteAndReadBlobUsingDbmsSupport(10000,1500);
	}

	@Test
	public void testWriteAndReadBlobUsingDbmsSupport20MB() throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse("MariaDb cannot handle statement packets> 16M", productKey.equals("MariaDB"));
		testWriteAndReadBlobUsingDbmsSupport(10000,2000);
	}

	@Test
	public void testWriteAndReadBlobUsingDbmsSupport100MB() throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse("MariaDb cannot handle statement packets> 16M, PostgreSQL uses ByteArray", productKey.equals("MariaDB") || productKey.equals("PostgreSQL"));
		testWriteAndReadBlobUsingDbmsSupport(10000,10000);
	}

	@Test
	public void testWriteAndReadClobUsingDbmsSupport15MB() throws Exception {
		testWriteAndReadClobUsingDbmsSupport(10000,1500);
	}

	@Test
	public void testWriteAndReadClobUsingDbmsSupport20MB() throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse("MariaDb cannot handle statement packets> 16M", productKey.equals("MariaDB"));
		testWriteAndReadClobUsingDbmsSupport(10000,2000);
	}

	@Test
	public void testWriteAndReadClobUsingDbmsSupport100MB() throws Exception {
		assumeTrue(testBigBlobs);
		assumeFalse("MariaDb cannot handle statement packets> 16M, PostgreSQL uses ByteArray", productKey.equals("MariaDB") || productKey.equals("PostgreSQL"));
		testWriteAndReadClobUsingDbmsSupport(10000,10000);
	}

}
