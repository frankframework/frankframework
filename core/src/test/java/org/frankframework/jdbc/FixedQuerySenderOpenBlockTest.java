package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.SQLException;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.stream.Message;

public class FixedQuerySenderOpenBlockTest {

	private Connection connection;
	private FixedQuerySender sender;
	private PipeLineSession session;

	@BeforeEach
	public void setup() {
		connection = mock(Connection.class);
		session = new PipeLineSession();
	}

	@AfterEach
	public void tearDown() throws SQLException {
		session.close();
	}

	@Test
	public void testOpenBlockClosesConnectionWhenQueryExecutionContextCreationFails() throws Exception {
		sender = new FixedQuerySender() {

			@Override
			protected @NonNull Connection getConnectionForSendMessage() {
				return connection;
			}

			@Override
			public QueryExecutionContext getQueryExecutionContext(Connection connection, Message message) throws SQLException {
				throw new SQLException("Simulated statement preparation failure");
			}
		};

		assertThrows(SenderException.class, () -> sender.openBlock(session));
		verify(connection).close();
	}

	@Test
	public void testOpenBlockClosesConnectionWhenRuntimeExceptionOccurs() throws Exception {
		sender = new FixedQuerySender() {

			@Override
			protected @NonNull Connection getConnectionForSendMessage() {
				return connection;
			}

			@Override
			public QueryExecutionContext getQueryExecutionContext(Connection connection, Message message) {
				throw new IllegalStateException("Simulated runtime failure");
			}
		};

		assertThrows(SenderException.class, () -> sender.openBlock(session));
		verify(connection).close();
	}
}
