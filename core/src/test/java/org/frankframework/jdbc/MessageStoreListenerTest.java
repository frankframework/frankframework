package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.core.ProcessState;
import org.frankframework.core.SenderException;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.JdbcException;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;

@WithLiquibase(tableName = MessageStoreListenerTest.TEST_TABLE_NAME)
public class MessageStoreListenerTest {

	private MessageStoreListener listener;
	private JdbcTransactionalStorage<String> storage;
	static final String TEST_TABLE_NAME = "JDBCTRANSACTIONALSTORAGETEST";
	private final String slotId = "slot";
	private final String messageIdField = "MESSAGEID";

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		assumeTrue(Dbms.H2 == env.getDbmsSupport().getDbms()); // tests are based on H2 syntax queries
		listener = env.createBean(MessageStoreListener.class);
		listener.setTableName(TEST_TABLE_NAME);
		listener.setMessageIdField(messageIdField);
		listener.setSlotId(slotId);

		storage = env.createBean(JdbcTransactionalStorage.class);
		storage.setTableName(TEST_TABLE_NAME);
		storage.setIdField(messageIdField);
		storage.setSlotId(slotId);
	}

	@AfterEach
	public void teardown() throws Exception {
		if(listener != null) {
			listener.close(); //does this trigger an exception
		}
	}

	private JdbcTableMessageBrowser getMessageBrowser(ProcessState state) throws JdbcException, ConfigurationException {
		JdbcTableMessageBrowser browser = (JdbcTableMessageBrowser) listener.getMessageBrowser(state);
		browser.configure();
		return browser;
	}


	@DatabaseTest
	public void testSetup() throws ConfigurationException, ListenerException {
		listener.configure();
		listener.open();
	}

	@DatabaseTest
	public void testSelectQuery() throws ConfigurationException {
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+TEST_TABLE_NAME+" t WHERE TYPE='M' AND (SLOTID='slot')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+TEST_TABLE_NAME+" t WHERE TYPE='M'";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+TEST_TABLE_NAME+" t WHERE TYPE='M' AND (SLOTID='slot' AND (t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithSelectConditionNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+TEST_TABLE_NAME+" t WHERE TYPE='M' AND ((t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testUpdateStatusQueryDone() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE "+TEST_TABLE_NAME+" SET TYPE='A',MESSAGEDATE=NOW(),COMMENTS=?,EXPIRYDATE = NOW() + 30 WHERE TYPE!='A' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
	}

	@DatabaseTest
	public void testUpdateStatusQueryDoneNoMoveToMessageLog() throws ConfigurationException {
		listener.setMoveToMessageLog(false);
		listener.configure();

		String expected = "DELETE FROM "+TEST_TABLE_NAME+" WHERE MESSAGEKEY = ?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
	}

	@DatabaseTest
	public void testUpdateStatusQueryErrorNoMoveToMessageLog() throws ConfigurationException {
		listener.setMoveToMessageLog(false);
		listener.configure();

		String expected = "UPDATE "+TEST_TABLE_NAME+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@DatabaseTest
	public void testUpdateStatusQueryError() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE "+TEST_TABLE_NAME+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@DatabaseTest
	public void testUpdateStatusQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("1=1");
		listener.configure();

		String expected = "UPDATE "+TEST_TABLE_NAME+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@DatabaseTest
	public void testGetMessageCountQueryAvailable() throws Exception {
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM "+TEST_TABLE_NAME+" t WHERE (TYPE='M' AND (SLOTID='slot'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryError() throws Exception {
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM "+TEST_TABLE_NAME+" t WHERE (TYPE='E' AND (SLOTID='slot'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryAvailableWithSelectCondition() throws Exception {
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM "+TEST_TABLE_NAME+" t WHERE (TYPE='M' AND (SLOTID='slot' AND (t.VARCHAR='A')))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryErrorSelectCondition() throws Exception {
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM "+TEST_TABLE_NAME+" t WHERE (TYPE='E' AND (SLOTID='slot' AND (t.VARCHAR='A')))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testMessageBrowserContainsMessageId() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		insertARecord(message, 'M');

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		assertTrue(browser.containsMessageId("fakeMid"));
	}

	@DatabaseTest
	public void testMessageBrowserContainsCorrelationId() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		insertARecord(message, 'M');

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		assertTrue(browser.containsCorrelationId("fakeCid"));
	}

	@DatabaseTest
	public void testMessageBrowserBrowseMessage() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		String storageKey = insertARecord(message, 'M');
		if (storageKey.startsWith("<id>")) {
			storageKey = storageKey.substring(4, storageKey.length()-5);
		}

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		RawMessageWrapper<?> ro = browser.browseMessage(storageKey);
		assertEquals(storageKey, ro.getId());
		Object o = ro.getRawMessage();
		if (o instanceof MessageWrapper mw) {
			assertEquals(message, mw.getMessage().asString());
		} else {
			assertEquals(message, o);
		}
	}

	@DatabaseTest
	public void testMessageBrowserIterator() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		String storageKey = insertARecord(message, 'M');
		if (storageKey.startsWith("<id>")) {
			storageKey = storageKey.substring(4, storageKey.length()-5);
		}

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		IMessageBrowsingIterator iterator = browser.getIterator();
		assertTrue(iterator.hasNext());

		IMessageBrowsingIteratorItem item = iterator.next();
		assertNotNull(item);

		assertEquals(storageKey, item.getId());
		assertEquals("fakeMid", item.getOriginalId());
		assertEquals("fakeCid", item.getCorrelationId());
		assertEquals("fakeComments", item.getCommentString());
	}

	private String insertARecord(String message, char type) throws SenderException, ConfigurationException {
		storage.setType(type+"");
		storage.configure();
		storage.open();
		try {
			return storage.storeMessage("fakeMid", "fakeCid", new Date(), "fakeComments", "fakeLabel", message);
		} finally {
			storage.close();
		}
	}

}
