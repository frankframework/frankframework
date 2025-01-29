package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.codec.binary.Base64InputStream;
import org.junit.jupiter.api.Test;

import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

class StreamCaptureUtilsTest {

	@Test
	void assertCaptureUtilsCallsWriteMethod() throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream("Test input".getBytes());

		// We need something effectively final here, we can't use a boolean directly unfortunately.
		AtomicBoolean container = new AtomicBoolean();

		ByteArrayOutputStream baos = new ByteArrayOutputStream() {
			@Override
			public void write(byte[] b, int off, int len) {
				container.set(true);
				super.write(b, off, len);
			}
		};

		InputStream inputStream = StreamCaptureUtils.captureInputStream(bis, baos, 16);
		inputStream.close();

		assertTrue(container.get(), "Asserts that the write method was called by the capture utils");
	}

	// This test doesn't really do much, base64 seemed like a great idea but it reads in chunks of 8kb. Our test input is way to small...
	@Test
	void testCaptureBase64InputStream() throws IOException {
		URL testFileURL = TestFileUtils.getTestFileURL("/Message/testString.txt");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		final String base64String;
		try (InputStream inputStream = StreamCaptureUtils.captureInputStream(testFileURL.openStream(), baos, 16)) {
			Base64InputStream base64 = new Base64InputStream(inputStream, true, 76, "\n".getBytes());

			// Read 20 bytes, and then the rest, to trigger a capture close.
			base64String = new String(base64.readNBytes(20)) + new String(base64.readAllBytes());
		}

		String expected = TestFileUtils.getTestFile("/Message/testString.txt");
		assertEquals("""
				PHJvb3Q+PHN1Yj5hYmMmYW1wOyZsdDsmZ3Q7PC9zdWI+PHN1Yj48IVtDREFUQVs8YT5hJmFtcDti
				PC9hPl1dPjwvc3ViPjxkYXRhIGF0dHI9IsOpw6luIOKCrCI+w6nDqW4g4oKsPC9kYXRhPjwvcm9v
				dD4=
				""", base64String);
		String trimmedResult = base64String.replaceAll("\n", "");
		assertEquals(expected, new String(Base64.getDecoder().decode(trimmedResult)));
		assertEquals(expected, new String(baos.toByteArray()));
	}

	@Test
	void testCaptureInputStream() throws IOException {
		URL testFileURL = TestFileUtils.getTestFileURL("/Message/testString.txt");
		AtomicBoolean container = new AtomicBoolean();
		ByteArrayOutputStream baos = new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				container.set(true);
				super.close();
			}
		};

		final String result;
		try (InputStream inputStream = StreamCaptureUtils.captureInputStream(testFileURL.openStream(), baos, 16)) {

			// Read 20 bytes, and then the rest, to trigger a capture close.
			result = new String(inputStream.readNBytes(20)) + new String(inputStream.readAllBytes());
		}

		String expected = TestFileUtils.getTestFile("/Message/testString.txt");
		assertEquals(expected, result);

		String capture = new String(baos.toByteArray());
		assertEquals(20, capture.length());
		assertEquals("<root><sub>abc&amp;&", capture);

		assertTrue(container.get(), "Asserts that the close method was called");
	}

	@Test
	public void testMarkSupportedOutputStream() throws Exception {
		byte[] bytes = "A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z".getBytes(); // 76 chars
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedOutputStream bout = new BufferedOutputStream(baos, 16);
		try (StreamCaptureUtils.MarkCompensatingOutputStream out = new StreamCaptureUtils.MarkCompensatingOutputStream(bout)) {

			out.write(bytes, 9, 6);
			out.flush();
			assertEquals("D, E, ", baos.toString());

			for (int i = 0; i < bytes.length/4; i++) {
				if(i%2==0) {
					out.mark(3);
				}
				out.write(bytes, i*4, 4);
			}
		}

		// D, E, prefix has already been written before the first skip has been called.
		assertEquals("D, E, B, C, E, F, H, J, K, M, N, P, R, S, U, V, X, Z", new String(baos.toByteArray()));
	}

	@Test
	public void testCaptureWithMarkSupportedOutputStream() throws Exception {
		URL input = ClassLoaderUtils.getResourceURL("/ForEachChildElementPipe/bulk2.xml");

		int bufferSize = 2048;
		Message message = new Message(input.openStream()); // non-repeatable
		ByteArrayOutputStream boas = message.captureBinaryStream();
		byte[] magic = message.getMagic(bufferSize);

		message.asString(); // Read twice after the magic has been fetched
		message.asString();

		byte[] capture = Arrays.copyOf(boas.toByteArray(), bufferSize); // It is possible more characters have been written to the captured stream
		assertEquals(new String(magic), new String(capture));
		assertEquals(message.asString(), new String(input.openStream().readAllBytes())); // Verify that the message output, after reading the magic, has not changed
		message.close();
	}

	@Test
	void testCaptureOutputStream() throws IOException {
		AtomicBoolean baosContainer = new AtomicBoolean();
		ByteArrayOutputStream baos = new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				baosContainer.set(true);
				super.close();
			}
		};

		AtomicBoolean captureContainer = new AtomicBoolean();
		ByteArrayOutputStream capture = new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				captureContainer.set(true);
				super.close();
			}
		};

		URL testFileURL = TestFileUtils.getTestFileURL("/Message/testString.txt");
		final int bufferSize = 20; // Read 20 bytes, and then the rest, to trigger a capture close.
		try (InputStream inputStream = testFileURL.openStream(); OutputStream writeToMe = StreamCaptureUtils.captureOutputStream(baos, capture, 16)) {
			byte[] buffer = new byte[bufferSize];
			int read;
			while ((read = inputStream.read(buffer, 0, bufferSize)) >= 0) {
				writeToMe.write(buffer, 0, read);
			}
		}

		String expected = TestFileUtils.getTestFile("/Message/testString.txt");
		String baosString = new String(baos.toByteArray());
		assertEquals(108, baosString.length());
		assertEquals(expected, baosString);
		assertTrue(baosContainer.get(), "Asserts that the close method was called");

		String capturedString = new String(capture.toByteArray());
		assertEquals(bufferSize, capturedString.length());
		assertEquals("<root><sub>abc&amp;&", capturedString);
		assertTrue(captureContainer.get(), "Asserts that the close method was called");
	}
}
