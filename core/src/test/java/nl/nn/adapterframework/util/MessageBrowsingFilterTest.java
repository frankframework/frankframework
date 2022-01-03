package nl.nn.adapterframework.util;


import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.stream.Message;

public class MessageBrowsingFilterTest extends TransactionManagerTestBase {

	private MessageBrowsingFilter filter;
	private JdbcTransactionalStorage storage = null;
	private IListener<?> listener = null;
	private String tableName="MESSAGEBROWSINGFILTERTEST";

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		filter = new MessageBrowsingFilter();
		storage = new JdbcTransactionalStorage();
		storage.setSlotId("MessageBrowsingFilter");
		storage.setTableName(tableName);
		storage.setSequenceName("SEQ_"+tableName);
		autowire(storage);
		System.setProperty("tableName", tableName);
		runMigrator(TEST_CHANGESET_PATH);

		listener = new JavaListener();
	}

	@Test
	public void testMessageFilter() throws Exception {
		String messageRoot = "message";
		filter.setMessageMask(messageRoot, storage);
		storage.configure();
		storage.storeMessage(getConnection(),"1", "corrId", new Date(), "comments", "label", new Message(messageRoot));
		storage.storeMessage(getConnection(),"2", "corrId2", new Date(), "comments", "label", new Message("out filter"));
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
	private void fillTable() throws Exception {
		StringBuilder sb = new StringBuilder("INSERT INTO "+tableName+" (" +
				(dbmsSupport.autoIncrementKeyMustBeInserted() ? storage.getKeyField()+"," : "")
				+ storage.getTypeField() + ","
				+ storage.getSlotIdField() + ","
				+ storage.getHostField() + ","
				+ storage.getIdField() + ","
				+ storage.getCorrelationIdField() + ","
				+ storage.getDateField() + ","
				+ storage.getCommentField() + ","
				+ storage.getExpiryDateField()  +","
				+ storage.getLabelField() + ")");
		Date date = new Date();

		if(dbmsSupport.getDbms() == Dbms.ORACLE) {
			sb.append(" WITH valuesTable AS ("
					+ "SELECT 1,'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 2,'L','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 3,'M','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 4,'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 5,'L','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 6,'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL) SELECT * FROM valuesTable");
		} else {
			sb.append(" VALUES"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "1," : "")+"'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "2," : "")+"'L','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "3," : "")+"'M','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "4," : "")+"'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "5," : "")+"'L','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "6," : "")+"'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments','2021-07-13 11:04:19.860','label')");
		}

		try(Connection connection = getConnection()) {
			try(PreparedStatement stmt = getConnection().prepareStatement(sb.toString())) {
				stmt.execute();
			}
		}
	}

	@Test
	public void testTypeFilter() throws Exception {
		filter.setTypeMask("L");
		storage.configure();
		
		fillTable();

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
		fillTable();
		
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
		storage.storeMessage(getConnection(),"1", "corrId", new Date(), "comments", "label", "message");
		storage.storeMessage(getConnection(),"2", "corrId2", new Date(), "comments", "label", "out filter");
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
