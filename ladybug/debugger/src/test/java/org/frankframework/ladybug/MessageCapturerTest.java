package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.Lombok;

import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.util.StreamUtil;

public class MessageCapturerTest {

	private MessageCapturer capturer = new MessageCapturer();

	// Similar to StreamCaptureUtilsTest
	@ParameterizedTest
	@ValueSource(ints = {11, 18, 20, 21, 39, 40, 55, 66})
	void testCaptureBinaryDataAsWriter(int ladybugMaxLength) throws IOException {
		URL testFileURL = getTestFileURL("/testString.txt");
		InputStream stream = spy(testFileURL.openStream());
		Message message = spy(new Message(stream));

		StringWriter capture = spy(new StringWriter());
		capturer.setMaxMessageLength(ladybugMaxLength);
		try (Message spyMessaged = capturer.toWriter(message, capture, Lombok::sneakyThrow)) {
			// No point to verify the read data at this point.
			// Read 20 bytes, in chunks, to trigger a capture close.
			try (InputStream str = message.asInputStream()) {
				int charsRead;
				byte[] buffer = new byte[20];
				while (true) {
					charsRead = str.read(buffer, 0, 20);
					if (charsRead <= 0) {
						break;
					}
				}
			}
		}

		verify(message, times(1)).close();
		verify(stream, times(2)).close();
		verify(capture, times(2)).close();

		String expected = new String(testFileURL.openStream().readAllBytes());
		String captureString = capture.toString();
		int maxMessageLength = getExpectedCapturedLength(ladybugMaxLength);
		assertEquals(maxMessageLength, captureString.length(), "expected length ["+maxMessageLength+"], capture text was: "+captureString);
		assertEquals(expected.substring(0, maxMessageLength), captureString);
	}

	/**
	 * This is either 1, 2 or 3 buffers (see StreamUtil#copyStream).
	 * @param ladybugMaxLength Max capture length of the MessageCapturer
	 * @return Expected length, max-size rounded to the nearest buffer-size +1
	 */
	private int getExpectedCapturedLength(int ladybugMaxLength) {
		return (int) (Math.round(ladybugMaxLength / 20) +1) * 20;
	}

	@ParameterizedTest
	@ValueSource(ints = {11, 18, 20, 21, 39, 40, 55, 66})
	void testCaptureBinaryDataAsOutputStream(int ladybugMaxLength) throws IOException {
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

		int maxMessageLength = getExpectedCapturedLength(ladybugMaxLength);
		String captureString = capture.toString();
		assertEquals(maxMessageLength, captureString.length(), "expected length ["+maxMessageLength+"], capture text was: "+captureString);
		assertEquals(expected.substring(0, maxMessageLength), captureString);
	}

	// Similar to StreamCaptureUtilsTest
	@ParameterizedTest
	@ValueSource(ints = {11, 18, 20, 21, 39, 40, 55, 66})
	void testCaptureCharacterDataAsWriter(int ladybugMaxLength) throws IOException {
		URL testFileURL = getTestFileURL("/testString.txt");
		String input = new String(testFileURL.openStream().readAllBytes());
		Message message = spy(new Message(input));

		StringWriter baos = new StringWriter();
		StringWriter capture = spy(new StringWriter());
		capturer.setMaxMessageLength(ladybugMaxLength);
		try (Message spyMessaged = capturer.toWriter(message, capture, Lombok::sneakyThrow)) {
			// Read 20 bytes, and then the rest, to trigger a capture close.
			StreamUtil.copyReaderToWriter(spyMessaged.asReader(), baos, 20);
		}

		assertEquals(input, baos.toString());
		verify(message, times(1)).close();
		verify(capture, times(2)).close();

		int maxMessageLength = getExpectedCapturedLength(ladybugMaxLength);
		String captureString = capture.toString();
		assertEquals(maxMessageLength, captureString.length(), "expected length ["+maxMessageLength+"], capture text was: "+captureString);
		assertEquals(input.substring(0, maxMessageLength), captureString);
	}

	@ParameterizedTest
	@ValueSource(ints = {11, 18, 20, 21, 39, 40, 55, 66})
	void testCaptureCharacterDataAsOutputStream(int ladybugMaxLength) throws IOException {
		URL testFileURL = getTestFileURL("/testString.txt");
		String input = new String(testFileURL.openStream().readAllBytes());
		Message message = spy(new Message(input));

		ByteArrayOutputStream capture = new ByteArrayOutputStream();
		capturer.setMaxMessageLength(ladybugMaxLength);
		try (Message spyMessaged = capturer.toOutputStream(message, capture, e -> {}, Lombok::sneakyThrow)) {
			// No point to verify the read data at this point.
			// Read 20 bytes, in chunks, to trigger a capture close.
			try (Reader str = message.asReader()) {
				int charsRead;
				char[] buffer = new char[20];
				while (true) {
					charsRead = str.read(buffer, 0, 20);
					if (charsRead <= 0) {
						break;
					}
				}
			}
		}

		int maxMessageLength = getExpectedCapturedLength(ladybugMaxLength);
		String captureString = capture.toString();
		assertEquals(maxMessageLength, captureString.length(), "expected length ["+maxMessageLength+"], capture text was: "+captureString);
		assertEquals(input.substring(0, maxMessageLength), captureString);
	}

	@Test
	void testCaptureEmptyMessage() throws IOException {
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
		assertEquals("", captureString);
		assertEquals(0, captureString.length());
	}

	@Test
	void testCaptureEmptyBinaryMessage() throws IOException {
		Message message = new Message(InputStream.nullInputStream());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayOutputStream capture = new ByteArrayOutputStream();
		capturer.setMaxMessageLength(1024);
		try (Message spyMessaged = capturer.toOutputStream(message, capture, e -> {}, Lombok::sneakyThrow)) {
			// Read 20 bytes, and then the rest, to trigger a capture close.
			StreamUtil.copyStream(spyMessaged.asInputStream(), baos, 20);
		}

		assertEquals("", baos.toString());

		String captureString = capture.toString();
		assertEquals("", captureString);
		assertEquals(0, captureString.length());
	}

	@Test
	void testCaptureEmptyMessageButNotRead() throws IOException {
		Message message = new Message("");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringWriter capture = new StringWriter();
		capturer.setMaxMessageLength(1024);

		try (Message spyMessaged = capturer.toWriter(message, capture, Lombok::sneakyThrow)) {
			// Prove that after a message has been closed, but not read, it still captures it's content for the Ladybug.
			assertEquals(-1, spyMessaged.size());
		}

		assertEquals("", baos.toString());

		String captureString = capture.toString();
		assertEquals("", captureString);
		assertEquals(0, captureString.length());
	}

	@Test
	void testCaptureMessageButNotRead() throws IOException {
		URL testFileURL = getTestFileURL("/testString.txt");
		InputStream stream = spy(testFileURL.openStream());
		Message message = spy(new Message(stream));

		StringWriter capture = spy(new StringWriter());
		capturer.setMaxMessageLength(1024);

		try (Message spyMessaged = capturer.toWriter(message, capture, Lombok::sneakyThrow)) {
			// Prove that after a message has been closed, but not read, it still captures it's content for the Ladybug.
			assertEquals(-1, spyMessaged.size());
		}

		String expected = new String(testFileURL.openStream().readAllBytes());
		assertEquals(expected, capture.toString());

		verify(message, times(1)).close();
		verify(stream, times(1)).close();
		verify(capture, times(2)).close();
	}

	@Test
	void testCaptureNullMessageAsWriter() throws IOException {
		Message message = Message.nullMessage();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringWriter capture = new StringWriter();
		capturer.setMaxMessageLength(1024);
		try (Message spyMessaged = capturer.toWriter(message, capture, Lombok::sneakyThrow)) {
			// Read 20 bytes, and then the rest, to trigger a capture close.
			StreamUtil.copyStream(spyMessaged.asInputStream(), baos, 20);
		}

		assertEquals("", baos.toString());

		String captureString = capture.toString();
		assertEquals("", captureString);
		assertEquals(0, captureString.length());
	}

	@Test
	void testCaptureNullMessageAsOutputStream() throws IOException {
		Message message = Message.nullMessage();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayOutputStream capture = new ByteArrayOutputStream();
		capturer.setMaxMessageLength(1024);
		try (Message spyMessaged = capturer.toOutputStream(message, capture, e -> {}, Lombok::sneakyThrow)) {
			// Read 20 bytes, and then the rest, to trigger a capture close.
			StreamUtil.copyStream(spyMessaged.asInputStream(), baos, 20);
		}

		assertEquals("", baos.toString());

		String captureString = capture.toString();
		assertEquals("", captureString);
		assertEquals(0, captureString.length());
	}

	public static URL getTestFileURL(String file) {
		String normalizedFilename = FilenameUtils.normalize(file, true);
		URL url = MessageCapturerTest.class.getResource(normalizedFilename);
		assertNotNull(url);
		return url;
	}
}
