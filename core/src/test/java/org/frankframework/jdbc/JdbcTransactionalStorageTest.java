/*
   Copyright 2021-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;

import org.junit.jupiter.api.BeforeEach;

import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;

@WithLiquibase(tableName = JdbcTransactionalStorageTest.tableName)
public class JdbcTransactionalStorageTest {

	static final String tableName = "JDBCTRANSACTIONALSTORAGETEST";

	private JdbcTransactionalStorage<String> storage;
	private DatabaseTestEnvironment env;

	private final String messageField = "MESSAGE";
	private final String keyField = "MESSAGEKEY";

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		this.env = env;
		storage = env.createBean(JdbcTransactionalStorage.class);
		storage.setTableName(tableName);
		storage.setMessageField(messageField);
		storage.setKeyField(keyField);
		storage.setCheckTable(false);
		storage.setSequenceName("SEQ_" + tableName);
		storage.setSlotId("test");
	}

	@DatabaseTest
	public void testQueryTextAndBrowseMessage() throws Exception {
		testQueryTextAndBrowseMessageHelper(true);
	}

	@DatabaseTest
	public void testQueryTextAndBrowseMessageNotCompressed() throws Exception {
		testQueryTextAndBrowseMessageHelper(false);
	}

	public void testQueryTextAndBrowseMessageHelper(boolean blobsCompressed) throws Exception {
		storage.setBlobsCompressed(blobsCompressed);
		storage.configure();
		// check created query
		String expected = "SELECT " + keyField + "," + messageField + " FROM " + tableName + " WHERE " + keyField + "=?";
		String query = storage.selectDataQuery;
		assertEquals(expected, query);

		String message = createMessage();
		String storageKey = insertARecord(blobsCompressed, message, 'E');

		RawMessageWrapper<String> rawMessageWrapper = storage.browseMessage(storageKey);
		String data = rawMessageWrapper.getRawMessage();
		assertEquals(storageKey, rawMessageWrapper.getContext().get(PipeLineSession.STORAGE_ID_KEY));
		assertEquals(message, data);
	}

	@DatabaseTest
	public void testRetrieveObject() throws Exception {
		testRetrieveObjectHelper(true);
	}

	@DatabaseTest
	public void testRetrieveObjectNotCompressed() throws Exception {
		testRetrieveObjectHelper(false);
	}

	public void testRetrieveObjectHelper(boolean blobsCompressed) throws Exception {
		storage.setBlobsCompressed(blobsCompressed);
		storage.configure();

		String message = createMessage();

		// insert a record
		try (Connection connection = env.getConnection(); PreparedStatement stmt = prepareStatement(connection, 'E')) {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStream out = blobsCompressed ? new DeflaterOutputStream(baos) : baos;
			try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
				oos.writeObject(message);
			}
			stmt.setBytes(1, baos.toByteArray());
			stmt.execute();

			String selectQuery = "SELECT * FROM " + tableName;
			try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					String result = storage.retrieveObject("dummy", rs, 9).getRawMessage();
					assertEquals(message, result);
				} else {
					fail("The query [" + selectQuery + "] returned empty result set expected 1");
				}
			}
		}
	}

	@DatabaseTest
	public void testBrowseMessage() throws Exception {
		boolean blobsCompressed = true;
		storage.setBlobsCompressed(blobsCompressed);
		storage.configure();

		String message = createMessage();
		String storageKey = insertARecord(blobsCompressed, message, 'E');

		RawMessageWrapper<?> ro = storage.browseMessage(storageKey);
		Object o = ro.getRawMessage();
		assertEquals(storageKey, ro.getId());
		assertNotNull(o);
		assertEquals(message, o);

	}


	private String insertARecord(boolean blobsCompressed, String message, char type) throws SQLException, IOException {
		try (Connection connection = env.getConnection()) {
			try (PreparedStatement stmt = prepareStatement(connection, type)) {

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				OutputStream out = blobsCompressed ? new DeflaterOutputStream(baos) : baos;
				try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
					oos.writeObject(message);
				}
				stmt.setBytes(1, baos.toByteArray());
				stmt.execute();

				try (ResultSet rs = stmt.getGeneratedKeys()) {
					if (rs.next()) {
						// check inserted data being correctly retrieved
						return rs.getString(1);
					}
					fail("The query [" + storage.selectDataQuery + "] returned empty result set expected 1");
					return null;
				}
			}
		}
	}

	private PreparedStatement prepareStatement(Connection connection, char type) throws SQLException {
		IDbmsSupport dbmsSupport = env.getDbmsSupport();
		String query = "INSERT INTO " + tableName + " (" +
				(dbmsSupport.autoIncrementKeyMustBeInserted() ? storage.getKeyField() + "," : "")
				+ storage.getTypeField() + ","
				+ storage.getSlotIdField() + ","
				+ storage.getHostField() + ","
				+ storage.getIdField() + ","
				+ storage.getCorrelationIdField() + ","
				+ storage.getDateField() + ","
				+ storage.getCommentField() + ","
				+ storage.getMessageField() + ","
				+ storage.getExpiryDateField() + ","
				+ storage.getLabelField() + ")"
				+ " VALUES(" + (dbmsSupport.autoIncrementKeyMustBeInserted() ? 1 + "," : "") + "'" + type + "','test','localhost','messageId','correlationId'," + dbmsSupport.getDatetimeLiteral(new Date()) + ",'comments', ? ," + dbmsSupport.getDatetimeLiteral(new Date()) + ",'label')";
		return !dbmsSupport.autoIncrementKeyMustBeInserted() ? connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(query, new String[]{storage.getKeyField()});
	}

	private String createMessage() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			sb.append("message");
		}
		return sb.toString();
	}

	@DatabaseTest
	public void testRetrieveObjectWithADifferentColumnNotCompressed() throws Exception {
		assertThrows(JdbcException.class, () -> testRetrieveObjectWithADifferentColumnHelper(false), "unknown compression method");
	}

	@DatabaseTest
	public void testRetrieveObjectWithADifferentColumn() throws Exception {
		assertThrows(JdbcException.class, () -> testRetrieveObjectWithADifferentColumnHelper(true), "invalid stream header");
	}

	public void testRetrieveObjectWithADifferentColumnHelper(boolean blobsCompressed) throws Exception {
		storage.setBlobsCompressed(blobsCompressed);
		storage.configure();

		String message = createMessage();

		try (Connection connection = env.getConnection()) {
			String storeMessageOutput = storage.storeMessage(connection, "1", "correlationId", new Date(), "comment", "label", message);

			String key = storeMessageOutput.substring(storeMessageOutput.indexOf(">") + 1, storeMessageOutput.lastIndexOf("<"));
			String selectQuery = "SELECT * FROM " + tableName + " where " + storage.getKeyField() + "=" + key;

			try (ResultSet rs = connection.prepareStatement(selectQuery).executeQuery()) {
				if (rs.next()) {
					String result = storage.retrieveObject("dummy", rs, 1).getRawMessage();
					assertEquals(message, result);
				} else {
					fail("The query [" + selectQuery + "] returned empty result set expected 1");
				}
			}
		}
	}

	@DatabaseTest
	public void testStoreAndGetMessage() throws Exception {
		storage.configure();

		String message = createMessage();
		String key;
		try (Connection connection = env.getConnection()) {
			String storeMessageOutput = storage.storeMessage(connection, "1", "correlationId", new Date(), "comment", "label", message);

			key = storeMessageOutput.substring(storeMessageOutput.indexOf(">") + 1, storeMessageOutput.lastIndexOf("<"));
		}

		String result = storage.getMessage(key).getRawMessage();
		assertEquals(message, result);
	}

	@DatabaseTest
	public void testGetContext() throws Exception {
		storage.configure();
		String key;

		String message = createMessage();
		try (Connection connection = env.getConnection()) {
			String storeMessageOutput = storage.storeMessage(connection, "1", "correlationId", new Date(), "comment", "label", message);

			key = storeMessageOutput.substring(storeMessageOutput.indexOf(">") + 1, storeMessageOutput.lastIndexOf("<"));
		}

		try (IMessageBrowsingIteratorItem item = storage.getContext(key)) {
			assertEquals("correlationId", item.getCorrelationId());
			assertEquals("comment", item.getCommentString());
			assertEquals("label", item.getLabel());
		}

		String result = storage.getMessage(key).getRawMessage();
		assertEquals(message, result);
	}

}
