package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;

public class JdbcTransactionalStorageTest extends TransactionManagerTestBase {

	private JdbcTransactionalStorage<Message> storage;
	private boolean tableCreated = false;
	private final String tableName = "IBISSTORE";
	private final String messageField = "MESSAGE";
	private final String keyField = "MESSAGEKEY";

	public JdbcTransactionalStorageTest() {}

	@Before
	public void setup() throws Exception {
		super.setup();
		storage = new JdbcTransactionalStorage<Message>();
		storage.setTableName(tableName);
		storage.setMessageField(messageField);
		storage.setKeyField(keyField);
		storage.setCheckTable(false);
		storage.setDatasourceName(getDataSourceName());
		storage.setDataSourceFactory(dataSourceFactory);
		if (!dbmsSupport.isTablePresent(connection, tableName)) {
			createDbTable();
			tableCreated = true;
		}
	}

	private void createDbTable() throws Exception {
		JdbcUtil.executeStatement(connection, "CREATE TABLE "+tableName+"("
				+ storage.getKeyField()+" "+dbmsSupport.getNumericKeyFieldType()+" IDENTITY NOT NULL, "
				+ storage.getTypeField()+" "+dbmsSupport.getTextFieldType()+"(1), "
				+ storage.getSlotIdField()+" "+dbmsSupport.getTextFieldType()+"(100), "
				+ storage.getHostField()+" "+dbmsSupport.getTextFieldType()+"(100), "
				+ storage.getIdField()+" "+dbmsSupport.getTextFieldType()+"(100), "
				+ storage.getCorrelationIdField()+" "+dbmsSupport.getTextFieldType()+"(256), "
				+ storage.getDateField()+" "+dbmsSupport.getTimestampFieldType()+", "
				+ storage.getCommentField()+" "+dbmsSupport.getTextFieldType()+"(1000), "
				+ storage.getMessageField()+" "+dbmsSupport.getBlobFieldType()+", "
				+ storage.getExpiryDateField()+" "+dbmsSupport.getTimestampFieldType()+", "
				+ storage.getLabelField()+" "+dbmsSupport.getTextFieldType()+"(100), "
				+ "CONSTRAINT PK_IBISSTORE PRIMARY KEY("+storage.getKeyField()+"));"); 
	}

	@After
	public void teardown() throws Exception {
		if (tableCreated) {
			JdbcUtil.executeStatement(connection, "DROP TABLE "+tableName); // drop the table if it was created, to avoid interference with Liquibase
		}
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

		storage.createQueryTexts(dbmsSupport);
		// check created query
		String expected = "SELECT "+keyField+","+messageField+" FROM "+tableName+" WHERE "+keyField+"=?";
		String query = storage.selectDataQuery; 
		assertEquals(expected, query);

		Message message = Message.asMessage("message");
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<5;i++) {
			sb.append(message);
		}

		// insert a record 
		PreparedStatement stmt = prepareStatement();

		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		OutputStream out = blobsCompressed ? new DeflaterOutputStream(baos) : baos;
		try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
			oos.writeObject(message);
		}
		stmt.setBytes(1, baos.toByteArray());
		stmt.execute();
		// check inserted data being correctly retrieved
		Message data =  storage.browseMessage("1");
		assertEquals(message.asString(), data.asString());

	}

	@Test
	public void testRetriveObject() throws Exception {
		testRetriveObjectHelper(true);
	}

	@Test
	public void testRetrieveObjectNotCompressed() throws Exception {
		testRetriveObjectHelper(false);
	}

	public void testRetriveObjectHelper(boolean blobsCompressed) throws Exception {
		storage.setBlobsCompressed(blobsCompressed);
		storage.configure();

		Message message = createMessage();

		// insert a record 
		PreparedStatement stmt = prepareStatement();

		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		OutputStream out = blobsCompressed ? new DeflaterOutputStream(baos) : baos;
		try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
			oos.writeObject(message);
		}
		stmt.setBytes(1, baos.toByteArray());
		stmt.execute();

		ResultSet rs = connection.prepareStatement("SELECT * FROM "+tableName).executeQuery();

		if(rs.next()) {
			Message result = storage.retrieveObject(rs, 9);
			assertEquals(message.asString(),result.asString());
		}
	}

	private PreparedStatement prepareStatement() throws SQLException {
		return connection.prepareStatement("INSERT INTO "+tableName+" VALUES"
				+ "(1,'E','test','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments',?,'2021-07-13 11:04:19.860','label')");
	}

	private Message createMessage() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<5;i++) {
			sb.append("message");
		}
		return Message.asMessage(sb.toString());
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

		Message message = createMessage();

		storage.storeMessage("1", "correlationId", new Date(), "comment", "label", message);

		ResultSet rs = connection.prepareStatement("SELECT * FROM "+tableName+" where "+storage.getKeyField()+"=1").executeQuery();

		if(rs.next()) {
			Message result = storage.retrieveObject(rs, 1);
			assertEquals(message.asString(),result.asString());
		}
	}

	@Test
	public void testStoreAndGetMessage() throws Exception {
		storage.configure();

		Message message = createMessage();

		storage.storeMessage(connection,"1", "correlationId", new Date(), "comment", "label", message);
		Message result = storage.getMessage("1");
		assertEquals(message.asString(),result.asString());
	}
}
