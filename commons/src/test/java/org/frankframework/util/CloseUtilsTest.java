package org.frankframework.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import org.junit.jupiter.api.Test;

public class CloseUtilsTest {

	@Test
	public void testDontCloseInputStream() throws IOException {
		InputStream originalStream = mock(InputStream.class);
		try (InputStream dontClose = CloseUtils.dontClose(originalStream)) {
			dontClose.read();
		}

		verify(originalStream, times(0)).close();
	}

	@Test
	public void testDontCloseReader() throws IOException {
		Reader originalReader = mock(Reader.class);
		try (Reader dontClose = CloseUtils.dontClose(originalReader)) {
			dontClose.read();
		}

		verify(originalReader, times(0)).close();
	}

	@Test
	public void testDontCloseOutputStream() throws IOException {
		OutputStream originalStream = mock(OutputStream.class);
		try (OutputStream dontClose = CloseUtils.dontClose(originalStream)) {
			dontClose.write("".getBytes());
		}

		verify(originalStream, times(0)).close();
	}
}
