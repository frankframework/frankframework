package org.frankframework.util;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;

import org.frankframework.core.IListener;
import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.jdbc.JdbcTransactionalStorage;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.TxManagerTest;
import org.frankframework.testutil.junit.WithLiquibase;

@WithLiquibase(tableName = MessageBrowsingFilterTest.tableName)
public class MessageBrowsingFilterTest {

	private MessageBrowsingFilter filter;
	private JdbcTransactionalStorage storage = null;
	private IListener<?> listener = null;
	static final String tableName = "MESSAGEBROWSINGFILTERTEST";

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		filter = env.createBean(MessageBrowsingFilter.class);
		storage = env.createBean(JdbcTransactionalStorage.class);
		storage.setSlotId("MessageBrowsingFilter");
		storage.setTableName(tableName);
		storage.setSequenceName("SEQ_"+tableName);
		listener = new JavaListener();
	}

	@TxManagerTest
	public void testMessageFilter() throws Exception {
		String messageRoot = "message";
		filter.setMessageMask(messageRoot, storage);
		storage.configure();
		storage.storeMessage("1", "corrId", TimeProvider.nowAsDate(), "comments", "label", messageRoot);
		storage.storeMessage("2", "corrId2", TimeProvider.nowAsDate(), "comments", "label", "out filter");

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

	private void fillTable(DatabaseTestEnvironment env) throws Exception {
		IDbmsSupport dbmsSupport = env.getDbmsSupport();

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
		Date date = TimeProvider.nowAsDate();

		if (dbmsSupport.getDbms() == Dbms.ORACLE) {
			sb.append(" WITH valuesTable AS (" + "SELECT 1,'E','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860")))
					.append(",'comments',")
					.append(dbmsSupport.getDatetimeLiteral(date))
					.append(",'label' FROM DUAL UNION ALL " + "SELECT 2,'L','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860")))
					.append(",'comments',")
					.append(dbmsSupport.getDatetimeLiteral(date))
					.append(",'label' FROM DUAL UNION ALL " + "SELECT 3,'M','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860")))
					.append(",'comments',")
					.append(dbmsSupport.getDatetimeLiteral(date))
					.append(",'label' FROM DUAL UNION ALL " + "SELECT 4,'E','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860")))
					.append(",'comments',")
					.append(dbmsSupport.getDatetimeLiteral(date))
					.append(",'label' FROM DUAL UNION ALL " + "SELECT 5,'L','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860")))
					.append(",'comments',")
					.append(dbmsSupport.getDatetimeLiteral(date))
					.append(",'label' FROM DUAL UNION ALL " + "SELECT 6,'E','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860")))
					.append(",'comments',")
					.append(dbmsSupport.getDatetimeLiteral(date))
					.append(",'label' FROM DUAL) SELECT * FROM valuesTable");
		} else {
			sb.append(" VALUES" + "(")
					.append(dbmsSupport.autoIncrementKeyMustBeInserted() ? "1," : "")
					.append("'E','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860")))
					.append(",'comments','2021-07-13 11:04:19.860','label')," + "(")
					.append(dbmsSupport.autoIncrementKeyMustBeInserted() ? "2," : "")
					.append("'L','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860")))
					.append(",'comments','2021-07-13 11:04:19.860','label')," + "(")
					.append(dbmsSupport.autoIncrementKeyMustBeInserted() ? "3," : "")
					.append("'M','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860")))
					.append(",'comments','2021-07-13 11:04:19.860','label')," + "(")
					.append(dbmsSupport.autoIncrementKeyMustBeInserted() ? "4," : "")
					.append("'E','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:08:19.860")))
					.append(",'comments','2021-07-13 11:04:19.860','label')," + "(")
					.append(dbmsSupport.autoIncrementKeyMustBeInserted() ? "5," : "")
					.append("'L','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860")))
					.append(",'comments','2021-07-13 11:04:19.860','label')," + "(")
					.append(dbmsSupport.autoIncrementKeyMustBeInserted() ? "6," : "")
					.append("'E','MessageBrowsingFilter','localhost','messageId','correlationId',")
					.append(dbmsSupport.getDatetimeLiteral(DateFormatUtils.parseAnyDate("2021-07-13 11:04:19.860")))
					.append(",'comments','2021-07-13 11:04:19.860','label')");
		}

		try(Connection connection = env.getConnection()) {
			try(PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
				stmt.execute();
			}
		}
	}

	@TxManagerTest
	public void testTypeFilter(DatabaseTestEnvironment env) throws Exception {
		filter.setTypeMask("L");
		storage.configure();

		fillTable(env);

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

	@TxManagerTest
	public void testDateFilter(DatabaseTestEnvironment env) throws Exception {
		filter.setStartDateMask("2021-07-13 11:03:19.860");
		filter.setEndDateMask("2021-07-13 11:07:19.860");
		storage.configure();

		fillTable(env);

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

	@TxManagerTest
	public void testMessageFilterWithMessageWrapper() throws Exception {
		MessageWrapper messageInFilter = new MessageWrapper(new Message("message"), "firstMessageID", null);
		MessageWrapper messageOutOfFilter = new MessageWrapper(new Message("out filter"), "id", null);
		testMessageFilterWithJavaListenerHelper(messageInFilter, messageOutOfFilter);
	}

	@TxManagerTest
	public void testMessageFilterWithMessage() throws Exception {
		testMessageFilterWithJavaListenerHelper("message", "out filter");
	}

	@TxManagerTest
	public void testMessageFilterWithString() throws Exception {
		testMessageFilterWithJavaListenerHelper("message", "out filter");
	}

	public void testMessageFilterWithJavaListenerHelper(Serializable messageInFilter, Serializable messageOutOfFilter) throws Exception {
		String messageRoot = "message";

		filter.setMessageMask(messageRoot, storage, listener);
		storage.configure();
		storage.storeMessage("1", "corrId", TimeProvider.nowAsDate(), "comments", "label", messageInFilter);
		storage.storeMessage("2", "corrId2", TimeProvider.nowAsDate(), "comments", "label", messageOutOfFilter);

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
