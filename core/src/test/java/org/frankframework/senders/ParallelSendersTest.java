package org.frankframework.senders;

import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import jakarta.annotation.Nonnull;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

public class ParallelSendersTest extends SenderTestBase<ParallelSenders> {

	private static final String BASEPATH = "/Senders/ParallelSenders/";
	protected static final long DELAY_MILLIS = 500L;

	@Override
	public ParallelSenders createSender() throws Exception {
		return new ParallelSenders();
	}

	protected String getExpectedTestFile(String path) throws IOException {
		return TestFileUtils.getTestFile(BASEPATH+path);
	}

	protected static class TestSender extends DelaySender {
		public TestSender(String name) {
			setName(name);
			setDelayTime(DELAY_MILLIS);
		}
	}

	@Test
	public void test10SubSenders() throws Exception {
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
			sender.registerSender(new TestSender("Sender"+i));
		}

		sender.configure();
		sender.open();

		String expected = getExpectedTestFile("test10SubSenders.txt");
		assertNotNull(expected, "cannot find expected result file");

		Message message = new Message("<dummy/>");
		String result = sender.sendMessageOrThrow(message, session).asString();
		assertEqualsIgnoreCRLF(expected, result);

		long duration = System.currentTimeMillis() - startTime;
		long maxDuration = DELAY_MILLIS + 1000;
		assertTrue(duration < maxDuration, "Test took ["+duration+"]s, maxDuration ["+maxDuration+"]s");
	}

	@Test
	public void test10SubSendersNonRepeatableMessage() throws Exception {
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
			sender.registerSender(new TestSender("Sender"+i));
		}

		sender.configure();
		sender.open();

		String expected = getExpectedTestFile("test10SubSendersNonRepeatableMessage.txt");
		assertNotNull(expected, "cannot find expected result file");

		Message message = MessageTestUtils.getNonRepeatableMessage(MessageTestUtils.MessageType.CHARACTER_UTF8);
		String result = sender.sendMessageOrThrow(message, session).asString();
		assertEqualsIgnoreCRLF(expected, result);

		long duration = System.currentTimeMillis() - startTime;
		long maxDuration = DELAY_MILLIS + 1000;
		assertTrue(duration < maxDuration, "Test took ["+duration+"]s, maxDuration ["+maxDuration+"]s");
	}

	@Test
	public void test5wrappersWith10SubSenders() throws Exception {
		long startTime = System.currentTimeMillis();

		int amountOfDelaySendersInWrapper = 5;
		for (int i = 0; i < 10; i++) {
			SenderSeries wrapper = getConfiguration().createBean(SenderSeries.class);
			wrapper.setName("Wrapper"+i);
			for (int j = 0; j < amountOfDelaySendersInWrapper; j++) {
				wrapper.registerSender(new TestSender("Wrapper"+i+"-Sender"+j));
			}
			sender.registerSender(wrapper);
		}

		sender.configure();
		sender.open();

		String expected = getExpectedTestFile("test5wrappersWith10SubSenders.txt");
		assertNotNull(expected, "cannot find expected result file");

		Message message = new Message("<dummy/>");
		String result = sender.sendMessageOrThrow(message, session).asString();
		assertEqualsIgnoreCRLF(expected, result);

		long duration = System.currentTimeMillis() - startTime;
		long maxDuration = (DELAY_MILLIS * amountOfDelaySendersInWrapper) + 1000;
		assertTrue(duration < maxDuration, "Test took ["+duration+"]s, maxDuration ["+maxDuration+"]s");
	}

	@Test
	public void testSingleExceptionHandling() throws Exception {
		sender.registerSender(new ExceptionThrowingSender());
		sender.configure();
		sender.open();

		SenderResult result = sender.sendMessage(new Message("fakeInput"), session);

		assertFalse(result.isSuccess());
		assertThat(result.getResult().asString(), containsString("<result senderClass=\"ExceptionThrowingSender\" type=\"SenderException\" success=\"false\">fakeException</result>"));
	}

	@Test
	public void testExceptionHandling() throws Exception {
		sender.registerSender(new EchoSender());
		sender.registerSender(new ExceptionThrowingSender());
		sender.registerSender(new EchoSender());

		sender.configure();
		sender.open();

		SenderResult result = sender.sendMessage(new Message("fakeInput"), session);

		assertFalse(result.isSuccess());
	}

	private static class ExceptionThrowingSender extends SenderBase {
		@Override
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
			throw new SenderException("fakeException");
		}
	}
}
