package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.Lombok;

import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;

public class MessageCapturerTest {

	@ParameterizedTest
	@ValueSource(ints = {11, 18, 20, 21, 39, 40, 55, 66})
	void testCaptureDataAsOutputStream(int ladybugMaxLength) throws IOException {
		MessageCapturer capturer = new MessageCapturer(ladybugMaxLength);
		URL testFileURL = getTestFileURL("/testString.txt");
		Message message = new UrlMessage(testFileURL);

		ByteArrayOutputStream capture = new ByteArrayOutputStream();
		InputStream stream = spy(message.asInputStream());
		InputStream capturedStream = capturer.toOutputStream(stream, capture, e -> {}, Lombok::sneakyThrow);
		// No point to verify the read data at this point.
		// Read 20 bytes, in chunks, to trigger a capture close.
		try (capturedStream) {
			int charsRead;
			byte[] buffer = new byte[20];
			while (true) {
				charsRead = capturedStream.read(buffer, 0, 20);
				if (charsRead <= 0) {
					break;
				}
			}
		}

		verify(stream).close();

		String captureString = capture.toString();
		assertEquals(message.asString(), captureString); // Message should be shorter, but is not!
	}

	public static URL getTestFileURL(String file) {
		String normalizedFilename = FilenameUtils.normalize(file, true);
		URL url = MessageCapturerTest.class.getResource(normalizedFilename);
		assertNotNull(url);
		return url;
	}
}
