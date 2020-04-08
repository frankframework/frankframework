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

		@Override
		protected IDataIterator<String> getIterator(Message input, IPipeLineSession session, Map<String, Object> threadContext) throws SenderException {
			try {
				return new ReaderLineIterator(input.asReader());
			} catch (IOException e) {
				throw new SenderException(e);
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
	

	public void testTenLines(String expectedFile) throws Exception {
		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
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
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testBasicBlockEnabled() throws Exception {
		testBasic(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogSingleBlock.txt");
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
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testBasicMaxItemsBlockEnabled() throws Exception {
		testBasicMaxItems(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultLogSingleBlock.txt");
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
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogPlain.txt");
		assertEquals(expectedRenderResult, resultLog.toString().trim());
	}
	@Test
	public void testFullBlocksBlockEnabled() throws Exception {
		testFullBlocks(true);
		String expectedRenderResult = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultLogBlocksOfFive.txt");
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
		testTenLinesToSeven();
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
