package nl.nn.adapterframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.pipes.FixedResultPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class InputOutputPipeProcessorTest {

	private InputOutputPipeProcessor processor;
	private PipeLine pipeLine;
	private PipeLineSession session;

	@BeforeEach
	public void setUp() {
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
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setOnlyIfSessionKey("this-key-isnt-there");
		pipe.setGetInputFromSessionKey("this-key-isnt-there");
		pipe.setReturnString("not this");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		pipe.configure();
		pipe.start();

		Message input = Message.asMessage("should get this");

		// This should be true
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("should get this", prr.getResult().asString());
	}

	@Test
	public void testThrowWhenInputSessionKeyNotSet() throws Exception {
		// Arrange
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setOnlyIfSessionKey("this-key-is-there");
		pipe.setGetInputFromSessionKey("this-key-isnt-there");
		pipe.setReturnString("not this");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
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
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setUnlessSessionKey("this-key-is-there");
		pipe.setReturnString("not this");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		pipe.configure();
		pipe.start();

		session.put("this-key-is-there", "value");

		Message input = Message.asMessage("should get this");

		// This should be true
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("should get this", prr.getResult().asString());
	}

	@Test
	public void testSkipOnEmptyInput() throws Exception {
		// Arrange
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setSkipOnEmptyInput(true);
		pipe.setReturnString("not this");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
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
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setSkipOnEmptyInput(true);
		pipe.setEmptyInputReplacement("empty input replacement");
		pipe.setReturnString("get this");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("get this", prr.getResult().asString());
	}

	@Test
	public void testGetInputFromFixedValue() throws Exception {
		// Arrange
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setSkipOnEmptyInput(true);
		pipe.setGetInputFromFixedValue("fixed value return");
		pipe.setReturnString("get this");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("get this", prr.getResult().asString());
	}

	@Test
	public void testGetInputFromSessionKey() throws Exception {
		// Arrange
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setSkipOnEmptyInput(true);
		pipe.setGetInputFromSessionKey("the-session-key");
		pipe.setReturnString("get this");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		session.put("the-session-key", "session-key-value");

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("get this", prr.getResult().asString());
	}

	@Test
	public void testSkipIfParameter() throws Exception {
		// Arrange
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setIfParam("my-param");
		pipe.setReturnString("not this");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		Parameter param = new Parameter("my-param", "the-value");
		pipe.addParameter(param);
		pipe.configure();
		pipe.start();

		Message input = Message.asMessage("should get this");

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("should get this", prr.getResult().asString());
	}
}
