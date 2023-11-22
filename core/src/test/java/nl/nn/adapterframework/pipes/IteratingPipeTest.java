package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.BlockEnabledSenderBase;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.ReaderLineIterator;

public class IteratingPipeTest<P extends IteratingPipe<String>> extends PipeTestBase<P> {

	protected StringBuilder resultLog;

	private final class TestIteratingPipe extends IteratingPipe<String> {

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
	public P createPipe() {
		return (P)new TestIteratingPipe();
	}

	protected ISender getElementRenderer(boolean blockEnabled) {
		resultLog = new StringBuilder();
		if (blockEnabled) {
			return new BlockEnabledRenderer();
		}
		return getElementRenderer(null);
	}

	protected class BlockEnabledRenderer extends BlockEnabledSenderBase<String> {

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
			try {
				String result = "["+message.asString()+"]";
				resultLog.append(result+"\n");
				return new SenderResult(result);
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}
	}

	protected ISender getElementRenderer(final Exception e) {
		EchoSender sender = new EchoSender() {

			@Override
			public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
				try {
					if (message.asString().contains("error")) {
						throw new SenderException("Exception triggered", e);
					}
					String result = "["+message.asString()+"]";
					resultLog.append(result+"\n");
					return new SenderResult(result);
				} catch (IOException e) {
					throw new SenderException(getLogPrefix(),e);
				}
			}

		};
		return sender;
	}

	public void testTenLines(String expectedFile) throws Exception {
		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile(expectedFile);

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	public void testTenLines() throws Exception {
		testTenLines("/IteratingPipe/TenLinesResult.xml");
	}
	public void testTenLinesToSeven() throws Exception {
		testTenLines("/IteratingPipe/SevenLinesResult.xml");
	}

	public void testBasic(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		configurePipe();
		pipe.start();
		testTenLines();
	}

	@Test
	public void testBasic() throws Exception {
		testBasic(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLogPlain.txt");
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
		configurePipe();
		pipe.start();
		testTenLinesToSeven();
	}

	@Test
	public void testBasicMaxItems() throws Exception {
		testBasicMaxItems(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesLogPlain.txt");
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
		configurePipe();
		pipe.start();
		testTenLines();
	}

	@Test
	public void testFullBlocks() throws Exception {
		testFullBlocks(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLogPlain.txt");
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
		configurePipe();
		pipe.start();
		testTenLines();
	}

	@Test
	public void testPartialFinalBlock() throws Exception {
		testPartialFinalBlock(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testPartialFinalBlockBlockEnabled() throws Exception {
		testPartialFinalBlock(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLogBlocksOfFour.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

	public void testPartialFinalBlockMaxItems(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		configurePipe();
		pipe.start();
		testTenLinesToSeven();
	}

	@Test
	public void testPartialFinalBlockMaxItems() throws Exception {
		testPartialFinalBlockMaxItems(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesLogPlain.txt");
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
		configurePipe();
		pipe.start();

		String expected = "<results/>";

		PipeRunResult prr = doPipe(new Message(""));
		MatchUtils.assertXmlEquals("null iterator", expected, prr.getResult().asString(), true);
	}

	@Test
	public void testParallel() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setParallel(true);
		pipe.setMaxChildThreads(1);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		configurePipe();
		pipe.start();
		testTenLines();
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

}
