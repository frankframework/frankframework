package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

@Ignore
public class ParallelSendersTest extends SenderTestBase<ParallelSenders> {

	private static final int DELAY = 2000;
	private ThreadPoolTaskExecutor executor = null;
	protected TaskExecutor getTaskExecutor() {
		if(executor == null) {
			executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(10);
			executor.initialize();
		}
		return executor;
	}

	@Override
	public ParallelSenders createSender() throws Exception {
		ParallelSenders ps = new ParallelSenders() {
			@Override
			protected TaskExecutor createTaskExecutor() {
				return getTaskExecutor();
			}
		};
		return ps;
	}

	private class TestSender extends DelaySender {
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

		String expected = TestFileUtils.getTestFile("/Senders/ParallelSenders/test10SubSenders.txt");
		assertNotNull("cannot find expected result file", expected);

		Message message = new Message("<dummy/>");
		String result = sender.sendMessage(message, session).asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);

		long duration = System.currentTimeMillis() - startTime;
		System.err.println(duration);
		assertTrue(duration < DELAY + 1000);
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

		String expected = TestFileUtils.getTestFile("/Senders/ParallelSenders/test5wrappersWith10SubSenders.txt");
		assertNotNull("cannot find expected result file", expected);

		Message message = new Message("<dummy/>");
		String result = sender.sendMessage(message, session).asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);

		long duration = System.currentTimeMillis() - startTime;
		System.err.println(duration);
		int maxDuration = (DELAY * amountOfDelaySendersInWrapper) + 1000;
		assertTrue("Test took ["+duration+"]s, maxDuration ["+maxDuration+"]s", duration < maxDuration);
	}
}
