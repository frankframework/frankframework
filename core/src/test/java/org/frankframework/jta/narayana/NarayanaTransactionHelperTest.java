package org.frankframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.JMSException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.jboss.narayana.jta.jms.TransactionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class NarayanaTransactionHelperTest {

	private TransactionManager txManager;
	private TransactionHelper txHelper;

	@BeforeEach
	public void setup() {
		txManager = mock(TransactionManager.class);
		txHelper = new NarayanaTransactionHelper(txManager);
	}

	@ParameterizedTest
	@ValueSource(ints = {Status.STATUS_ACTIVE, Status.STATUS_PREPARING, Status.STATUS_MARKED_ROLLBACK})
	public void isActiveTransactionAvailable(int status) throws Exception {
		when(txManager.getStatus()).thenReturn(status);
		assertTrue(txHelper.isTransactionAvailable());
		verify(txManager, times(2)).getStatus();
	}

	@ParameterizedTest
	@ValueSource(ints = {Status.STATUS_ROLLEDBACK, Status.STATUS_NO_TRANSACTION})
	public void isNotActiveTransactionAvailable(int status) throws Exception {
		when(txManager.getStatus()).thenReturn(status);
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
