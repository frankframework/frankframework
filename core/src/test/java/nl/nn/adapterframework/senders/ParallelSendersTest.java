package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

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

	private class DelaySender extends EchoSender {
		public DelaySender(String name) {
			setName(name);
		}

		@Override
		public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				fail("delay interrupted");
			}
			return super.sendMessage(message, session);
		}
	}

	@Test
	public void basic3SubSenders() throws Exception {
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
			sender.registerSender(new DelaySender("Sender"+i));
		}

		sender.configure();
		sender.open();

		String expected = TestFileUtils.getTestFile("/ParallelSendersTestResult.txt");
		assertNotNull("cannot find expected result file", expected);

		Message message = new Message("<dummy/>");
		String result = sender.sendMessage(message, session).asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);

		long duration = System.currentTimeMillis() - startTime;
		System.err.println(duration);
		assertTrue(duration < DELAY + 1000);
	}
}
