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
package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.codec.binary.Base64InputStream;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.Base64Pipe.Direction;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;
import nl.nn.adapterframework.util.StreamUtil;

public class Base64PipeTest extends StreamingPipeTestBase<Base64Pipe> {

	private String plainText = "Bacon ipsum dolor amet chuck pork loin flank picanha.";
	private String base64Encoded = "QmFjb24gaXBzdW0gZG9sb3IgYW1ldCBjaHVjayBwb3JrIGxvaW4gZmxhbmsgcGljYW5oYS4=";

	@Override
	public Base64Pipe createPipe() {
		return new Base64Pipe();
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongOutputType() throws ConfigurationException {
		pipe.setOutputType("not string or stream or bytes");
		pipe.configure();
	}

	@Test(expected = PipeRunException.class)
	public void wrongInputEncoding() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setCharset("test123");
		pipe.configure();
		pipe.start();

		doPipe(pipe,plainText, session);
	}

	@Test(expected = Exception.class)
	public void wrongOutputEncoding() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setCharset("test123");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult decodeResult = doPipe(pipe, base64Encoded, session);
		decodeResult.getResult().asString();
	}

	@Test
	public void wrongCharsetEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.configure();
		pipe.start();
		byte[] inputString = "Më-×m👌‰Œœ‡TzdDEyMt120=".getBytes("WINDOWS-1252"); //String containing utf-8 characters
		Message in = new Message(inputString); //Saving it with a different charset

		PipeRunResult encodeResult = doPipe(pipe, in, session); //Base64Pipe still works and does as told (convert a string with an incompatible charset)

		assertEquals("Test120/iYych1R6ZERFeU10MTIwPQ==", encodeResult.getResult().asString().trim()); //Unreadable base64 string
	}

	@Test
	public void wrongCharsetShouldNotBeUsed() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setCharset("ISO-8859-1"); //Should be ignored
		pipe.configure();
		pipe.start();

		String utf8Input = "Më-×m👌‰Œœ‡TzdDEyMt120=";
		byte[] inputBytes = utf8Input.getBytes("UTF-8"); //String containing utf-8 characters
		Message in = new Message(inputBytes, "auto"); //Saving it with a different charset

		assertEquals(utf8Input, in.asString()); // read the message which should update the auto field in the MessageContext
		assertEquals("UTF-8", in.getContext().get(MessageContext.METADATA_CHARSET)); //base64#charset attribute should be ignored because of explicit value in the MessageContext.

		Message result = doPipe(pipe, in, session).getResult();

		assertEquals("TcOrLcOXbfCfkYzigLDFksWT4oChVHpkREV5TXQxMjA9", result.asString().trim()); //validate and preserve the message

		InputStream decodedResult = new Base64InputStream(result.asInputStream(), false);
		assertEquals(utf8Input, StreamUtil.streamToString(decodedResult));

	}

	@Test
	public void wrongCharsetDecoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		String encodedString = "Test120/iYych1R6ZERFeU10MTIwPQ==";
		byte[] expected = "Më-×m👌‰Œœ‡TzdDEyMt120=".getBytes("WINDOWS-1252"); //String containing utf-8 characters

		PipeRunResult decodeResult = doPipe(pipe, encodedString, session);
		assertEquals(new String(expected, "UTF-8"), decodeResult.getResult().asString());
	}

	@Test
	public void correctEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.configure();
		pipe.start();
		byte[] inputString = "Më-×m👌‰Œœ‡TzdDEyMt120=".getBytes("UTF-8");
		Message in = new Message(inputString);
		PipeRunResult encodeResult = doPipe(pipe, in, session);

		assertEquals("TcOrLcOXbfCfkYzigLDFksWT4oChVHpkREV5TXQxMjA9", encodeResult.getResult().asString().trim());
	}

	@Test
	public void correctDecoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		String encodedString = "TcOrLcOXbfCfkYzigLDFksWT4oChVHpkREV5TXQxMjA9";
		String decodedString = "Më-×m👌‰Œœ‡TzdDEyMt120=";

		PipeRunResult decodeResult = doPipe(encodedString);
		Message result = decodeResult.getResult();
		assertTrue("Base64 decode defaults to binary data", result.isBinary());
		assertEquals(decodedString, result.asString());
	}

	@Test
	public void encodeConvert2StringTrue() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setConvert2String(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,plainText, session);
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void encodeConvert2StringFalse() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setConvert2String(false);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void decodeConvert2StringTrue() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setConvert2String(true);
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(plainText, result.trim());
	}

	@Test
	public void decodeConvert2StringFalse() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setConvert2String(false);
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(plainText, new String(result).trim());
	}

	//String input encode
	@Test
	public void inputStringOutputString() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,plainText, session);
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void inputStringOutputBytes() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,plainText, session);
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(base64Encoded, new String(result).trim());
	}

	@Test
	public void inputStringOutputStreamEncode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,plainText, session);
		assertFalse(prr.getResult().isBinary());
		Reader result = provideStreamForInput ? prr.getResult().asReader() : (Reader)prr.getResult().asObject();
		assertEquals(base64Encoded, (StreamUtil.readerToString(result, null, false)).trim());
	}

	//String bytes encode
	@Test
	public void inputBytesOutputString() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void inputBytesOutputBytes() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(base64Encoded, new String(result).trim());
	}

	@Test
	public void inputBytesOutputStreamEncode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);
		assertFalse(prr.getResult().isBinary());
		Reader result = provideStreamForInput ? prr.getResult().asReader() : (Reader)prr.getResult().asObject();
		assertEquals(base64Encoded, (StreamUtil.readerToString(result, null, false)).trim());
	}

	//String stream encode
	@Test
	public void inputStreamOutputString() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(plainText.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void inputStreamOutputBytes() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(plainText.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(base64Encoded, new String(result).trim());
	}

	@Test
	public void inputStreamOutputStreamEncode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(plainText.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		assertFalse(prr.getResult().isBinary());
		Reader result = provideStreamForInput ? prr.getResult().asReader() : (Reader)prr.getResult().asObject();
		assertEquals(base64Encoded, (StreamUtil.readerToString(result, null, false)).trim());
	}

	//String input decode
	@Test
	public void inputStringOutputStringDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(plainText, result.trim());
	}

	@Test
	public void inputStringOutputBytesDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(plainText, new String(result).trim());
	}

	@Test
	public void inputStringOutputStreamDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(plainText, (StreamUtil.streamToString(result)).trim());
	}

	//String bytes decode
	@Test
	public void inputBytesOutputStringDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, base64Encoded.getBytes(), session);
		assertFalse(prr.getResult().isBinary());
		String result = prr.getResult().asString();
		assertEquals(plainText, result.trim());
	}

	@Test
	public void inputBytesOutputBytesDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, base64Encoded.getBytes(), session);
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(plainText, new String(result).trim());
	}

	@Test
	public void inputBytesOutputStreamDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, base64Encoded.getBytes(), session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(plainText, (StreamUtil.streamToString(result)).trim());
	}

	//String stream decode
	@Test
	public void inputStreamOutputStringDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(base64Encoded.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		String result = prr.getResult().asString();
		assertEquals(plainText, result.trim());
	}

	@Test
	public void inputStreamOutputBytesDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(base64Encoded.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		assertTrue(prr.getResult().isBinary());
		byte[] result = prr.getResult().asByteArray();
		assertEquals(plainText, new String(result).trim());
	}

	@Test
	public void inputStreamOutputStreamDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.setDirection(Direction.DECODE);
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(base64Encoded.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(plainText, (StreamUtil.streamToString(result)).trim());
	}
}
