/*
   Copyright 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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
import static org.junit.Assume.assumeFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;
import nl.nn.adapterframework.util.Misc;

public class Base64PipeTest extends StreamingPipeTestBase<Base64Pipe> {

	private String plainText = "Bacon ipsum dolor amet chuck pork loin flank picanha.";
	private String base64Encoded = "QmFjb24gaXBzdW0gZG9sb3IgYW1ldCBjaHVjayBwb3JrIGxvaW4gZmxhbmsgcGljYW5oYS4=";

	@Override
	public Base64Pipe createPipe() {
		return new Base64Pipe();
	}

	@Test(expected = ConfigurationException.class)
	public void noDirection() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setDirection("");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongDirection() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setDirection("not encode");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongOutputType() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("not string or stream or bytes");
		pipe.configure();
	}

	@Test(expected = PipeRunException.class)
	public void wrongInputEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		assumeFalse(provideStreamForInput); // when providing an outputstream, the charset is not used for decoding the input
		pipe.setCharset("test123");
		pipe.configure();
		pipe.start();

		doPipe(pipe,plainText, session);
	}

	@Test(expected = PipeRunException.class)
	public void wrongOutputEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setCharset("test123");
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		doPipe(pipe, base64Encoded, session);
	}

	@Test
	public void wrongEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.configure();
		pipe.start();
		byte[] inputString = "Më-×m👌‰Œœ‡TzdDEyMt120=".getBytes("WINDOWS-1252"); //String containing utf-8 characters
		Message in = new Message(inputString); //Saving it with a different charset
		System.out.println();
		PipeRunResult encodeResult = doPipe(pipe, in, session); //Base64Pipe still works and does as told (convert a string with an incompatible charset)

		assertEquals("Test120/iYych1R6ZERFeU10MTIwPQ==", encodeResult.getResult().asString().trim()); //Unreadable base64 string

		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult decodeResult = doPipe(pipe, encodeResult.getResult(), session);
		assertEquals(new String(in.asByteArray(), "UTF-8"), decodeResult.getResult().asString());
	}

	@Test
	public void correctEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.configure();
		pipe.start();
		byte[] inputString = "Më-×m👌‰Œœ‡TzdDEyMt120=".getBytes("UTF-8");
		Message in = new Message(inputString);
		PipeRunResult encodeResult = doPipe(pipe, in, session);

		assertEquals("TcOrLcOXbfCfkYzigLDFksWT4oChVHpkREV5TXQxMjA9", encodeResult.getResult().asString().trim());

		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult decodeResult = doPipe(pipe, encodeResult.getResult(), session);
		assertEquals(new String(inputString, "UTF-8"), decodeResult.getResult().asString());
	}

	@Test
	public void encodeConvert2StringTrue() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setConvert2String(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,plainText, session);
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void encodeConvert2StringFalse() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setConvert2String(false);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void decodeConvert2StringTrue() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setConvert2String(true);
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		String result = prr.getResult().asString();
		assertEquals(plainText, result.trim());
	}

	@Test
	public void decodeConvert2StringFalse() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setConvert2String(false);
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		byte[] result = (byte[]) prr.getResult().asObject();
		assertEquals(plainText, new String(result).trim());
	}

	//String input encode
	@Test
	public void inputStringOutputString() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,plainText, session);
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void inputStringOutputBytes() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,plainText, session);
		byte[] result = (byte[]) prr.getResult().asObject();
		assertEquals(base64Encoded, new String(result).trim());
	}

	@Test
	public void inputStringOutputStream() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,plainText, session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(base64Encoded, (Misc.streamToString(result)).trim());
	}

	//String bytes encode
	@Test
	public void inputBytesOutputString() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);
		String result = prr.getResult().asString();
		assertEquals(base64Encoded, result.trim());
	}

	@Test
	public void inputBytesOutputBytes() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);
		byte[] result = (byte[]) prr.getResult().asObject();
		assertEquals(base64Encoded, new String(result).trim());
	}

	@Test
	public void inputBytesOutputStream() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, plainText.getBytes(), session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(base64Encoded, (Misc.streamToString(result)).trim());
	}

	//String stream encode
	@Test
	public void inputStreamOutputString() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(plainText.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
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
		byte[] result = (byte[]) prr.getResult().asObject();
		assertEquals(base64Encoded, new String(result).trim());
	}

	@Test
	public void inputStreamOutputStream() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(plainText.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(base64Encoded, (Misc.streamToString(result)).trim());
	}

	//String input decode
	@Test
	public void inputStringOutputStringDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		String result = prr.getResult().asString();
		assertEquals(plainText, result.trim());
	}

	@Test
	public void inputStringOutputBytesDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		byte[] result = (byte[]) prr.getResult().asObject();
		assertEquals(plainText, new String(result).trim());
	}

	@Test
	public void inputStringOutputStreamDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,base64Encoded, session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(plainText, (Misc.streamToString(result)).trim());
	}

	//String bytes decode
	@Test
	public void inputBytesOutputStringDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, base64Encoded.getBytes(), session);
		String result = prr.getResult().asString();
		assertEquals(plainText, result.trim());
	}

	@Test
	public void inputBytesOutputBytesDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("bytes");
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, base64Encoded.getBytes(), session);
		byte[] result = (byte[]) prr.getResult().asObject();
		assertEquals(plainText, new String(result).trim());
	}

	@Test
	public void inputBytesOutputStreamDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, base64Encoded.getBytes(), session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(plainText, (Misc.streamToString(result)).trim());
	}

	//String stream decode
	@Test
	public void inputStreamOutputStringDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("string");
		pipe.setDirection("decode");
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
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(base64Encoded.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		byte[] result = (byte[]) prr.getResult().asObject();
		assertEquals(plainText, new String(result).trim());
	}

	@Test
	public void inputStreamOutputStreamDecode() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setOutputType("stream");
		pipe.setDirection("decode");
		pipe.configure();
		pipe.start();

		InputStream stream = new ByteArrayInputStream(base64Encoded.getBytes());
		PipeRunResult prr = doPipe(pipe, stream, session);
		InputStream result = provideStreamForInput ? prr.getResult().asInputStream() : (InputStream)prr.getResult().asObject();
		assertEquals(plainText, (Misc.streamToString(result)).trim());
	}
}