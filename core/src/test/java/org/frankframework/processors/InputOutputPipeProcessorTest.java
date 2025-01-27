package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.FixedResultPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

public class InputOutputPipeProcessorTest {

	public static final String PIPE_RUN_RESULT_TEXT = "pipe run result";
	public static final String INPUT_MESSAGE_TEXT = "input message";

	private InputOutputPipeProcessor processor;
	private PipeLine pipeLine;
	private PipeLineSession session;
	private FixedResultPipe pipe;


	@BeforeEach
	public void setUp() throws ConfigurationException {
		processor = new InputOutputPipeProcessor();
		PipeProcessor chain = new PipeProcessor() {
			@Override
			public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession) throws PipeRunException {
				return pipe.doPipe(message, pipeLineSession);
			}

			@Override
			public PipeRunResult validate(PipeLine pipeLine, IValidator validator, Message message, PipeLineSession pipeLineSession, String messageRoot) throws PipeRunException {
				return validator.validate(message, pipeLineSession, messageRoot);
			}
		};
		processor.setPipeProcessor(chain);

		pipeLine = new PipeLine();
		Adapter owner = new Adapter();
		owner.setName("PipeLine owner");
		pipeLine.setOwner(owner);

		pipe = new FixedResultPipe();
		pipe.setReturnString(PIPE_RUN_RESULT_TEXT);
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);

		session = new PipeLineSession();
	}

	@Test
	public void testCompactMessage() throws Exception {
		// Arrange
		EchoPipe pipe = new EchoPipe();
		pipe.setRestoreMovedElements(false);
		pipe.setRemoveCompactMsgNamespaces(true);
		pipe.setElementToMove("identificatie");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		pipe.configure();
		pipe.start();

		Message input = MessageTestUtils.getMessage("/Util/CompactSaxHandler/input.xml");

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(2, session.size());
		assertEquals("DC2023-00020", session.getString("ref_identificatie"));
		assertEquals("DC2022-012345", session.getString("ref_identificatie2"));

		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-chaintest.xml");
		assertEquals(testOutputFile, prr.getResult().asString());
	}

	@Test
	public void testCompactMessageAndRestoreElement() throws Exception {
		// Arrange
		EchoPipe pipe1 = new EchoPipe();
		pipe1.setRestoreMovedElements(false);
		pipe1.setRemoveCompactMsgNamespaces(true);
		pipe1.setElementToMove("identificatie");
		PipeForward forward1 = new PipeForward();
		forward1.setName("success");
		pipe1.registerForward(forward1);
		pipe1.configure();
		pipe1.start();

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setRestoreMovedElements(true);
		pipe2.setRemoveCompactMsgNamespaces(false);
		PipeForward forward2 = new PipeForward();
		forward2.setName("success");
		pipe2.registerForward(forward2);
		pipe2.configure();
		pipe2.start();

		Message input = MessageTestUtils.getMessage("/Util/CompactSaxHandler/input.xml");

		// Act
		PipeRunResult prr1 = processor.processPipe(pipeLine, pipe1, input, session);
		PipeRunResult prr2 = processor.processPipe(pipeLine, pipe2, prr1.getResult(), session);

		// Assert
		assertEquals(2, session.size());
		assertEquals("DC2023-00020", session.getString("ref_identificatie"));
		assertEquals("DC2022-012345", session.getString("ref_identificatie2"));

		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output.xml");
		assertEquals(testOutputFile, prr2.getResult().asString());
		assertFalse(input.isNull(), "Input Message should not be closed, because it can be used in the session");
		assertFalse(prr1.getResult().isNull(), "Input Message of pipe2 should not be closed, because it can be used in the session");
	}

	public void testRestoreMovedElement(Object sessionVarContents) throws Exception {

		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setRestoreMovedElements(true);
		pipe.setReturnString("result [{sessionKey:replaceThis}]");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		pipe.configure();
		pipe.start();

		Message input = new Message("input");

		session.put("replaceThis", sessionVarContents);

		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		assertEquals("result [ReplacedValue]", prr.getResult().asString());

	}

	@Test
	public void testRestoreMovedElementString() throws Exception {
		testRestoreMovedElement("ReplacedValue");
	}

	@Test
	public void testRestoreMovedElementMessage() throws Exception {
		testRestoreMovedElement(new Message("ReplacedValue"));
	}

	@Test
	public void testRestoreMovedElementReader() throws Exception {
		testRestoreMovedElement(new StringReader("ReplacedValue"));
	}

	@Test
	public void testRestoreMovedElementByteArray() throws Exception {
		testRestoreMovedElement("ReplacedValue".getBytes());
	}

	@Test
	public void testSkipWhenInputSessionKeyNotSet() throws Exception {
		// Arrange
		pipe.setOnlyIfSessionKey("this-key-isnt-there");
		pipe.setGetInputFromSessionKey("this-key-isnt-there");
		pipe.configure();
		pipe.start();

		Message input = Message.asMessage(INPUT_MESSAGE_TEXT);

		// This should be true
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
	}

	@Test
	public void testThrowWhenInputSessionKeyNotSet() throws Exception {
		// Arrange
		pipe.setOnlyIfSessionKey("this-key-is-there");
		pipe.setGetInputFromSessionKey("this-key-isnt-there");
		pipe.configure();
		pipe.start();

		Message input = Message.asMessage("dummy");
		session.put("this-key-is-there", "the-key-the-value");

		// This should be true
		assertFalse(pipe.skipPipe(input, session));

		// Act / Assert
		assertThrows(PipeRunException.class, ()-> processor.processPipe(pipeLine, pipe, input, session));
	}

	@Test
	public void testWhenUnlessSessionKeySetAndPresent() throws Exception {
		// Arrange
		pipe.setUnlessSessionKey("this-key-is-there");
		pipe.configure();
		pipe.start();

		session.put("this-key-is-there", "value");

		Message input = Message.asMessage(INPUT_MESSAGE_TEXT);

		// This should be true
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
	}

	@Test
	public void testSkipOnEmptyInput() throws Exception {
		// Arrange
		pipe.setSkipOnEmptyInput(true);
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		// This should be true
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertNull(prr.getResult().asString());
	}

	@Test
	public void testEmptyInputReplaced() throws Exception {
		// Arrange
		pipe.setSkipOnEmptyInput(true);
		pipe.setEmptyInputReplacement("empty input replacement");
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(PIPE_RUN_RESULT_TEXT, prr.getResult().asString());
	}

	@Test
	public void testGetInputFromFixedValue() throws Exception {
		// Arrange
		pipe.setSkipOnEmptyInput(true);
		pipe.setGetInputFromFixedValue("fixed value return");
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(PIPE_RUN_RESULT_TEXT, prr.getResult().asString());
	}

	@Test
	public void testGetInputFromSessionKey() throws Exception {
		// Arrange
		pipe.setSkipOnEmptyInput(true);
		pipe.setGetInputFromSessionKey("the-session-key");
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		session.put("the-session-key", "session-key-value");

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(PIPE_RUN_RESULT_TEXT, prr.getResult().asString());
	}

	@Test
	public void testSkipIfParameter() throws Exception {
		// Arrange
		pipe.setIfParam("my-param");
		Parameter param = new Parameter("my-param", "the-value");
		pipe.addParameter(param);
		pipe.configure();
		pipe.start();

		Message input = Message.asMessage(INPUT_MESSAGE_TEXT);

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
	}
}
