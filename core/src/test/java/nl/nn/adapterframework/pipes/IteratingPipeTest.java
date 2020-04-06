package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.BlockEnabledSenderBase;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.ReaderLineIterator;

public class IteratingPipeTest extends PipeTestBase<IteratingPipe<String>> {

	private final class TestIteratingPipe extends IteratingPipe<String> {

		public boolean useIterator=true;
		
		@Override
		protected IDataIterator<String> getIterator(Message input, IPipeLineSession session, Map<String, Object> threadContext) throws SenderException {
			try {
				return useIterator ? new ReaderLineIterator(input.asReader()) : null;
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}

		@Override
		protected void iterateOverInput(Message input, IPipeLineSession session, Map<String, Object> threadContext, IteratingPipe<String>.ItemCallback callback) throws SenderException, TimeOutException {
			if (useIterator) {
				super.iterateOverInput(input, session, threadContext, callback);
			} else {
				try {
					for (IDataIterator<String> iterator = new ReaderLineIterator(input.asReader()); iterator.hasNext();) {
						callback.handleItem(iterator.next());
					}
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}
		}

	}

	@Override
	public IteratingPipe<String> createPipe() {
		return new TestIteratingPipe();
	}
	
	protected StringBuffer resultLog;


	protected ISender getElementRenderer(boolean blockEnabled) {
		resultLog = new StringBuffer();
		if (blockEnabled) {
			return new BlockEnabledRenderer();
		}
		return getElementRenderer(null);
	}

	private class BlockEnabledRenderer extends BlockEnabledSenderBase<String> {

		@Override
		public String openBlock(IPipeLineSession session) throws SenderException, TimeOutException {
			resultLog.append("openBlock\n");
			return "";
		}

		@Override
		public void closeBlock(String blockHandle, IPipeLineSession session) throws SenderException {
			resultLog.append("closeBlock\n");
		}

		@Override
		public Message sendMessage(String blockHandle, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
			try {
				String result = "["+message.asString()+"]";
				resultLog.append(result+"\n");
				return new Message(result);
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}
		
	}
	
	protected ISender getElementRenderer(final Exception e) {
		EchoSender sender = new EchoSender() {

			@Override
			public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
				try {
					if (message.asString().contains("error")) {
						throw new SenderException("Exception triggered", e);
					}
					String result = "["+message.asString()+"]";
					resultLog.append(result+"\n");
					return new Message(result);
				} catch (IOException e) {
					throw new SenderException(getLogPrefix(),e);
				}
			}

		};
		return sender;
	}
	

	public void testTenLines(boolean noIterator, String expectedFile) throws Exception {
		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile(expectedFile);

		((TestIteratingPipe)pipe).useIterator=!noIterator;
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	public void testTenLines(boolean noIterator) throws Exception {
		testTenLines(noIterator, "/IteratingPipe/TenLinesResult.xml");
	}
	public void testTenLinesToSeven(boolean noIterator) throws Exception {
		testTenLines(noIterator, "/IteratingPipe/SevenLinesResult.xml");
	}
	
	public void testBasic(boolean blockEnabled, boolean noIterator) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		configurePipe();
		pipe.start();
		testTenLines(noIterator);
	}

	public void testBasic(boolean noIterator) throws Exception {
		testBasic(false, noIterator);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	public void testBasicBlockEnabled(boolean noIterator) throws Exception {
		testBasic(true, noIterator);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogSingleBlock.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

	@Test
	public void testBasic() throws Exception {
		testBasic(false);
	}
	@Test
	public void testBasicBlockEnabled() throws Exception {
		testBasic(false);
	}
	@Test
	public void testBasicNoIterator() throws Exception {
		testBasic(true);
	}
	@Test
	public void testBasicBlockEnabledNoIterator() throws Exception {
		testBasic(true);
	}

	public void testBasicMaxItems(boolean blockEnabled, boolean noIterator) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setMaxItems(7);
		configurePipe();
		pipe.start();
		testTenLinesToSeven(noIterator);
	}
	
	public void testBasicMaxItems(boolean noIterator) throws Exception {
		testBasicMaxItems(false, noIterator);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	public void testBasicMaxItemsBlockEnabled(boolean noIterator) throws Exception {
		testBasicMaxItems(true, noIterator);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenResultLogSingleBlock.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

	@Test
	public void testBasicMaxItems() throws Exception {
		testBasic(false);
	}
	@Test
	public void testBasicMaxItemsBlockEnabled() throws Exception {
		testBasic(false);
	}
	@Test
	public void testBasicMaxItemsNoIterator() throws Exception {
		testBasic(true);
	}
	@Test
	public void testBasicMaxItemsBlockEnabledNoIterator() throws Exception {
		testBasic(true);
	}


	public void testFullBlocks(boolean blockEnabled, boolean noIterator) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(5);
		configurePipe();
		pipe.start();
		testTenLines(noIterator);
	}

	public void testFullBlocks(boolean noIterator) throws Exception {
		testFullBlocks(false, noIterator);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	public void testFullBlocksBlockEnabled(boolean noIterator) throws Exception {
		testFullBlocks(true, noIterator);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogBlocksOfFive.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

	@Test
	public void testFullBlocks() throws Exception {
		testFullBlocks(false);
	}
	@Test
	public void testFullBlocksBlockEnabled() throws Exception {
		testFullBlocksBlockEnabled(false);
	}
	@Test
	public void testFullBlocksNoIterator() throws Exception {
		testFullBlocks(true);
	}
	@Test
	public void testFullBlocksBlockEnabledNoIterator() throws Exception {
		testFullBlocksBlockEnabled(true);
	}
	
	
	public void testPartialFinalBlock(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(4);
		configurePipe();
		pipe.start();
		testTenLines(false);
	}

	@Test
	public void testPartialFinalBlock() throws Exception {
		testPartialFinalBlock(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testPartialFinalBlockBlockEnabled() throws Exception {
		testPartialFinalBlock(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogBlocksOfFour.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	
	public void testPartialFinalBlockMaxItems(boolean blockEnabled) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		configurePipe();
		pipe.start();
		testTenLinesToSeven(false);
	}

	@Test
	public void testPartialFinalBlockMaxItems() throws Exception {
		testPartialFinalBlockMaxItems(false);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testPartialFinalBlockMaxItemsBlockEnabled() throws Exception {
		testPartialFinalBlockMaxItems(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultLogBlocksOfFour.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}

}
