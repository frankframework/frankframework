package org.frankframework.ladybug.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class TestOutputStreamCaptureWrapper {

	@Test
	void nothingWrittenToBOAS() throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		OutputStreamCaptureWrapper wrapper = new OutputStreamCaptureWrapper(boas);

		wrapper.close();

		assertEquals(">> Captured stream was closed without being read.", boas.toString());
	}

	@Test
	void somethingWrittenToBOAS() throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		OutputStreamCaptureWrapper wrapper = new OutputStreamCaptureWrapper(boas);

		wrapper.write("something".getBytes());
		wrapper.close();

		assertEquals("something", boas.toString());
	}

	@Test
	void emptyWrittenToBOAS() throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		OutputStreamCaptureWrapper wrapper = new OutputStreamCaptureWrapper(boas);

		wrapper.write("".getBytes());
		wrapper.close();

		assertEquals("", boas.toString());
	}
}
