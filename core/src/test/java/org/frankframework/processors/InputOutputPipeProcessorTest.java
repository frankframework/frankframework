package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.util.FileSystemUtils;

import net.jcip.annotations.NotThreadSafe;

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
import org.frankframework.stream.Message;
import org.frankframework.stream.SerializableFileReference;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.TemporaryDirectoryUtils;

@NotThreadSafe // should be picked up by surefire
public class InputOutputPipeProcessorTest {

	public static final String INPUT_MESSAGE_TEXT = "input message";
	private EchoPipe pipe;
	private PipeLine pipeLine;
	private InputOutputPipeProcessor processor;
	private PipeLineSession session;
	private final AtomicBoolean pipeExecuted = new AtomicBoolean(false);

	@BeforeEach
	public void setUp() throws Exception {
		FileSystemUtils.deleteRecursively(TemporaryDirectoryUtils.getTempDirectory(SerializableFileReference.TEMP_MESSAGE_DIRECTORY));

		processor = new InputOutputPipeProcessor();
		PipeProcessor chain = new PipeProcessor() {
			@Nonnull
			@Override
			public PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession) throws PipeRunException {
				pipeExecuted.set(true);
				return pipe.doPipe(message, pipeLineSession);
			}

			@Nonnull
			@Override
			public PipeRunResult validate(@Nonnull PipeLine pipeLine, @Nonnull IValidator validator, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, String messageRoot) throws PipeRunException {
				pipeExecuted.set(true);
				return validator.validate(message, pipeLineSession, messageRoot);
			}
		};
		processor.setPipeProcessor(chain);

		pipeLine = new PipeLine();
		Adapter owner = new Adapter();
		owner.setName("PipeLine owner");
		pipeLine.setApplicationContext(owner);

		pipe = new EchoPipe();

		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.addForward(forward);

		session = new PipeLineSession();
	}

	// ensure that no files are in the 'restoreMovedElements' folder
	@AfterEach
	public void teardown() throws IOException {
		session.close();
		Path tempDirectory = TemporaryDirectoryUtils.getTempDirectory(SerializableFileReference.TEMP_MESSAGE_DIRECTORY);
		assertEquals(0, Files.list(tempDirectory).count());
	}

	@Test
	public void testCompactMessage() throws Exception {
		// Arrange
		pipe.setRestoreMovedElements(false);
		pipe.setRemoveCompactMsgNamespaces(true);
		pipe.setElementToMove("identificatie");
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
		assertFalse(input.isNull());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
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
		pipe1.addForward(forward1);
		pipe1.configure();
		pipe1.start();

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setRestoreMovedElements(true);
		pipe2.setRemoveCompactMsgNamespaces(false);
		PipeForward forward2 = new PipeForward();
		forward2.setName("success");
		pipe2.addForward(forward2);
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

		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output.xml")
				.replace("<Header/>", "<Header></Header>"); // RestoreMovedElementsHandler does not handle empty elements
		assertEquals(testOutputFile, prr2.getResult().asString());
		assertFalse(input.isNull());
		assertFalse(prr1.getResult().isNull());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
	}

	private void testRestoreMovedElement(Object sessionVarContents) throws Exception {
		EchoPipe pipe = new EchoPipe();
		pipe.setGetInputFromFixedValue("<xml>result [{sessionKey:replaceThis}]</xml>");
		pipe.setRestoreMovedElements(true);
		Message input = new Message("input");
		session.put("replaceThis", sessionVarContents);

		PipeForward forward = new PipeForward();
		forward.setName("success");

		pipe.addForward(forward);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		assertEquals("<xml>result [ReplacedValue]</xml>", prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
		session.close();
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

		Message input = new Message(INPUT_MESSAGE_TEXT);

		// This should be true
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
		assertFalse(pipeExecuted.get(), "Pipe executed but should not have been");
	}

	@Test
	public void testThrowWhenInputSessionKeyNotSet() throws Exception {
		// Arrange
		pipe.setOnlyIfSessionKey("this-key-is-there");
		pipe.setGetInputFromSessionKey("this-key-isnt-there");
		pipe.configure();
		pipe.start();

		Message input = new Message("dummy");
		session.put("this-key-is-there", "the-key-the-value");

		// This should be false
		assertFalse(pipe.skipPipe(input, session));

		// Act / Assert
		assertThrows(PipeRunException.class, () -> processor.processPipe(pipeLine, pipe, input, session));
		assertFalse(pipeExecuted.get(), "Pipe executed but should not have been");
	}

	@Test
	public void testDontThrowWhenInputSessionKeyNotSetButEmptyInputReplacementSet() throws Exception {
		// Arrange
		pipe.setOnlyIfSessionKey("this-key-is-there");
		pipe.setGetInputFromSessionKey("this-key-isnt-there");
		pipe.setEmptyInputReplacement("");
		pipe.configure();
		pipe.start();

		Message input = new Message(INPUT_MESSAGE_TEXT);
		session.put("this-key-is-there", "the-key-the-value");

		// This should be false
		assertFalse(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("", prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
	}

	@Test
	public void testWhenUnlessSessionKeySetAndPresent() throws Exception {
		// Arrange
		pipe.setUnlessSessionKey("this-key-is-there");
		pipe.configure();
		pipe.start();

		session.put("this-key-is-there", "value");

		Message input = new Message(INPUT_MESSAGE_TEXT);

		// This should be true
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
		assertFalse(pipeExecuted.get(), "Pipe executed but should not have been");
	}

	@Test
	public void testWhenUnlessSessionKeySetAndNotPresent() throws Exception {
		// Arrange
		pipe.setUnlessSessionKey("this-key-is-not-there");
		pipe.configure();
		pipe.start();

		Message input = new Message(INPUT_MESSAGE_TEXT);

		// This should be false, because the sessionkey does not exist
		assertFalse(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
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
		assertFalse(pipeExecuted.get(), "Pipe executed but should not have been");
	}

	@Test
	public void testEmptyInputReplaced() throws Exception {
		// Arrange
		String expectedValue = "empty input replacement";

		pipe.setSkipOnEmptyInput(true);
		pipe.setEmptyInputReplacement(expectedValue);
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(expectedValue, prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
	}

	@Test
	public void testGetInputFromFixedValue() throws Exception {
		// Arrange
		String expectedValue = "fixed value return";

		pipe.setSkipOnEmptyInput(true);
		pipe.setGetInputFromFixedValue(expectedValue);
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(expectedValue, prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
	}

	@Test
	public void testGetInputFromSessionKey() throws Exception {
		// Arrange
		pipe.setSkipOnEmptyInput(true);
		pipe.setGetInputFromSessionKey("the-session-key");
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		String expectedValue = "session-key-value";
		session.put("the-session-key", expectedValue);

		// This should be true, b/c input message is empty, but code point
		// should not be triggered because it is replaced by the session key.
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(expectedValue, prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");

	}

	@ParameterizedTest // SessionKey exists but is empty
	@NullAndEmptySource
	public void testSkipOnEmptyInputAfterGetInputFromSessionKey(String sessionValue) throws Exception {
		// Arrange
		pipe.setGetInputFromSessionKey("the-session-key");
		pipe.setSkipOnEmptyInput(true);
		pipe.setStoreResultInSessionKey("output-session-key");
		pipe.configure();
		pipe.start();

		Message input = new Message("original-input");
		session.put("the-session-key", sessionValue);

		// This should be true when using the value from session-key
		assertTrue(pipe.skipPipe(session.getMessage("the-session-key"), session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert

		// Pipe was skipped on getting input from session-key made it an empty input, but the original input is preserved
		assertEquals("original-input", prr.getResult().asString());
		assertFalse(session.containsKey("output-session-key"));
		assertFalse(pipeExecuted.get(), "Pipe executed but should not have been");
	}

	@Test
	public void testGetInputFromSessionKeyAndFixedValue() throws Exception {
		// Arrange
		pipe.setSkipOnEmptyInput(true);
		pipe.setGetInputFromSessionKey("the-session-key");
		pipe.setGetInputFromFixedValue("fixed-value");
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();
		session.put("the-session-key", "session-value"); // This value is overwritten by the fixed value

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("fixed-value", prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void testGetInputFromEmptyFixedValue(String fixedValue) throws Exception {
		// Arrange
		pipe.setSkipOnEmptyInput(true);
		pipe.setGetInputFromFixedValue(fixedValue);
		pipe.setEmptyInputReplacement("tralala");
		pipe.configure();
		pipe.start();

		Message input = Message.nullMessage();

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("tralala", prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
	}

	@Test
	public void testSkipIfParameter() throws Exception {
		// Arrange
		pipe.setIfParam("my-param");
		Parameter param = new Parameter("my-param", "the-value");
		pipe.addParameter(param);
		pipe.configure();
		pipe.start();

		Message input = new Message(INPUT_MESSAGE_TEXT);

		// This should be true, b/c input message is empty
		assertTrue(pipe.skipPipe(input, session));

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
		assertFalse(pipeExecuted.get(), "Pipe executed but should not have been");
	}

	@Test
	public void testPreserveInput() throws Exception {
		// Arrange
		pipe.setGetInputFromFixedValue("dummy");
		pipe.setPreserveInput(true);
		pipe.configure();
		pipe.start();

		Message input = new Message(INPUT_MESSAGE_TEXT);

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
		assertTrue(pipeExecuted.get(), "Pipe should have been executed but isn't");
	}
}
