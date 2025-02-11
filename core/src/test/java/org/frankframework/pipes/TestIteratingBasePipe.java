package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ConcurrencyThrottleSupport;
import org.springframework.util.StringUtils;

import org.frankframework.core.IDataIterator;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.pipes.TestIteratingBasePipe.IteratingTestPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.ReaderLineIterator;
import org.frankframework.util.SpringUtils;

public class TestIteratingBasePipe extends IteratingPipeTestBase<IteratingTestPipe> {

	final class IteratingTestPipe extends IteratingPipe<String> {

		@Override
		protected IDataIterator<String> getIterator(Message input, PipeLineSession session, Map<String, Object> threadContext) throws SenderException {
			try {
				if (input.isEmpty()) {
					return null;
				}
				return new ReaderLineIterator(input.asReader());
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}
	}

	@Override
	public IteratingTestPipe createPipe() {
		return new IteratingTestPipe();
	}

	@Test
	public void testParallelWithItemNoSessionKey() throws Exception {
		pipe.setSender(new IndexAwareSlowRenderer());
		pipe.setParallel(true);
		pipe.setMaxChildThreads(10);
		pipe.setTaskExecutor(createTaskExecutor());
		pipe.setItemNoSessionKey("index");

		configureAndStartPipe();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/HundredLines.txt");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();
		prr.getResult().close();

		String expected = TestFileUtils.getTestFile("/IteratingPipe/HundredLines.xml");
		assertEquals(expected, actual);

		String resultLogTrimmed = resultLog.toString().trim();
		String expectedLog = TestFileUtils.getTestFile("/IteratingPipe/HundredLinesLog.txt");
		assertNotEquals(expectedLog, resultLogTrimmed); // Ensure it's not in chronological order
		int occurance = StringUtils.countOccurrencesOf(resultLogTrimmed, "\n");
		assertEquals(99, occurance);
	}

	protected TaskExecutor createTaskExecutor() {
		SimpleAsyncTaskExecutor executor = SpringUtils.createBean(getConfiguration(), SimpleAsyncTaskExecutor.class);
		executor.setConcurrencyLimit(ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY);
		return executor;
	}

	private class IndexAwareSlowRenderer extends SlowRenderer {
		@Override
		public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException {
			SenderResult result = super.sendMessage(message, session);
			Integer index = session.getInteger("index");
			if (index == null) {
				fail("no index");
			}
			try {
				String msg = message.asString();
				String number = msg.substring(0, msg.indexOf(" ="));
				assertEquals(""+index, number);
			} catch (IOException e) {
				throw new SenderException(e);
			}
			return result;
		}
	}
}
