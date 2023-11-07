package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.pipes.IteratingPipe.StopReason;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class StreamLineIteratorPipeTest extends IteratingPipeTest<StreamLineIteratorPipe> {

	@Override
	public StreamLineIteratorPipe createPipe() {
		StreamLineIteratorPipe result = new StreamLineIteratorPipe();
		result.setCombineBlocks(false); // default is true, but false is compatible with super test class IteratingPipeTest
		return result;
	}

	public void testBasicWithLinePrefixAndSuffix(boolean blockEnabled, boolean combinedBlocks, String expectedLogFile) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(combinedBlocks);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultWithLineFixes.xml");
		String expectedLog = TestFileUtils.getTestFile(expectedLogFile);

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedLogFile, expectedLog, resultLog.toString().trim());
		assertEquals(expected, actual);
	}

	@Test
	public void testBasicWithLinePrefixAndSuffix() throws Exception {
		testBasicWithLinePrefixAndSuffix(false, false, "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}
	@Test
	public void testBasicWithLinePrefixAndSuffixCombined() throws Exception {
		testBasicWithLinePrefixAndSuffix(false, true, "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}

	@Test
	public void testBasicWithLinePrefixAndSuffixBlockEnabled() throws Exception {
		testBasicWithLinePrefixAndSuffix(true, false, "/IteratingPipe/TenLinesLogPlainWithLineFixesBlockEnabled.txt");
	}

	@Test
	public void testBasicWithLinePrefixAndSuffixBlockEnabledCombined() throws Exception {
		testBasicWithLinePrefixAndSuffix(true, true, "/IteratingPipe/TenLinesLogPlainWithLineFixesBlockEnabled.txt");
	}


	public void testBlocksWithCombine(boolean blockEnabled, boolean combinedBlocks, int blockSize, String expectedFile, String expectedLogFile) throws Exception {
		pipe.setSender(getElementRenderer(blockEnabled));
		pipe.setBlockSize(blockSize);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(combinedBlocks);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile(expectedFile);
		String expectedLog = TestFileUtils.getTestFile(expectedLogFile);

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedLogFile, expectedLog, resultLog.toString().trim());
		assertEquals(expectedFile, expected, actual);
	}

	@Test
	public void testFullBlocksWithCombineOff() throws Exception {
		testBlocksWithCombine(false, false, 5, "/IteratingPipe/TenLinesResultWithLineFixes.xml", "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}
	@Test
	public void testFullBlocksWithCombineOn() throws Exception {
		testBlocksWithCombine(false, true, 5, "/IteratingPipe/TenLinesResultCombinedInBlocksOfFiveWithLineFixes.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfFiveWithLineFixes.txt");
	}
	@Test
	public void testFullBlocksWithCombineOffBlockEnabled() throws Exception {
		testBlocksWithCombine(true, false, 5, "/IteratingPipe/TenLinesResultWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogInBlocksOfFiveWithLineFixesBlockEnabled.txt");
	}
	@Test
	public void testFullBlocksWithCombineOnBlockEnabled() throws Exception {
		testBlocksWithCombine(true, true, 5, "/IteratingPipe/TenLinesResultCombinedInBlocksOfFiveWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfFiveWithLineFixesBlockEnabled.txt");
	}

	@Test
	public void testBlocksOf1WithCombineOff() throws Exception {
		testBlocksWithCombine(false, false, 1, "/IteratingPipe/TenLinesResultWithLineFixes.xml", "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}
	@Test
	public void testBlocksOf1WithCombineOn() throws Exception {
		testBlocksWithCombine(false, true, 1, "/IteratingPipe/TenLinesResultCombinedInBlocksOfOneWithLineFixes.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfOneWithLineFixes.txt");
	}

	@Test
	public void testBlocksOf1WithCombineOffBlockEnabled() throws Exception {
		testBlocksWithCombine(true, false, 1, "/IteratingPipe/TenLinesResultWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogInBlocksOfOneWithLineFixesBlockEnabled.txt");
	}
	@Test
	public void testBlocksOf1WithCombineOnBlockEnabled() throws Exception {
		testBlocksWithCombine(true, true, 1, "/IteratingPipe/TenLinesResultCombinedInBlocksOfOneWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfOneWithLineFixesBlockEnabled.txt");
	}

	@Test
	public void testPartialFinalBlockWithLinePrefixAndSuffix() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setBlockSize(4);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultInBlocksOfFour.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testPartialFinalBlockMaxItemsWithLinePrefixAndSuffix() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultInBlocksOfFour.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testMaxItemsWithSpecialForward() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		pipe.registerForward(new PipeForward(StopReason.MAX_ITEMS_REACHED.getForwardName(),"dummy"));
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultInBlocksOfFour.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(StopReason.MAX_ITEMS_REACHED.getForwardName(), prr.getPipeForward().getName());
		assertEquals(expected, actual);
	}

	@Test
	public void testMaxItemsReachedWithoutSpecialForwardRegistered() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultInBlocksOfFour.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
		assertEquals(expected, actual);
	}

	@Test
	public void testBlocksByKey() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setStartPosition(4);
		pipe.setEndPosition(5);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultInKeyBlocks.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testBlocksByKeyWithStopConditionXpath() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setStartPosition(4);
		pipe.setEndPosition(5);
		pipe.registerForward(new PipeForward(StopReason.STOP_CONDITION_MET.getForwardName(), "dummy"));
		pipe.setStopConditionXPathExpression("/block='key 4 nine'");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultStopConditionXpath.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
		assertEquals(StopReason.STOP_CONDITION_MET.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	public void testBasicWithoutXmlEscaping() throws Exception {
		pipe.setSender(getElementRenderer(false));
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithXmlChars.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultWithoutXmlCharsEscaped.txt");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testBasicWithXmlEscaping() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setEscapeXml(true);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithXmlChars.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultWithXmlCharsEscaped.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testEndOfLineString() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setEndOfLineString("EOL");
		configurePipe();
		pipe.start();

		Message input = getResource("EndMarked.txt");
		String expected = TestFileUtils.getTestFile("/Pipes/StreamLineIteratorPipe/EndMarkedResult.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertXmlEquals(expected, actual);
	}

	@Test
	public void testStartOfLineStringAndItemNoSessionKey() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setStartOfLineString("BOL");
		pipe.setItemNoSessionKey("itemNo");
		configurePipe();
		pipe.start();

		Message input = getResource("BeginMarked.txt");
		String expected = TestFileUtils.getTestFile("/Pipes/StreamLineIteratorPipe/BeginMarkedResult.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertXmlEquals(expected, actual);
		assertEquals("3", session.getString("itemNo"));
	}

	@Test
	public void testItemNoSessionKeyEmptyInput() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setItemNoSessionKey("itemNo");
		configurePipe();
		pipe.start();

		String input = "";
		String expected = "<results/>";

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertXmlEquals(expected, actual);
		assertEquals("0", session.getString("itemNo"));
	}

	private ISender getElementRenderer() {
		resultLog = new StringBuilder();
		// returns the renderer that does not surround the input with brackets
		return new BlockEnabledRenderer() {
			@Override
			public SenderResult sendMessage(String blockHandle, Message message, PipeLineSession session) throws SenderException, TimeoutException {
				try {
					String result = message.asString();
					resultLog.append(result+"\n");
					return new SenderResult(result);
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}
		};
	}
}
