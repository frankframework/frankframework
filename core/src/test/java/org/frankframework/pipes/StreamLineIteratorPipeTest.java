package org.frankframework.pipes;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.pipes.IteratingPipe.StopReason;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

class StreamLineIteratorPipeTest extends IteratingPipeTestBase<StreamLineIteratorPipe> {

	@Override
	public StreamLineIteratorPipe createPipe() {
		StreamLineIteratorPipe result = new StreamLineIteratorPipe();
		result.setCombineBlocks(false); // default is true, but false is compatible with super test class IteratingPipeTestBase
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
		String actual = prr.getResult().asString();

		assertEquals(expectedLog, resultLog.toString().trim(), expectedLogFile);
		assertEquals(expected, actual);
	}

	@Test
	void testBasicWithLinePrefixAndSuffix() throws Exception {
		testBasicWithLinePrefixAndSuffix(false, false, "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}

	@Test
	void testBasicWithLinePrefixAndSuffixCombined() throws Exception {
		testBasicWithLinePrefixAndSuffix(false, true, "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}

	@Test
	void testBasicWithLinePrefixAndSuffixBlockEnabled() throws Exception {
		testBasicWithLinePrefixAndSuffix(true, false, "/IteratingPipe/TenLinesLogPlainWithLineFixesBlockEnabled.txt");
	}

	@Test
	void testBasicWithLinePrefixAndSuffixBlockEnabledCombined() throws Exception {
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
		String actual = prr.getResult().asString();

		assertEquals(expectedLog, resultLog.toString().trim(), expectedLogFile);
		assertEquals(expected, actual, expectedFile);
	}

	@Test
	void testFullBlocksWithCombineOff() throws Exception {
		testBlocksWithCombine(false, false, 5, "/IteratingPipe/TenLinesResultWithLineFixes.xml", "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}

	@Test
	void testFullBlocksWithCombineOn() throws Exception {
		testBlocksWithCombine(false, true, 5, "/IteratingPipe/TenLinesResultCombinedInBlocksOfFiveWithLineFixes.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfFiveWithLineFixes.txt");
	}

	@Test
	void testFullBlocksWithCombineOffBlockEnabled() throws Exception {
		testBlocksWithCombine(true, false, 5, "/IteratingPipe/TenLinesResultWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogInBlocksOfFiveWithLineFixesBlockEnabled.txt");
	}

	@Test
	void testFullBlocksWithCombineOnBlockEnabled() throws Exception {
		testBlocksWithCombine(true, true, 5, "/IteratingPipe/TenLinesResultCombinedInBlocksOfFiveWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfFiveWithLineFixesBlockEnabled.txt");
	}

	@Test
	void testBlocksOf1WithCombineOff() throws Exception {
		testBlocksWithCombine(false, false, 1, "/IteratingPipe/TenLinesResultWithLineFixes.xml", "/IteratingPipe/TenLinesLogPlainWithLineFixes.txt");
	}

	@Test
	void testBlocksOf1WithCombineOn() throws Exception {
		testBlocksWithCombine(false, true, 1, "/IteratingPipe/TenLinesResultCombinedInBlocksOfOneWithLineFixes.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfOneWithLineFixes.txt");
	}

	@Test
	void testBlocksOf1WithCombineOffBlockEnabled() throws Exception {
		testBlocksWithCombine(true, false, 1, "/IteratingPipe/TenLinesResultWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogInBlocksOfOneWithLineFixesBlockEnabled.txt");
	}

	@Test
	void testBlocksOf1WithCombineOnBlockEnabled() throws Exception {
		testBlocksWithCombine(true, true, 1, "/IteratingPipe/TenLinesResultCombinedInBlocksOfOneWithLineFixesBlockEnabled.xml", "/IteratingPipe/TenLinesLogCombinedInBlocksOfOneWithLineFixesBlockEnabled.txt");
	}

	@Test
	void testPartialFinalBlockWithLinePrefixAndSuffix() throws Exception {
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
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	@Test
	void testPartialFinalBlockMaxItemsWithLinePrefixAndSuffix() throws Exception {
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
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	@Test
	void testMaxItemsWithSpecialForward() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		pipe.setLinePrefix("{");
		pipe.setLineSuffix("}");
		pipe.setCombineBlocks(true);
		pipe.addForward(new PipeForward(StopReason.MAX_ITEMS_REACHED.getForwardName(), "dummy"));
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultInBlocksOfFour.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(StopReason.MAX_ITEMS_REACHED.getForwardName(), prr.getPipeForward().getName());
		assertEquals(expected, actual);
	}

	@Test
	void testMaxItemsReachedWithoutSpecialForwardRegistered() throws Exception {
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
		String actual = prr.getResult().asString();

		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
		assertEquals(expected, actual);
	}

	@Test
	void testBlocksByKey() throws Exception {
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
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	@Test
	void testBlocksByKeyWithStopConditionXpath() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setStartPosition(4);
		pipe.setEndPosition(5);
		pipe.addForward(new PipeForward(StopReason.STOP_CONDITION_MET.getForwardName(), "dummy"));
		pipe.setStopConditionXPathExpression("/block='key 4 nine'");
		pipe.setCombineBlocks(true);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultStopConditionXpath.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
		assertEquals(StopReason.STOP_CONDITION_MET.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	void testBasicWithoutXmlEscaping() throws Exception {
		pipe.setSender(getElementRenderer(false));
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithXmlChars.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultWithoutXmlCharsEscaped.txt");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	@Test
	void testBasicWithXmlEscaping() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setEscapeXml(true);
		configurePipe();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/IteratingPipe/TenLinesWithXmlChars.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultWithXmlCharsEscaped.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	@Test
	void testEndOfLineString() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setEndOfLineString("EOL");
		configurePipe();
		pipe.start();

		Message input = getResource("EndMarked.txt");
		String expected = TestFileUtils.getTestFile("/Pipes/StreamLineIteratorPipe/EndMarkedResult.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertXmlEquals(expected, actual);
	}

	@Test
	void testStartOfLineStringAndItemNoSessionKey() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setStartOfLineString("BOL");
		pipe.setItemNoSessionKey("itemNo");
		configurePipe();
		pipe.start();

		Message input = getResource("BeginMarked.txt");
		String expected = TestFileUtils.getTestFile("/Pipes/StreamLineIteratorPipe/BeginMarkedResult.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertXmlEquals(expected, actual);
		assertEquals("3", session.getString("itemNo"));
	}

	@Test
	void testItemNoSessionKeyEmptyInput() throws Exception {
		pipe.setSender(getElementRenderer(false));
		pipe.setItemNoSessionKey("itemNo");
		configurePipe();
		pipe.start();

		String input = "";
		String expected = "<results/>";

		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertXmlEquals(expected, actual);
		assertEquals("0", session.getString("itemNo"));
	}

	private ISender getElementRenderer() {
		// returns the renderer that does not surround the input with brackets
		return new BlockEnabledRenderer() {
			@Override
			public SenderResult sendMessage(String blockHandle, Message message, PipeLineSession session) throws SenderException {
				try {
					String result = message.asString();
					resultLog.append(result).append("\n");
					return new SenderResult(result);
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}
		};
	}
}
