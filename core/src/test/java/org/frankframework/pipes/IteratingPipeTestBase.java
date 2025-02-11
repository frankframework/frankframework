package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.AbstractBlockEnabledSender;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

public abstract class IteratingPipeTestBase<P extends IteratingPipe<String>> extends PipeTestBase<P> {

	protected StringBuilder resultLog = new StringBuilder();

	protected ISender getElementRenderer(boolean blockEnabled) {
		if (blockEnabled) {
			return new BlockEnabledRenderer();
		}
		return new ElementRenderer();
	}

	/** If a line contains the word 'error' an exception will be thrown and the line won't be logged */
	private SenderResult resultCollector(Message message) throws SenderException {
		try {
			if (message.asString().contains("exception")) {
				throw new SenderException("Exception triggered");
			}
			String result = "["+message.asString()+"]";
			resultLog.append(result).append("\n");
			if (message.asString().contains("error")) {
				return new SenderResult(new Message(result), "Error triggered");
			}
			return new SenderResult(result);
		} catch (IOException e) {
			throw new SenderException("unable to parse message", e);
		}
	}

	protected class BlockEnabledRenderer extends AbstractBlockEnabledSender<String> {

		@Override
		public String openBlock(PipeLineSession session) throws SenderException, TimeoutException {
			resultLog.append("openBlock\n");
			return "";
		}

		@Override
		public void closeBlock(String blockHandle, PipeLineSession session) {
			resultLog.append("closeBlock\n");
		}

		@Override
		public SenderResult sendMessage(String blockHandle, Message message, PipeLineSession session) throws SenderException {
			return resultCollector(message);
		}
	}

	protected class ElementRenderer extends EchoSender {
		@Override
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
			return resultCollector(message);
		}
	}

	// Sender Class to find threading issues in parallel execution
	protected class SlowRenderer extends AbstractSenderWithParameters {
		@Override
		@SuppressWarnings("java:S2925")
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
			int random = (int) (Math.random() * 20);
			try {
				Thread.sleep(random);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
			return resultCollector(message);
		}
	}

	private void doPipeWithTenLineInput(String expectedFile) throws Exception {
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile(expectedFile);

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();
		prr.getResult().close();

		assertEquals(expected, actual);
	}

	protected void testTenLines() throws Exception {
		doPipeWithTenLineInput("/IteratingPipe/TenLinesResult.xml");
	}
	protected void testTenLinesToSeven() throws Exception {
		doPipeWithTenLineInput("/IteratingPipe/SevenLinesResult.xml");
	}

	public void testBasic(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		configureAndStartPipe();
		testTenLines();
	}

	@Test
	public void testBasic() throws Exception {
		testBasic(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLog.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testBasicBlockEnabled() throws Exception {
		testBasic(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLogSingleBlock.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}


	public void testBasicMaxItems(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setMaxItems(7);
		configureAndStartPipe();
		testTenLinesToSeven();
	}

	@Test
	public void testBasicMaxItems() throws Exception {
		testBasicMaxItems(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesLog.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testBasicMaxItemsBlockEnabled() throws Exception {
		testBasicMaxItems(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesLogSingleBlock.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}



	public void testFullBlocks(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(5);
		configureAndStartPipe();
		testTenLines();
	}

	@Test
	public void testFullBlocks() throws Exception {
		testFullBlocks(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLog.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testFullBlocksBlockEnabled() throws Exception {
		testFullBlocks(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLogBlocksOfFive.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}


	public void testPartialFinalBlock(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(4);
		configureAndStartPipe();
		testTenLines();
	}

	@Test
	public void testPartialFinalBlock() throws Exception {
		testPartialFinalBlock(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLog.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testPartialFinalBlockBlockEnabled() throws Exception {
		testPartialFinalBlock(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLogBlocksOfFour.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

	private void testPartialFinalBlockMaxItems(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		configureAndStartPipe();

		testTenLinesToSeven();
	}

	@Test
	public void testPartialFinalBlockMaxItems() throws Exception {
		testPartialFinalBlockMaxItems(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesLog.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testPartialFinalBlockMaxItemsBlockEnabled() throws Exception {
		testPartialFinalBlockMaxItems(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesLogBlocksOfFour.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

	@Test
	public void testNullIterator() throws Exception {
		pipe.setSender(getElementRenderer(false));
		configureAndStartPipe();

		String expected = "<results/>";

		PipeRunResult prr = doPipe(new Message(""));
		MatchUtils.assertXmlEquals("null iterator", expected, prr.getResult().asString(), true);
	}

	@Test
	public void testCollectResultsFalseWithExceptionsShouldRethrowException() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setMaxChildThreads(1);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		pipe.setCollectResults(false);
		configureAndStartPipe();

		// Act
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithExceptions.txt");

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, input, session));
		assertTrue(e.getMessage().contains("Exception triggered"));
	}

	@Test
	public void testParallel() throws Exception {
		pipe.setSender(new SlowRenderer());
		pipe.setParallel(true);
		pipe.setMaxChildThreads(4);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		configureAndStartPipe();
		testTenLines();
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLog.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

	@Test
	public void testParallelResultsWithErrors() throws Exception {
		pipe.setSender(new SlowRenderer());
		pipe.setParallel(true);
		pipe.setMaxChildThreads(3);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		pipe.setCollectResults(false);
		configureAndStartPipe();

		// Act
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithErrors.txt");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals("<results count=\"10\"/>", actual);
	}

	@Test
	public void testParallelCollectResultsWithErrors() throws Exception {
		pipe.setSender(new SlowRenderer());
		pipe.setParallel(true);
		pipe.setMaxChildThreads(2);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		pipe.setCollectResults(true);
		configureAndStartPipe();

		// Act
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithErrors.txt");

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, input, session));
		String message = e.getMessage();
		assertEquals("caught exception: an error occurred during parallel execution: [key 2 error]", message.substring(message.length() - 76));
	}

	@Test
	public void testParallelCollectResultsWithIgnoredErrors() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setParallel(true);
		pipe.setMaxChildThreads(1);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		pipe.setCollectResults(true);
		pipe.setIgnoreExceptions(true);
		configureAndStartPipe();

		// Act
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithErrors.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesWithErrorsResult.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);

		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesWithErrorsLog.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

	@Test
	public void testParallelResultsWithExceptions() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setParallel(true);
		pipe.setMaxChildThreads(1);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		pipe.setCollectResults(false);
		configureAndStartPipe();

		// Act
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithExceptions.txt");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals("<results count=\"10\"/>", actual);
	}

	@Test
	public void testParallelCollectResultsWithExceptions() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setParallel(true);
		pipe.setMaxChildThreads(1);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		pipe.setCollectResults(true);
		configureAndStartPipe();

		// Act
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithExceptions.txt");

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, input, session));
		String actual = e.getMessage();
		assertEquals("an error occurred during parallel execution: Exception triggered", actual.substring(actual.length() - 64));
	}

	@Test
	public void testParallelCollectResultsWithIgnoredExceptions() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setParallel(true);
		pipe.setMaxChildThreads(1);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		pipe.setCollectResults(true);
		pipe.setIgnoreExceptions(true);
		configureAndStartPipe();

		// Act
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithExceptions.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesWithExceptionsResult.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);

		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesWithExceptionsLog.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
}
