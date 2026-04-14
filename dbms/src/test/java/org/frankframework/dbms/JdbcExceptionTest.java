package org.frankframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.core.IbisException;

public class JdbcExceptionTest {

	@Test
	public void testJdbcException() {
		IbisException ex = new JdbcException();
		assertEquals("no message in exception: org.frankframework.dbms.JdbcException", ex.getMessage());
	}

	@Test
	public void jdbcExceptionMessageTest() {
		IbisException ex = new JdbcException("test");
		assertEquals("test", ex.getMessage());
	}

	@Test
	public void jdbcExceptionMessageAndCauseTest() {
		IllegalStateException cause = new IllegalStateException("cause-message");
		IbisException ex = new JdbcException("test", cause);
		assertEquals("test: (IllegalStateException) cause-message", ex.getMessage());
	}

	@Test
	public void jdbcExceptionCauseTest() {
		IllegalStateException cause = new IllegalStateException("cause-message");
		IbisException ex = new JdbcException(cause);
		assertEquals("(IllegalStateException) cause-message", ex.getMessage());
	}

}
