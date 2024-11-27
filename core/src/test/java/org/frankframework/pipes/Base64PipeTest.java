/*
   Copyright 2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64InputStream;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.pipes.Base64Pipe.Direction;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;
import org.frankframework.util.StreamUtil;

class Base64PipeTest extends PipeTestBase<Base64Pipe> {

	private final String plainText = "Bacon ipsum dolor amet chuck pork loin flank picanha.";
	private final String base64Encoded = "QmFjb24gaXBzdW0gZG9sb3IgYW1ldCBjaHVjayBwb3JrIGxvaW4gZmxhbmsgcGljYW5oYS4=";

	@Override
	public Base64Pipe createPipe() {
		return new Base64Pipe();
	}

	@Test
	void wrongInputEncoding() throws ConfigurationException, PipeStartException {
		// Arrange
		pipe.setCharset("test123");
		pipe.configure();
		pipe.start();

		// Act / Assert
		assertThrows(PipeRunException.class, () -> doPipe(pipe, plainText, session));
	}

	@Test
	void wrongOutputEncoding() throws ConfigurationException, PipeStartException, PipeRunException {
		// Arrange
		pipe.setCharset("test123");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		// Act
		PipeRunResult decodeResult = doPipe(pipe, base64Encoded, session);

		// Assert
		assertThrows(Exception.class, () -> decodeResult.getResult().asString());
	}

	@Test
	void wrongCharsetEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.configure();
		pipe.start();
		byte[] inputString = "Më-×m👌‰Œœ‡TzdDEyMt120=".getBytes("WINDOWS-1252"); //String containing utf-8 characters
		Message in = new Message(inputString); //Saving it with a different charset

		// Act
		PipeRunResult encodeResult = doPipe(pipe, in, session); //Base64Pipe still works and does as told (convert a string with an incompatible charset)

		// Assert
		assertEquals("Test120/iYych1R6ZERFeU10MTIwPQ==", encodeResult.getResult().asString().trim()); //Unreadable base64 string
	}

	@Test
	void wrongCharsetShouldNotBeUsed() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.setCharset("ISO-8859-1"); //Should be ignored
		pipe.configure();
		pipe.start();

		String utf8Input = "Më-×m👌‰Œœ‡TzdDEyMt120=";
		byte[] inputBytes = utf8Input.getBytes(StandardCharsets.UTF_8); //String containing utf-8 characters
		Message in = new Message(inputBytes, "auto"); //Saving it with a different charset

		assertEquals(utf8Input, in.asString()); // read the message which should update the auto field in the MessageContext
		assertEquals("UTF-8", in.getContext().get(MessageContext.METADATA_CHARSET)); //base64#charset attribute should be ignored because of explicit value in the MessageContext.

		// Act
		Message result = doPipe(pipe, in, session).getResult();

		// Assert
		assertEquals("TcOrLcOXbfCfkYzigLDFksWT4oChVHpkREV5TXQxMjA9", result.asString().trim()); //validate and preserve the message

		InputStream decodedResult = new Base64InputStream(result.asInputStream(), false);
		assertEquals(utf8Input, StreamUtil.streamToString(decodedResult));
	}

	@Test
	void wrongCharsetDecoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		String encodedString = "Test120/iYych1R6ZERFeU10MTIwPQ==";
		byte[] expected = "Më-×m👌‰Œœ‡TzdDEyMt120=".getBytes("WINDOWS-1252"); //String containing utf-8 characters

		// Act
		PipeRunResult decodeResult = doPipe(pipe, encodedString, session);

		// Assert
		assertEquals(new String(expected, StandardCharsets.UTF_8), decodeResult.getResult().asString());
	}

	@Test
	void correctEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.configure();
		pipe.start();
		byte[] inputString = "Më-×m👌‰Œœ‡TzdDEyMt120=".getBytes(StandardCharsets.UTF_8);
		Message in = new Message(inputString);

		// Act
		PipeRunResult encodeResult = doPipe(pipe, in, session);

		// Assert
		assertEquals("TcOrLcOXbfCfkYzigLDFksWT4oChVHpkREV5TXQxMjA9", encodeResult.getResult().asString().trim());
	}

	@Test
	void correctDecoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		String encodedString = "TcOrLcOXbfCfkYzigLDFksWT4oChVHpkREV5TXQxMjA9";
		String decodedString = "Më-×m👌‰Œœ‡TzdDEyMt120=";

		// Act
		PipeRunResult decodeResult = doPipe(encodedString);

		// Assert
		Message result = decodeResult.getResult();
		assertTrue(result.isBinary(), "Base64 decode defaults to binary data");
		assertEquals(decodedString, result.asString());
	}

	@Test
	void encodeConvertStringInput() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.configure();
		pipe.start();

		// Act
		PipeRunResult prr = doPipe(pipe, plainText, session);

		// Assert
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	void encodeConvertBytesInput() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.configure();
		pipe.start();

		// Act
		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);

		// Assert
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	void decodeConvert2StringFalse() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		// Act
		PipeRunResult prr = doPipe(pipe, base64Encoded, session);

		// Assert
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(plainText, new String(result).trim());
	}

	//String input encode
	@Test
	void inputStringOutputString() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.configure();
		pipe.start();

		// Act
		PipeRunResult prr = doPipe(pipe, plainText, session);

		// Assert
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	//String stream encode
	@Test
	void inputStreamOutputString() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(plainText.getBytes());

		// Act
		PipeRunResult prr = doBase64PipeWithInputStream(stream);

		// Assert
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	void inputStringOutputBytesDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		// Act
		PipeRunResult prr = doPipe(pipe, base64Encoded, session);

		// Assert
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(plainText, new String(result).trim());
	}

	//String stream decode
	@Test
	void inputStreamOutputStringDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		// Arrange
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(base64Encoded.getBytes());

		// Act
		PipeRunResult prr = doBase64PipeWithInputStream(stream);

		// Assert
		String result = prr.getResult().asString();
		assertEquals(plainText, result.trim());
	}

	private PipeRunResult doBase64PipeWithInputStream(final InputStream stream) throws PipeRunException {

		Message input = new Message(new ThrowingAfterCloseInputStream(stream));
		input.closeOnCloseOf(session, pipe);

		assertTrue(input.isScheduledForCloseOnExitOf(session), "Before Base64Pipe, streaming input message should be scheduled for close on close of session");

		PipeRunResult prr;
		try (PipeLineSession ignored = session) {
			prr = pipe.doPipe(input, session);

			// Before session closes, unschedule result from close-on-close.
			prr.getResult().unscheduleFromCloseOnExitOf(session);
		}
		assertFalse(session.isScheduledForCloseOnExit(input), "After Base64Pipe, input message should no longer be scheduled for close on close of session");
		return prr;
	}
}
