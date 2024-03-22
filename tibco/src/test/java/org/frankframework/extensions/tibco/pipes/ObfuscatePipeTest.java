package org.frankframework.extensions.tibco.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.tibco.security.AXSecurityException;
import com.tibco.security.ObfuscationEngine;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;

class ObfuscatePipeTest extends PipeTestBase<ObfuscatePipe> {

	private final String plainText = "Bacon ipsum dolor amet chuck pork.";

	@Override
	public ObfuscatePipe createPipe() {
		return new ObfuscatePipe();
	}

	@Test
	void testEncryption() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		configureAndStartPipe();
		byte[] inputString = plainText.getBytes(StandardCharsets.UTF_8);
		Message in = new Message(inputString);

		// Act
		PipeRunResult encodeResult = doPipe(pipe, in, session);

		// Assert
		Message message = encodeResult.getResult();
		assertFalse(message.isBinary());
		System.out.println(message.asString());
		assertEquals(110, message.asString().length());
		message.close();
		in.close();
	}

	@Test
	void testEmptyInput() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
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
	@Disabled("This test is not always working somehow, but it works in production at customer site.")
	void testLibraryDoesWork() throws AXSecurityException {
		String result = ObfuscationEngine.encrypt(plainText.toCharArray());
		char[] decrypted = ObfuscationEngine.decrypt(result);

		assertEquals(plainText, new String(decrypted));
	}

	@Test
	@Disabled("This test is not always working somehow, but it works in production at customer site.")
	void testDecryption() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.setDirection(ObfuscatePipe.Direction.DEOBFUSCATE);
		configureAndStartPipe();
		byte[] inputString = "#!IE4R9xiIP+k4jLhPT1m4wubEcOfiMsq7eB6K93utkaqyrsMem/8uPWf7Ktq4JllwjsedkKsrMcYbmP0dvR5GzfnrrO1/MhVvPbWJLV0oxdI=".getBytes(StandardCharsets.UTF_8);
		Message in = new Message(inputString);

		// Act
		PipeRunResult encodeResult = doPipe(pipe, in, session);

		// Assert
		assertEquals(53, encodeResult.getResult().asString().length());
	}

}
