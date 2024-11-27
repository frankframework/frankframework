package org.frankframework.pipes;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;


/**
 * TextSplitterPipe Tester.
 *
 * @author Gerrit van Brakel
 */
public class TextSplitterPipeTest extends PipeTestBase<TextSplitterPipe> {

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		pipe.configure();
		pipe.start();
	}

	@Override
	public TextSplitterPipe createPipe() {
		return new TextSplitterPipe();
	}

	@Test
	void testEmptyOrNullInput() throws PipeRunException, IOException {
		PipeRunResult prr = doPipe("");
		assertEquals("<text/>", prr.getResult().asString());

		prr = doPipe(Message.nullMessage());
		assertEquals("<text/>", prr.getResult().asString());

		prr = doPipe(new Message(""));
		assertEquals("<text/>", prr.getResult().asString());
	}

	@Test
	public void testShortMessage() throws Exception {
		String message = "This is a short message that can be sent in a single SMS message";
		String expected = "<text><block>This is a short message that can be sent in a single SMS message</block></text>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testLongMessageSoftSplit() throws Exception {
		pipe.setSoftSplit(true);

		String messagePart1 = "This is a long message that that will be split up one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen";
		String messagePart2 = "eighteen nineteen twenty";
		String message = messagePart1 + " " + messagePart2;
		String expected = "<text><block>" + messagePart1 + "</block><block>" + messagePart2 + "</block></text>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testLongMessageHardSplit() throws Exception {
		String messagePart1 = "This is a long message that that will be split up one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eig";
		String messagePart2 = "hteen nineteen twenty";
		String message = messagePart1 + messagePart2;
		String expected = "<text><block>" + messagePart1 + "</block><block>" + messagePart2 + "</block></text>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

}
