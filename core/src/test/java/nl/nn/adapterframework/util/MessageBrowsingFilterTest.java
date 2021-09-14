package nl.nn.adapterframework.util;


import static org.junit.Assert.assertEquals;

import java.sql.PreparedStatement;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.stream.Message;

public class MessageBrowsingFilterTest extends TransactionManagerTestBase {
	
	private MessageBrowsingFilter filter;
	private JdbcTransactionalStorage storage = null;
	private IListener<?> listener = null;
	
	private boolean tableCreated = false;
	
	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		filter = new MessageBrowsingFilter();
		storage = new JdbcTransactionalStorage();
		storage.setSlotId("MessageBrowsingFilter");
		storage.setDatasourceName(getDataSourceName());
		storage.setDataSourceFactory(dataSourceFactory);
		
		if (!dbmsSupport.isTablePresent(connection, storage.getTableName())) {
			createDbTable();
			tableCreated = true;
		}
		
		listener = new JavaListener();
	}

	@After
	public void teardown() throws Exception {
		if (tableCreated) {
			JdbcUtil.executeStatement(connection, "DROP TABLE "+storage.getTableName()); // drop the table if it was created, to avoid interference with Liquibase
		}
	}
	
	private void createDbTable() throws Exception {
		JdbcUtil.executeStatement(connection, "CREATE TABLE "+storage.getTableName()+"("
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
	
	@Test
	public void testMessageFilter() throws Exception {
		String messageRoot = "message";
		filter.setMessageMask(messageRoot, storage);
		storage.configure();
		storage.storeMessage(connection,"1", "corrId", new Date(), "comments", "label", new Message(messageRoot));
		storage.storeMessage(connection,"2", "corrId2", new Date(), "comments", "label", new Message("out filter"));
		int count = 0 ;
		try(IMessageBrowsingIterator iterator = storage.getIterator()){
			while(iterator.hasNext()) {
				try (IMessageBrowsingIteratorItem item = iterator.next()) {
					count += filter.matchAny(item) ? 1 : 0;
				}
			}
		}
		
		assertEquals(1, count);
	}
	
	@Test
	public void testTypeFilter() throws Exception {
		filter.setTypeMask("L");
		storage.configure();
		
		PreparedStatement stmt = connection.prepareStatement("INSERT INTO IBISSTORE VALUES"
				+ "(1,'E','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(2,'L','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(3,'M','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(4,'E','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(5,'L','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(6,'E','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label')");
		stmt.execute();
		
		int count = 0 ;
		try(IMessageBrowsingIterator iterator = storage.getIterator()){
			while(iterator.hasNext()) {
				try (IMessageBrowsingIteratorItem item = iterator.next()) {
					count += filter.matchAny(item) ? 1 : 0;
				}
			}
		}
		
		assertEquals(2, count);
	}
	
	@Test
	public void testDateFilter() throws Exception {
		filter.setStartDateMask("2021-07-13 11:03:19.860");
		filter.setEndDateMask("2021-07-13 11:07:19.860");
		storage.configure();
		PreparedStatement stmt = connection.prepareStatement("INSERT INTO IBISSTORE VALUES"
				+ "(1,'E','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(2,'L','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:08:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(3,'M','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:08:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(4,'E','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:08:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(5,'L','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label'),"
				+ "(6,'E','MessageBrowsingFilter','localhost','messageId','correlationId','2021-07-13 11:04:19.860','comments','','2021-07-13 11:04:19.860','label')");
		stmt.execute();
		
		int count = 0;
		try(IMessageBrowsingIterator iterator = storage.getIterator()){
			while(iterator.hasNext()) {
				try (IMessageBrowsingIteratorItem item = iterator.next()) {
					count += filter.matchAny(item) ? 1 : 0;
				}
			}
		}
		assertEquals(3,count);
	}
	
	@Test
	public void testMessageFilterWithListener() throws Exception {
		String messageRoot = "message";
		
		filter.setMessageMask(messageRoot, storage, listener);
		storage.configure();
		storage.storeMessage(connection,"1", "corrId", new Date(), "comments", "label", "message");
		storage.storeMessage(connection,"2", "corrId2", new Date(), "comments", "label", "out filter");
		int count = 0 ;
		try(IMessageBrowsingIterator iterator = storage.getIterator()){
			while(iterator.hasNext()) {
				try (IMessageBrowsingIteratorItem item = iterator.next()) {
					count += filter.matchAny(item) ? 1 : 0;
				}
			}
		}
		
		assertEquals(1, count);
	}
	
}
