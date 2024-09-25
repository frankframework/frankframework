package org.frankframework.ladybug.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

public class TestWriterCaptureWrapper {

	@Test
	void nothingWrittenToBOAS() throws IOException {
		StringWriter writer = new StringWriter();
		WriterCaptureWrapper wrapper = new WriterCaptureWrapper(writer);

		wrapper.close();

		assertEquals(">> Captured writer was closed without being read.", writer.toString());
	}

	@Test
	void somethingWrittenToBOAS() throws IOException {
		StringWriter writer = new StringWriter();
		WriterCaptureWrapper wrapper = new WriterCaptureWrapper(writer);

		wrapper.write("something");
		wrapper.close();

		assertEquals("something", writer.toString());
	}

	@Test
	void emptyWrittenToBOAS() throws IOException {
		StringWriter writer = new StringWriter();
		WriterCaptureWrapper wrapper = new WriterCaptureWrapper(writer);

		wrapper.write("");
		wrapper.close();

		assertEquals("", writer.toString());
	}
}
