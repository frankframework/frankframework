package org.frankframework.jdbc.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.core.IbisException;

public class JdbcMigrationExceptionTest {

	@Test
	public void testJdbcException() {
		IbisException ex = new JdbcMigrationException();
		assertEquals("no message in exception: org.frankframework.jdbc.migration.JdbcMigrationException", ex.getMessage());
	}

	@Test
	public void jdbcExceptionMessageTest() {
		IbisException ex = new JdbcMigrationException("test");
		assertEquals("test", ex.getMessage());
	}

	@Test
	public void jdbcExceptionMessageAndCauseTest() {
		IllegalStateException cause = new IllegalStateException("cause-message");
		IbisException ex = new JdbcMigrationException("test", cause);
		assertEquals("test: (IllegalStateException) cause-message", ex.getMessage());
	}

	@Test
	public void jdbcExceptionCauseTest() {
		IllegalStateException cause = new IllegalStateException("cause-message");
		IbisException ex = new JdbcMigrationException(cause);
		assertEquals("(IllegalStateException) cause-message", ex.getMessage());
	}

}
