package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class ParallelSendersTest extends SenderTestBase<ParallelSenders> {

	private static final String BASEPATH = "/Senders/ParallelSenders/";
	private static final int DELAY = 2000;

	@Override
	public ParallelSenders createSender() throws Exception {
		ParallelSenders ps = new ParallelSenders();
		return ps;
	}

	protected String getExpectedTestFile(String path) throws IOException {
		return TestFileUtils.getTestFile(BASEPATH+path);
	}

	protected static class TestSender extends DelaySender {
		public TestSender(String name) {
			setName(name);
			setDelayTime(DELAY);
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
		assertNotNull("cannot find expected result file", expected);

		Message message = new Message("<dummy/>");
		String result = sender.sendMessage(message, session).asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);

		long duration = System.currentTimeMillis() - startTime;
		int maxDuration = DELAY + 1000;
		assertTrue("Test took ["+duration+"]s, maxDuration ["+maxDuration+"]s", duration < maxDuration);
	}

	@Test
	public void test5wrappersWith10SubSenders() throws Exception {
		long startTime = System.currentTimeMillis();

		int amountOfDelaySendersInWrapper = 5;
		for (int i = 0; i < 10; i++) {
			SenderSeries wrapper = new SenderSeries();
			wrapper.setName("Wrapper"+i);
			for (int j = 0; j < amountOfDelaySendersInWrapper; j++) {
				wrapper.registerSender(new TestSender("Wrapper"+i+"-Sender"+j));
			}
			sender.registerSender(wrapper);
		}

		sender.configure();
		sender.open();

		String expected = getExpectedTestFile("test5wrappersWith10SubSenders.txt");
		assertNotNull("cannot find expected result file", expected);

		Message message = new Message("<dummy/>");
		String result = sender.sendMessage(message, session).asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);

		long duration = System.currentTimeMillis() - startTime;
		int maxDuration = (DELAY * amountOfDelaySendersInWrapper) + 1000;
		assertTrue("Test took ["+duration+"]s, maxDuration ["+maxDuration+"]s", duration < maxDuration);
	}
}
