package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;

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
		sender.start();

		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessageOrThrow(message, session).asString();
		assertEquals(input, result);
	}

	@Test
	void testIfSenderIsClosed() throws Exception {
		// Arrange
		ISender senderMock = Mockito.mock(ISender.class);
		when(senderMock.sendMessage(Mockito.any(Message.class), Mockito.any(PipeLineSession.class))).thenReturn(new SenderResult(Message.nullMessage()));
		sender.setSender(senderMock);
		sender.configure();
		sender.start();

		// Act 1
		sender.sendMessageOrThrow(Message.nullMessage(), session).asString();

		// Assert 1
		verify(senderMock, Mockito.times(2)).start();
		verify(senderMock, Mockito.times(1)).stop();

		// Act 2: now close the session too
		session.close();

		// Assert 2: only now the sender should be closed
		verify(senderMock, Mockito.times(2)).stop();
	}

	private static class TestOpenAndConfigureSender extends AbstractSenderWithParameters {
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
		public void start() {
			if (!opened.compareAndSet(false, true)) {
				throw new LifecycleException("not yet opened");
			}
			if (!configured.getAcquire()) {
				throw new LifecycleException("not configured");
			}
		}

		@Override
		public void stop() {
			if (!opened.compareAndSet(true, false)) {
				throw new LifecycleException("already closed");
			}
		}

		@Override
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
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
