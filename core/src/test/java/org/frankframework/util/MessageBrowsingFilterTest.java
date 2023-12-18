package org.frankframework.util;


import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import org.frankframework.core.IListener;
import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;

import org.frankframework.dbms.Dbms;
import org.frankframework.jdbc.JdbcTransactionalStorage;
import org.frankframework.jdbc.TransactionManagerTestBase;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;

public class MessageBrowsingFilterTest extends TransactionManagerTestBase {

	private MessageBrowsingFilter filter;
	private JdbcTransactionalStorage storage = null;
	private IListener<?> listener = null;
	private final String tableName = "MESSAGEBROWSINGFILTERTEST";

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
		storage.storeMessage("1", "corrId", new Date(), "comments", "label", messageRoot);
		storage.storeMessage("2", "corrId2", new Date(), "comments", "label", "out filter");

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
					+ "SELECT 1,'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 2,'L','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 3,'M','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 4,'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 5,'L','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL UNION ALL "
					+ "SELECT 6,'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments',"+dbmsSupport.getDatetimeLiteral(date)+",'label' FROM DUAL) SELECT * FROM valuesTable");
		} else {
			sb.append(" VALUES"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "1," : "")+"'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "2," : "")+"'L','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "3," : "")+"'M','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "4," : "")+"'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "5," : "")+"'L','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments','2021-07-13 11:04:19.860','label'),"
					+ "("+(dbmsSupport.autoIncrementKeyMustBeInserted() ? "6," : "")+"'E','MessageBrowsingFilter','localhost','messageId','correlationId',"+dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860"))+",'comments','2021-07-13 11:04:19.860','label')");
		}

		try(Connection connection = getConnection()) {
			try(PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
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
	public void testMessageFilterWithMessageWrapper() throws Exception {
		MessageWrapper messageInFilter = new MessageWrapper(new Message("message"), "firstMessageID", null);
		MessageWrapper messageOutOfFilter = new MessageWrapper(new Message("out filter"), "id", null);
		testMessageFilterWithJavaListenerHelper(messageInFilter, messageOutOfFilter);
	}

	@Test
	public void testMessageFilterWithMessage() throws Exception {
		testMessageFilterWithJavaListenerHelper("message", "out filter");
	}

	@Test
	public void testMessageFilterWithString() throws Exception {
		testMessageFilterWithJavaListenerHelper("message", "out filter");
	}

	public void testMessageFilterWithJavaListenerHelper(Serializable messageInFilter, Serializable messageOutOfFilter) throws Exception {
		String messageRoot = "message";

		filter.setMessageMask(messageRoot, storage, listener);
		storage.configure();
		storage.storeMessage("1", "corrId", new Date(), "comments", "label", messageInFilter);
		storage.storeMessage("2", "corrId2", new Date(), "comments", "label", messageOutOfFilter);

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
