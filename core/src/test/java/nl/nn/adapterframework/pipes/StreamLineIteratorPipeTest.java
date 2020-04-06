package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class StreamLineIteratorPipeTest extends PipeTestBase<StreamLineIteratorPipe> {

	@Override
	public StreamLineIteratorPipe createPipe() {
		return new StreamLineIteratorPipe();
	}

	protected ISender getElementRenderer() {
		return getElementRenderer(null);
	}

	protected ISender getElementRenderer(final Exception e) {
		EchoSender sender = new EchoSender() {

			@Override
			public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
				try {
					if (message.asString().contains("error")) {
						throw new SenderException("Exception triggered", e);
					}
				} catch (IOException e) {
					throw new SenderException(getLogPrefix(),e);
				}
				return super.sendMessage(message, session);
			}

		};
		return sender;
	}


	@Test
	public void testBasic() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setLinePrefix("["); //TODO: 2020-04-01: this currently does not work properly. 
		pipe.setLineSuffix("]");
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResult.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testFullBlocks() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setBlockSize(5);
		pipe.setLinePrefix("[");
		pipe.setLineSuffix("]");
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultInBlocksOfFive.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testPartialFinalBlock() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setBlockSize(4);
		pipe.setLinePrefix("[");
		pipe.setLineSuffix("]");
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultInBlocksOfFour.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testPartialFinalBlockMaxItems() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setBlockSize(4);
		pipe.setMaxItems(7);
		pipe.setLinePrefix("[");
		pipe.setLineSuffix("]");
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/SevenLinesResultInBlocksOfFour.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	@Test
	public void testBlocksByKey() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setStartPosition(4);
		pipe.setEndPosition(5);
		pipe.setLinePrefix("[");
		pipe.setLineSuffix("]");
		configurePipe();
		pipe.start();

		Message input = TestFileUtils.getTestFileMessage("/IteratingPipe/TenLines.txt");
		String expected = TestFileUtils.getTestFile("/IteratingPipe/TenLinesResultInKeyBlocks.xml");
		
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

}
