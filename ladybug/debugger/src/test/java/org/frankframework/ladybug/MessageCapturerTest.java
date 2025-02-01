package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.Lombok;

import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.util.StreamUtil;

public class MessageCapturerTest {

	private MessageCapturer capturer;

	@BeforeEach
	public void setup() {
		capturer = new MessageCapturer();
	}

	// Similar to StreamCaptureUtilsTest
	@ParameterizedTest
	@ValueSource(ints = {11, 18, 20, 21, 39, 40, 55, 66})
	void testCaptureWriter(int ladybugMaxLength) throws IOException {
		URL testFileURL = getTestFileURL("/testString.txt");
		InputStream stream = spy(testFileURL.openStream());
		Message message = spy(new Message(stream));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringWriter capture = spy(new StringWriter());
		capturer.setMaxMessageLength(ladybugMaxLength);
		try (Message spyMessaged = capturer.toWriter(message, capture, Lombok::sneakyThrow)) {
			// Read 20 bytes, and then the rest, to trigger a capture close.
			StreamUtil.copyStream(spyMessaged.asInputStream(), baos, 20);
		}

		String expected = new String(testFileURL.openStream().readAllBytes());
		assertEquals(expected, baos.toString());
		verify(message, times(1)).close();
		verify(stream, times(2)).close();
		verify(capture, times(2)).close();

		int maxMessageLength = (int) (Math.round(ladybugMaxLength / 20) +1) * 20; // This is either 1, 2 or 3 buffers (see StreamUtil#copyStream).
		String captureString = capture.toString();
		assertEquals(maxMessageLength, captureString.length(), "expected length ["+maxMessageLength+"], capture text was: "+captureString);
		assertEquals(expected.substring(0, maxMessageLength), captureString);
	}

	@Test
	void testCaptureEmptyWriter() throws IOException {
		Message message = new Message("");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringWriter capture = new StringWriter();
		capturer.setMaxMessageLength(1024);
		try (Message spyMessaged = capturer.toWriter(message, capture, Lombok::sneakyThrow)) {
			// Read 20 bytes, and then the rest, to trigger a capture close.
			StreamUtil.copyStream(spyMessaged.asInputStream(), baos, 20);
		}

		assertEquals("", baos.toString());

		String captureString = capture.toString();
		assertEquals(">> Captured writer was closed without being read.", captureString);
		assertEquals(49, captureString.length());
	}

	@ParameterizedTest
	@ValueSource(ints = {11, 18, 20, 21, 39, 40, 55, 66})
	void testCaptureOutputStream(int ladybugMaxLength) throws IOException {
		URL testFileURL = getTestFileURL("/testString.txt");
		Message message = new UrlMessage(testFileURL);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayOutputStream capture = new ByteArrayOutputStream();
		capturer.setMaxMessageLength(ladybugMaxLength);
		try (Message spyMessaged = capturer.toOutputStream(message, capture, e -> {}, Lombok::sneakyThrow)) {
			// Read 20 bytes, and then the rest, to trigger a capture close.
			StreamUtil.copyStream(spyMessaged.asInputStream(), baos, 20);
		}

		String expected = new String(testFileURL.openStream().readAllBytes());
		assertEquals(expected, baos.toString());

		int maxMessageLength = (int) (Math.round(ladybugMaxLength / 20) +1) * 20; // This is either 1, 2 or 3 buffers (see StreamUtil#copyStream).
		String captureString = capture.toString();
		assertEquals(maxMessageLength, captureString.length(), "expected length ["+maxMessageLength+"], capture text was: "+captureString);
		assertEquals(expected.substring(0, maxMessageLength), captureString);
	}

	@Test
	void testCaptureEmptyOutputStream() throws IOException {
		Message message = new Message(new ByteArrayInputStream("".getBytes()));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayOutputStream capture = new ByteArrayOutputStream();
		capturer.setMaxMessageLength(1024);
		try (Message spyMessaged = capturer.toOutputStream(message, capture, e -> {}, Lombok::sneakyThrow)) {
			// Read 20 bytes, and then the rest, to trigger a capture close.
			StreamUtil.copyStream(spyMessaged.asInputStream(), baos, 20);
		}

		assertEquals("", baos.toString());

		String captureString = capture.toString();
		assertEquals(">> Captured stream was closed without being read.", captureString);
		assertEquals(49, captureString.length());
	}

	public static URL getTestFileURL(String file) {
		String normalizedFilename = FilenameUtils.normalize(file, true);
		URL url = MessageCapturerTest.class.getResource(normalizedFilename);
		assertNotNull(url);
		return url;
	}
}
