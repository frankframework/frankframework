package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.junit.jupiter.api.Test;

public class RuntimeSenderWrapperTest extends SenderTestBase<RuntimeSenderWrapper> {

	@Override
	public RuntimeSenderWrapper createSender() {
		RuntimeSenderWrapper senderWrapper = new RuntimeSenderWrapper();
		TestOpenAndConfigureSender testSender = new TestOpenAndConfigureSender();
		senderWrapper.setSender(testSender);
		return senderWrapper;
	}

	@Test
	public void basic() throws Exception {
		sender.configure();
		sender.open();

		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessageOrThrow(message, session).asString();
		assertEquals(input, result);
	}

	@Test
	public void basic2() throws Exception {
		sender.configure();
		sender.open();

		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessageOrThrow(message, session).asString();
		assertEquals(input, result);
	}

	private static class TestOpenAndConfigureSender extends SenderWithParametersBase {
		private final AtomicBoolean opened = new AtomicBoolean(false);

		@Override
		public void open() throws SenderException {
			if(!opened.compareAndSet(false, true)) {
				throw new IllegalStateException("not yet opened");
			}
		}

		@Override
		public void close() throws SenderException {
			if(!opened.compareAndSet(true, false)) {
				throw new IllegalStateException("already closed");
			}
		}

		@Override
		public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
			if(opened.getAcquire()) {
				return new SenderResult(message);
			}
			throw new SenderException("not opened");
		}
	}
}
