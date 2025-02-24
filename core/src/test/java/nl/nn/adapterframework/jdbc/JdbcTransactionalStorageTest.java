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
package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.RawMessageWrapper;

public class JdbcTransactionalStorageTest extends TransactionManagerTestBase {

	private JdbcTransactionalStorage<String> storage;
	private final String tableName = "JDBCTRANSACTIONALSTORAGETEST";
	private final String messageField = "MESSAGE";
	private final String keyField = "MESSAGEKEY";

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		storage = getConfiguration().createBean(JdbcTransactionalStorage.class);
		storage.setTableName(tableName);
		storage.setMessageField(messageField);
		storage.setKeyField(keyField);
		storage.setCheckTable(false);
		storage.setSequenceName("SEQ_"+tableName);
		storage.setSlotId("test");
		System.setProperty("tableName", tableName);
		autowire(storage);

		runMigrator(TEST_CHANGESET_PATH);
	}

	@Test
	public void testQueryTextAndBrowseMessage() throws Exception {
		testQueryTextAndBrowseMessageHelper(true);
	}
	@Test
	public void testQueryTextAndBrowseMessageNotCompressed() throws Exception {
		testQueryTextAndBrowseMessageHelper(false);
	}

	public void testQueryTextAndBrowseMessageHelper(boolean blobsCompressed) throws Exception {
		storage.setBlobsCompressed(blobsCompressed);
		storage.configure();
		// check created query
		String expected = "SELECT "+keyField+","+messageField+","+storage.getIdField()+","+storage.getCorrelationIdField()+" FROM "+tableName+" WHERE "+keyField+"=?";
		String query = storage.selectDataQuery;
		assertEquals(expected, query);

		String message = createMessage();
		String storageKey = insertARecord(blobsCompressed, message, 'E');

		RawMessageWrapper<String> rawMessageWrapper = storage.browseMessage(storageKey);
		String data = rawMessageWrapper.getRawMessage();
		assertEquals(storageKey, rawMessageWrapper.getContext().get(PipeLineSession.STORAGE_ID_KEY));
		assertEquals(message, data);
	}

	@Test
	public void testRetrieveObject() throws Exception {
		testRetrieveObjectHelper(true);
	}

	@Test
	public void testRetrieveObjectNotCompressed() throws Exception {
		testRetrieveObjectHelper(false);
	}

	public void testRetrieveObjectHelper(boolean blobsCompressed) throws Exception {
		storage.setBlobsCompressed(blobsCompressed);
		storage.configure();

		String message = createMessage();

		// insert a record
		try (PreparedStatement stmt = prepareStatement(connection,'E')) {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStream out = blobsCompressed ? new DeflaterOutputStream(baos) : baos;
			try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
				oos.writeObject(message);
			}
			stmt.setBytes(1, baos.toByteArray());
			stmt.execute();

			String selectQuery = "SELECT * FROM "+tableName;
			try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
				ResultSet rs = statement.executeQuery();
				if(rs.next()) {
					String result = storage.retrieveObject("dummy", rs, 9).getRawMessage();
					assertEquals(message,result);
				} else {
					Assert.fail("The query ["+selectQuery+"] returned empty result set expected 1");
				}
			}
		}
	}

	@Test
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
		try (Connection connection = getConnection()) {
			try (PreparedStatement stmt = prepareStatement(connection, type)) {

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				OutputStream out = blobsCompressed ? new DeflaterOutputStream(baos) : baos;
				try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
					oos.writeObject(message);
				}
				stmt.setBytes(1, baos.toByteArray());
				stmt.execute();

				try (ResultSet rs = stmt.getGeneratedKeys()) {
					if(rs.next()) {
						// check inserted data being correctly retrieved
						return rs.getString(1);
					}
					Assert.fail("The query ["+storage.selectDataQuery+"] returned empty result set expected 1");
					return null;
				}
			}
		}
	}

	private PreparedStatement prepareStatement(Connection connection, char type) throws SQLException {
		String query ="INSERT INTO "+tableName+" (" +
			(dbmsSupport.autoIncrementKeyMustBeInserted() ? storage.getKeyField()+"," : "")
			+ storage.getTypeField() + ","
			+ storage.getSlotIdField() + ","
			+ storage.getHostField() + ","
			+ storage.getIdField() + ","
			+ storage.getCorrelationIdField() + ","
			+ storage.getDateField() + ","
			+ storage.getCommentField() + ","
			+ storage.getMessageField() + ","
			+ storage.getExpiryDateField()  +","
			+ storage.getLabelField() + ")"
			+ " VALUES("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? 1+"," : "")+"'"+type+"','test','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(new Date())+",'comments', ? ,"+dbmsSupport.getDatetimeLiteral(new Date())+",'label')";
		return !dbmsSupport.autoIncrementKeyMustBeInserted() ? connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(query, new String[]{storage.getKeyField()});
	}

	private String createMessage() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<5;i++) {
			sb.append("message");
		}
		return sb.toString();
	}

	@Test
	public void testRetrieveObjectWithADifferentColumnNotCompressed() throws Exception {
		assertThrows("unknown compression method", JdbcException.class, () -> {
			testRetrieveObjectWithADifferentColumnHelper(false);
		});
	}

	@Test
	public void testRetrieveObjectWithADifferentColumn() throws Exception {
		assertThrows("invalid stream header", JdbcException.class, () -> {
			testRetrieveObjectWithADifferentColumnHelper(true);
		});
	}
	public void testRetrieveObjectWithADifferentColumnHelper(boolean blobsCompressed) throws Exception {
		storage.setBlobsCompressed(blobsCompressed);
		storage.configure();

		String message = createMessage();

		try (Connection connection = getConnection()) {
			String storeMessageOutput = storage.storeMessage(connection,"1", "correlationId", new Date(), "comment", "label", message);

			String key = storeMessageOutput.substring(storeMessageOutput.indexOf(">")+1, storeMessageOutput.lastIndexOf("<"));
			String selectQuery = "SELECT * FROM "+tableName+" where "+storage.getKeyField()+"="+key;

			try (ResultSet rs = connection.prepareStatement(selectQuery).executeQuery()) {
				if(rs.next()) {
					String result = storage.retrieveObject("dummy", rs, 1).getRawMessage();
					assertEquals(message,result);
				} else {
					Assert.fail("The query ["+selectQuery+"] returned empty result set expected 1");
				}
			}
		}
	}

	@Test
	public void testStoreAndGetMessage() throws Exception {
		storage.configure();

		String message = createMessage();
		String key;
		try (Connection connection = getConnection()) {
			String storeMessageOutput = storage.storeMessage(connection,"1", "correlationId", new Date(), "comment", "label", message);

			key = storeMessageOutput.substring(storeMessageOutput.indexOf(">")+1, storeMessageOutput.lastIndexOf("<"));
		}

		String result = storage.getMessage(key).getRawMessage();
		assertEquals(message,result);
	}

	@Test
	public void testGetContext() throws Exception {
		storage.configure();
		String key = null;

		String message = createMessage();
		try (Connection connection = getConnection()) {
			String storeMessageOutput = storage.storeMessage(connection,"1", "correlationId", new Date(), "comment", "label", message);

			key = storeMessageOutput.substring(storeMessageOutput.indexOf(">")+1, storeMessageOutput.lastIndexOf("<"));
		}

		try(IMessageBrowsingIteratorItem item = storage.getContext(key)){
			assertEquals("correlationId", item.getCorrelationId());
			assertEquals("comment", item.getCommentString());
			assertEquals("label", item.getLabel());
		}

		String result = storage.getMessage(key).getRawMessage();
		assertEquals(message,result);
	}

}
