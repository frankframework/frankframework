package nl.nn.adapterframework.jta.narayana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jms.JMSException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.narayana.jta.jms.TransactionHelper;
import org.junit.Before;
import org.junit.Test;

public class NarayanaTransactionHelperTest {

	private TransactionManager txManager;
	private TransactionHelper txHelper;

	@Before
	public void setup() {
		txManager = mock(TransactionManager.class);
		txHelper = new NarayanaTransactionHelper(txManager);
	}

	@Test
	public void testIsActiveTransactionAvailable1() throws Exception {
		when(txManager.getStatus()).thenReturn(Status.STATUS_ACTIVE);
		assertTrue(txHelper.isTransactionAvailable());
		verify(txManager, times(2)).getStatus();
	}
	@Test
	public void testIsActiveTransactionAvailable2() throws Exception {
		when(txManager.getStatus()).thenReturn(Status.STATUS_PREPARING);
		assertTrue(txHelper.isTransactionAvailable());
		verify(txManager, times(2)).getStatus();
	}
	@Test
	public void testIsActiveTransactionAvailable3() throws Exception {
		when(txManager.getStatus()).thenReturn(Status.STATUS_MARKED_ROLLBACK);
		assertTrue(txHelper.isTransactionAvailable());
		verify(txManager, times(2)).getStatus();
	}

	@Test
	public void isNotActiveTransactionAvailable() throws Exception {
		when(txManager.getStatus()).thenReturn(Status.STATUS_ROLLEDBACK);
		assertFalse(txHelper.isTransactionAvailable());
		verify(txManager, atMost(2)).getStatus();
	}
	@Test
	public void isNotActiveTransactionAvailable2() throws Exception {
		when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
		assertFalse(txHelper.isTransactionAvailable());
		verify(txManager, atMost(2)).getStatus();
	}

	@Test
	public void unableToGetStatusInParentHelper() throws Exception {
		when(txManager.getStatus()).thenThrow(new SystemException("dummy error"));
		JMSException e = assertThrows(JMSException.class, txHelper::isTransactionAvailable);
		assertEquals("ARJUNA016121: Failed to get transaction status", e.getMessage());
		assertNotNull(e.getLinkedException());
		assertEquals("dummy error", e.getLinkedException().getMessage());
	}

	@Test // 2nd getStatus() call fails
	public void unableToGetStatusInNarayanaTransactionHelper() throws Exception {
		when(txManager.getStatus()).thenReturn(Status.STATUS_ACTIVE).thenThrow(new SystemException("dummy error"));
		JMSException e = assertThrows(JMSException.class, txHelper::isTransactionAvailable);
		assertEquals("failed to get transaction status", e.getMessage());
		assertNotNull(e.getLinkedException());
		assertEquals("dummy error", e.getLinkedException().getMessage());
	}
}
