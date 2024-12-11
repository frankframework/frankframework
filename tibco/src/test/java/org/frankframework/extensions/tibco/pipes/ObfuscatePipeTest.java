package org.frankframework.extensions.tibco.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;

class ObfuscatePipeTest extends PipeTestBase<ObfuscatePipe> {

	private static final String PLAIN_TEXT = "Bacon ipsum dolor amet chuck pork.";

	@Override
	public ObfuscatePipe createPipe() {
		return new ObfuscatePipe();
	}

	@Test
	void testEncryption() throws Exception {
		// Arrange
		configureAndStartPipe();
		Message in = new Message(PLAIN_TEXT);

		// Act
		PipeRunResult encodeResult = doPipe(pipe, in, session);

		// Assert
		Message message = encodeResult.getResult();
		assertFalse(message.isBinary());
		String messageContent = message.asString();

		// The actual generated string differs. The length doesn't
		assertEquals(110, messageContent.length());
		message.close();
		in.close();

		assertEquals(PLAIN_TEXT, ObfuscationEngine.decrypt(messageContent));
	}

	@Test
	void testEmptyInput() throws ConfigurationException, IOException, PipeRunException {
		// Arrange
		configureAndStartPipe();

		// Act
		PipeRunResult nullMessageResult = doPipe(pipe, Message.nullMessage(), session);
		PipeRunResult emptyResult = doPipe(pipe, "", session);

		// Assert
		assertNull(nullMessageResult.getResult().asString());
		assertEquals("", emptyResult.getResult().asString());
	}

	@Test
	void testObfuscationEngine() throws Exception {
		String encrypted = ObfuscationEngine.encrypt(PLAIN_TEXT);

		assertEquals(PLAIN_TEXT, ObfuscationEngine.decrypt(encrypted));

		// Test from the site we based the ObfuscationEngine off
		String password = "Amadeus@123";
		String encryptedPassword = ObfuscationEngine.encrypt(password);

		assertEquals(password, ObfuscationEngine.decrypt(encryptedPassword));
	}

	@Test
	void testDecryption() throws ConfigurationException, IOException, PipeRunException {
		// Arrange
		pipe.setDirection(ObfuscatePipe.Direction.DEOBFUSCATE);
		configureAndStartPipe();

		String input = "#!ghmoEO0lRMwnSi7AoRkMx5w1UuoWGU9Z9uM2mK1Nl7jw0rpQRPaInL725oe0NlCo+skzv8rBb+JCBR6d4qvbTWh68raP58uP+6iw+HI7t7c=";
		Message in = new Message(input);

		// Act
		PipeRunResult encodeResult = doPipe(pipe, in, session);

		// Assert
		assertEquals(PLAIN_TEXT, encodeResult.getResult().asString());
	}
}
