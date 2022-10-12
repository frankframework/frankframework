package nl.nn.adapterframework.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.doAnswer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.jdbc.dbms.Dbms;

public class MessageStoreListenerTest extends JdbcTestBase {

	private MessageStoreListener listener;


	@Before
	@Override
	public void setup() throws Exception {
		super.setup();
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener = new MessageStoreListener();
		autowire(listener);
		listener.setSlotId("slot");
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

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM IBISSTORE t WHERE TYPE='M' AND (SLOTID='slot')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectQueryNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM IBISSTORE t WHERE TYPE='M'";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM IBISSTORE t WHERE TYPE='M' AND (SLOTID='slot' AND (t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectQueryWithSelectConditionNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM IBISSTORE t WHERE TYPE='M' AND ((t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testUpdateStatusQueryDone() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE IBISSTORE SET TYPE='A',MESSAGEDATE=NOW(),COMMENTS=?,EXPIRYDATE = NOW() + 30 WHERE TYPE!='A' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
	}

	@Test
	public void testUpdateStatusQueryDoneNoMoveToMessageLog() throws ConfigurationException {
		listener.setMoveToMessageLog(false);
		listener.configure();

		String expected = "DELETE FROM IBISSTORE WHERE MESSAGEKEY = ?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
	}

	@Test
	public void testUpdateStatusQueryError() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE IBISSTORE SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@Test
	public void testUpdateStatusQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("1=1");
		listener.configure();

		String expected = "UPDATE IBISSTORE SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@Test
	public void testGetMessageCountQueryAvailable() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM IBISSTORE t WHERE (TYPE='M' AND (SLOTID='slot'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryError() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM IBISSTORE t WHERE (TYPE='E' AND (SLOTID='slot'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryAvailableWithSelectCondition() throws Exception {
		listener.setSelectCondition("t.VARCHAR='A'");
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM IBISSTORE t WHERE (TYPE='M' AND (SLOTID='slot' AND (t.VARCHAR='A')))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryErrorSelectCondition() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM IBISSTORE t WHERE (TYPE='E' AND (SLOTID='slot' AND (t.VARCHAR='A')))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

}
