package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil.MarkCompensatingOutputStream;

public class StreamUtilTest {

	private String UTF8_EXPECTED="ABC één euro: €1,00";
	private String OTHER_EXPECTED="ABC néé hè";

	public void testReader(String inputFile, String expected) throws IOException {
		testReader(inputFile, expected, null);
	}

	public void testReader(String inputFile, String expected, String defaultCharset) throws IOException {
		URL input = ClassUtils.getResourceURL(inputFile);

		int i;
		InputStream inputStream = input.openStream();
		while( (i=inputStream.read())>=0) {
			System.out.print(Integer.toHexString(i)+" ");
		}

		Reader reader;
		if (defaultCharset==null) {
			reader = StreamUtil.getCharsetDetectingInputStreamReader(input.openStream());
		} else {
			reader = StreamUtil.getCharsetDetectingInputStreamReader(input.openStream(), defaultCharset);
		}

		String actual = StreamUtil.readerToString(reader, null);

		assertEquals(expected, actual);
		reader.close();
	}

	@Test
	public void testInputStreamReaderPlainUTF8() throws IOException {
		testReader("/StreamUtil/inUTF8noBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderUTF8withBOM() throws IOException {
		testReader("/StreamUtil/inUTF8withBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderUTF16LEwithBOM() throws IOException {
		testReader("/StreamUtil/inUTF16LEwithBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderUTF16BEwithBOM() throws IOException {
		testReader("/StreamUtil/inUTF16BEwithBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderAnsi() throws IOException {
		testReader("/StreamUtil/inISO8859-1.bin", OTHER_EXPECTED, "ISO8859-1");
	}

	@Test
	public void testInputStreamReaderUTF8withBOMWrongDefaultCharset() throws IOException {
		testReader("/StreamUtil/inUTF8withBOM.bin", UTF8_EXPECTED, "ISO8859-1");
	}


	public void testStreamToByteArray(String inputFile, boolean skipBOM, String expected, boolean expectBOM) throws IOException {
		URL input = ClassUtils.getResourceURL(inputFile);

		byte[] byteArray = StreamUtil.streamToByteArray(input.openStream(), skipBOM);

		String actual;
		if (expectBOM) {
			assertEquals((byte)0xEF,byteArray[0]);
			assertEquals((byte)0xBB,byteArray[1]);
			assertEquals((byte)0xBF,byteArray[2]);
			actual = new String(byteArray,3, byteArray.length-3, StandardCharsets.UTF_8);
		} else {
			actual = new String(byteArray, StandardCharsets.UTF_8);
		}

		assertEquals(expected, actual);
	}

	@Test
	public void testStreamToByteArrayUTF8noBOM() throws IOException {
		testStreamToByteArray("/StreamUtil/inUTF8noBOM.bin", true, UTF8_EXPECTED, false);
		testStreamToByteArray("/StreamUtil/inUTF8noBOM.bin", false, UTF8_EXPECTED, false);
	}

	@Test
	public void testStreamToByteArrayUTF8withBOM() throws IOException {
		testStreamToByteArray("/StreamUtil/inUTF8withBOM.bin", true, UTF8_EXPECTED, false);
		testStreamToByteArray("/StreamUtil/inUTF8withBOM.bin", false, UTF8_EXPECTED, true);
	}

	@Test
	public void testCaptureWithMarkSupportedOutputStream() throws Exception {
		URL input = ClassUtils.getResourceURL("/ForEachChildElementPipe/bulk2.xml");

		int bufferSize = 2048;
		Message message = new Message(input.openStream()); //non-repeatable
		ByteArrayOutputStream boas = message.captureBinaryStream();
		byte[] magic = message.getMagic(bufferSize);

		message.asString(); //read twice after the magic has been fetched
		message.asString();

		byte[] capture = Arrays.copyOf(boas.toByteArray(), bufferSize); //it is possible more characters have been written to the captured stream
		assertEquals(new String(magic), new String(capture));
		assertEquals(message.asString(), Message.asMessage(input).asString()); //verify that the message output, after reading the magic, has not changed
	}

	@Test
	public void testMarkSupportedOutputStream() throws Exception {
		byte[] bytes = "A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z".getBytes(); //76 chars
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		BufferedOutputStream bout = new BufferedOutputStream(boas, 16);
		try (MarkCompensatingOutputStream out = new MarkCompensatingOutputStream(bout)) {

			out.write(bytes, 9, 6);
			out.flush();
			assertEquals("D, E, ", new String(boas.toByteArray()));

			for (int i = 0; i < bytes.length/4; i++) {
				if(i%2==0) {
					out.mark(3);
				}
				out.write(bytes, i*4, 4);
			}
		}

		//D, E, prefix has already been written before the first skip has been called.
		assertEquals("D, E, B, C, E, F, H, J, K, M, N, P, R, S, U, V, X, Z", new String(boas.toByteArray()));
	}
}
