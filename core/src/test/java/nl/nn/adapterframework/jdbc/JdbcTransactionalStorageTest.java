package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class JdbcTransactionalStorageTest extends TransactionManagerTestBase {

	private JdbcTransactionalStorage<Message> storage;
	private Liquibase liquibase;
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
		FileSystemResourceAccessor resourceAccessor = new FileSystemResourceAccessor(TestFileUtils.getTestFileURL("/").getPath());
		String changesetFilePath = TestFileUtils.getTestFileURL("/Migrator/Ibisstore_changeset.xml").getPath();
		liquibase = new Liquibase(changesetFilePath, resourceAccessor, new JdbcConnection(connection));
		liquibase.update(2, null);
	}

	@After
	public void teardown() throws Exception {
		if (tableCreated) {
			liquibase.rollback(2, null);
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
		if(!connection.getAutoCommit()) {
			connection.commit();
		}

		ResultSet rs = stmt.getGeneratedKeys();
		rs.next();
		// check inserted data being correctly retrieved
		Message data =  storage.browseMessage(rs.getString(1));
		assertEquals(message.asString(), data.asString());
		rs.close();
		stmt.close();

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
		if(!connection.getAutoCommit()) {
			connection.commit();
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM "+tableName).executeQuery();

		rs.next();
		Message result = storage.retrieveObject(rs, 9);
		assertEquals(message.asString(),result.asString());
		
		rs.close();
		stmt.close();
	}

	private PreparedStatement prepareStatement() throws SQLException {
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
				+ " VALUES("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? 1+"," : "")+"'E','test','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(new Date())+",'comments', ? ,"+dbmsSupport.getDatetimeLiteral(new Date())+",'label')";
		return !dbmsSupport.autoIncrementKeyMustBeInserted() ? connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(query);
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

		String storeMessageOutput = storage.storeMessage(connection,"1", "correlationId", new Date(), "comment", "label", message);
		if(!connection.getAutoCommit()) {
			connection.commit();
		}
		String key = storeMessageOutput.substring(storeMessageOutput.indexOf(">")+1, storeMessageOutput.lastIndexOf("<"));
		ResultSet rs = connection.prepareStatement("SELECT * FROM "+tableName+" where "+storage.getKeyField()+"="+key).executeQuery();

		rs.next();
		Message result = storage.retrieveObject(rs, 1);
		assertEquals(message.asString(),result.asString());
		rs.close();
	}

	@Test
	public void testStoreAndGetMessage() throws Exception {
		storage.configure();

		Message message = createMessage();
		String storeMessageOutput = storage.storeMessage(connection,"1", "correlationId", new Date(), "comment", "label", message);
		if(!connection.getAutoCommit()) {
			connection.commit();
		}
		String key = storeMessageOutput.substring(storeMessageOutput.indexOf(">")+1, storeMessageOutput.lastIndexOf("<"));

		Message result = storage.getMessage(key);
		assertEquals(message.asString(),result.asString());
	}
}
