package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunResult;


/**
 * TextSplitterPipe Tester.
 *
 * @author Gerrit van Brakel
 */
public class TextSplitterPipeTest extends PipeTestBase<TextSplitterPipe> {

	@Override
	public TextSplitterPipe createPipe() {
		return new TextSplitterPipe();
	}


	@Test
	public void testConfigure() throws Exception {
		pipe.configure();
	}

	@Test
	public void testShortMessage() throws Exception {
		pipe.configure();
		pipe.start();
		
		String message ="This is a short message that can be sent in a single SMS message";
		String expected = "<text><block>This is a short message that can be sent in a single SMS message</block></text>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testLongMessageSoftSplit() throws Exception {
		pipe.setSoftSplit(true);
		pipe.configure();
		pipe.start();
		
		String messagepart1 ="This is a long message that that will be split up one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen";
		String messagepart2 ="eighteen nineteen twenty";
		String message= messagepart1+ " " +messagepart2;
		String expected = "<text><block>"+messagepart1+"</block><block>"+messagepart2+"</block></text>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testLongMessageHardSplit() throws Exception {
		pipe.configure();
		pipe.start();
		
		String messagepart1 ="This is a long message that that will be split up one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eig";
		String messagepart2 ="hteen nineteen twenty";
		String message= messagepart1 +messagepart2;
		String expected = "<text><block>"+messagepart1+"</block><block>"+messagepart2+"</block></text>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

}
