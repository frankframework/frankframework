package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

public class SenderSeriesTest extends SenderTestBase<SenderSeries> {

	private static final String BASEPATH = "/Senders/SenderSeries/";

	@Override
	public SenderSeries createSender() throws Exception {
		return new SenderSeries();
	}

	protected String getExpectedTestFile(String path) throws IOException {
		return TestFileUtils.getTestFile(BASEPATH+path);
	}

	@Test
	public void test0SubSenders() throws Exception {
		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertEquals("must have at least a sender configured", e.getMessage());
	}

	@Test
	public void test1SubSenders() throws Exception {
		sender.addSender(new TestSender("Sender0"));

		sender.configure();
		sender.start();

		SenderResult result = sender.sendMessage(new Message("input+"), session);
		assertTrue(result.isSuccess());
		assertEquals("input+,0", result.getResult().asString());
	}

	@Test
	public void test10SubSenders() throws Exception {
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
			sender.addSender(new TestSender("Sender"+i));
		}

		sender.configure();
		sender.start();

		SenderResult result = sender.sendMessage(new Message("input+"), session);
		assertTrue(result.isSuccess());
		assertEquals("input+,0,1,2,3,4,5,6,7,8,9", result.getResult().asString());

		long duration = System.currentTimeMillis() - startTime;
		long maxDuration = 1000;
		assertTrue(duration < maxDuration, "Test took ["+duration+"]s, maxDuration ["+maxDuration+"]s");
	}

	@Test
	public void testExceptionHandling() throws Exception {
		for (int i = 0; i < 10; i++) {
			if (i == 5) {
				sender.addSender(new ExceptionThrowingSender());
			} else {
				sender.addSender(new TestSender("Sender"+i));
			}
		}

		AtomicInteger calcSize = new AtomicInteger();
		sender.getSenders().forEach(s -> calcSize.incrementAndGet());
		assertEquals(10, calcSize.get());

		sender.configure();
		sender.start();

		SenderException result = assertThrows(SenderException.class, () -> sender.sendMessage(new Message("input+"), session));
		assertEquals("fakeException", result.getMessage());
	}

	@Test
	public void testErrorHandling() throws Exception {
		for (int i = 0; i < 10; i++) {
			if (i == 5) {
				sender.addSender(new ErrorReturningSender());
			} else {
				sender.addSender(new TestSender("Sender"+i));
			}
		}

		AtomicInteger calcSize = new AtomicInteger();
		sender.getSenders().forEach(s -> calcSize.incrementAndGet());
		assertEquals(10, calcSize.get());

		sender.configure();
		sender.start();

		SenderResult result = sender.sendMessage(new Message("input+"), session);
		assertFalse(result.isSuccess());
		assertEquals("input+,0,1,2,3,4", result.getResult().asString());
	}

	private static class TestSender extends AbstractSenderWithParameters {
		private String shortname;
		public TestSender(String name) {
			setName(name);
			shortname = name.substring(6);
		}

		@Override
		public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException {
			try {
				String input = message.asString() + "," + shortname;
				return new SenderResult(input);
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}
	}

	private static class ErrorReturningSender extends AbstractSender {
		@Override
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
			return new SenderResult(false, message, "fakeError", "fakeError");
		}
	}

	private static class ExceptionThrowingSender extends AbstractSender {
		@Override
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
			throw new SenderException("fakeException");
		}
	}
}
