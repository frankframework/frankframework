package org.frankframework.ladybug.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class TestOutputStreamCaptureWrapper {

	@Test
	void nothingWrittenToBAOS() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamCaptureWrapper wrapper = new OutputStreamCaptureWrapper(baos);

		wrapper.close();

		assertEquals(">> Captured stream was closed without being read.", baos.toString());
	}

	@Test
	void somethingWrittenToBAOS() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamCaptureWrapper wrapper = new OutputStreamCaptureWrapper(baos);

		wrapper.write("something".getBytes());
		wrapper.close();

		assertEquals("something", baos.toString());
	}

	@Test
	void emptyWrittenToBAOS() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamCaptureWrapper wrapper = new OutputStreamCaptureWrapper(baos);

		wrapper.write("".getBytes());
		wrapper.close();

		assertEquals("", baos.toString());
	}
}
