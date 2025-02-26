package nl.nn.adapterframework.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.doAnswer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;

public class MessageStoreListenerTest extends JdbcTestBase {

	private MessageStoreListener listener;
	private JdbcTransactionalStorage<String> storage;
	private final String tableName = "JDBCTRANSACTIONALSTORAGETEST";
	private final String slotId = "slot";
	private final String messageIdField = "MESSAGEID";


	@Before
	@Override
	public void setup() throws Exception {
		super.setup();
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2)); // tests are based on H2 syntax queries
		listener = getConfiguration().createBean(MessageStoreListener.class);
		autowire(listener);
		listener.setTableName(tableName);
		listener.setMessageIdField(messageIdField);
		listener.setSlotId(slotId);

		storage = getConfiguration().createBean(JdbcTransactionalStorage.class);
		autowire(storage);
		storage.setTableName(tableName);
		storage.setIdField(messageIdField);
		storage.setSlotId(slotId);
		System.setProperty("tableName", tableName);

		runMigrator(TEST_CHANGESET_PATH);
	}

	@After
	@Override
	public void teardown() throws Exception {
		if(listener != null) {
			listener.close();
		}
		super.teardown();
	}


	public JdbcTableMessageBrowser getMessageBrowser(ProcessState state) throws JdbcException, ConfigurationException {
		JdbcTableMessageBrowser browser = Mockito.spy((JdbcTableMessageBrowser)listener.getMessageBrowser(state));
		doAnswer(arg -> {
			autowire(browser);
			return null;
		}).when(browser).copyFacadeSettings(listener);
		browser.configure();
		return browser;
	}


	@Test
	public void testSetup() throws ConfigurationException, ListenerException {
		listener.configure();
		listener.open();
	}

	@Test
	public void testSelectQuery() throws ConfigurationException {
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+tableName+" t WHERE TYPE='M' AND (SLOTID='slot')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectQueryNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+tableName+" t WHERE TYPE='M'";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+tableName+" t WHERE TYPE='M' AND (SLOTID='slot' AND (t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectQueryWithSelectConditionNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+tableName+" t WHERE TYPE='M' AND ((t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testUpdateStatusQueryDone() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE "+tableName+" SET TYPE='A',MESSAGEDATE=NOW(),COMMENTS=?,EXPIRYDATE = NOW() + 30 WHERE TYPE!='A' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
	}

	@Test
	public void testUpdateStatusQueryDoneNoMoveToMessageLog() throws ConfigurationException {
		listener.setMoveToMessageLog(false);
		listener.configure();

		String expected = "DELETE FROM "+tableName+" WHERE MESSAGEKEY = ?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
	}

	@Test
	public void testUpdateStatusQueryError() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE "+tableName+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@Test
	public void testUpdateStatusQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("1=1");
		listener.configure();

		String expected = "UPDATE "+tableName+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@Test
	public void testGetMessageCountQueryAvailable() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='M' AND (SLOTID='slot'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryError() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='E' AND (SLOTID='slot'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryAvailableWithSelectCondition() throws Exception {
		listener.setSelectCondition("t.VARCHAR='A'");
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='M' AND (SLOTID='slot' AND (t.VARCHAR='A')))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryErrorSelectCondition() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='E' AND (SLOTID='slot' AND (t.VARCHAR='A')))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testMessageBrowserContainsMessageId() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		insertARecord(message, 'M');

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		assertTrue(browser.containsMessageId("fakeMid"));
	}

	@Test
	public void testMessageBrowserContainsCorrelationId() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		insertARecord(message, 'M');

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		assertTrue(browser.containsCorrelationId("fakeCid"));
	}

	@Test
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
		assertEquals(message, ro.getRawMessage());
		Object o = ro.getRawMessage();
		if (o instanceof MessageWrapper) {
			MessageWrapper mw = (MessageWrapper)o;
			assertEquals(message, mw.getMessage().asString());
		} else {
			assertEquals(message, o);
		}
	}

	@Test
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

	private String insertARecord(String message, char type) throws SQLException, IOException, SenderException, ConfigurationException {
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
