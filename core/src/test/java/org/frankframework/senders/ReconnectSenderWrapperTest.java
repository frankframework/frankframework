package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.stream.Message;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReconnectSenderWrapperTest extends SenderTestBase<ReconnectSenderWrapper> {

	@Override
	public ReconnectSenderWrapper createSender() {
		ReconnectSenderWrapper senderWrapper = new ReconnectSenderWrapper();
		TestOpenAndConfigureSender testSender = new TestOpenAndConfigureSender();
		senderWrapper.setSender(testSender);
		return senderWrapper;
	}

	@Test
	void basic() throws Exception {
		sender.configure();
		sender.open();

		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessageOrThrow(message, session).asString();
		assertEquals(input, result);
		assertInstanceOf(ReconnectSenderWrapper.AutoCloseableSenderWrapper.class, session.get(ReconnectSenderWrapper.EMBEDDED_SENDER_KEY));
	}

	@Test
	void testIfSenderIsClosed() throws Exception {
		// Arrange
		ISender senderMock = Mockito.mock(ISender.class);
		when(senderMock.sendMessage(Mockito.any(Message.class), Mockito.any(PipeLineSession.class))).thenReturn(new SenderResult(Message.nullMessage()));
		sender.setSender(senderMock);
		sender.configure();
		sender.open();

		// Act
		sender.sendMessageOrThrow(Message.nullMessage(), session).asString();

		// Assert
		verify(senderMock, Mockito.times(2)).open();
		verify(senderMock, Mockito.times(1)).close();
	}

	private static class TestOpenAndConfigureSender extends SenderWithParametersBase {
		private final AtomicBoolean opened = new AtomicBoolean(false);
		private final AtomicBoolean configured = new AtomicBoolean(false);

		@Override
		public void configure() throws ConfigurationException {
			setName(TestOpenAndConfigureSender.class.getSimpleName());
			if (!configured.compareAndSet(false, true)) {
				throw new ConfigurationException("should only be configured once");
			}
			super.configure();
		}

		@Override
		public void open() throws SenderException {
			if (!opened.compareAndSet(false, true)) {
				throw new SenderException("not yet opened");
			}
			if (!configured.getAcquire()) {
				throw new SenderException("not configured");
			}
		}

		@Override
		public void close() throws SenderException {
			if (!opened.compareAndSet(true, false)) {
				throw new SenderException("already closed");
			}
		}

		@Override
		public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException {
			if (opened.getAcquire()) {
				if (!configured.getAcquire()) {
					throw new IllegalStateException("not configured");
				}

				return new SenderResult(message);
			}
			throw new SenderException("not opened");
		}
	}
}
