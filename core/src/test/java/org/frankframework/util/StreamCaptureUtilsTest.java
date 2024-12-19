package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;

class StreamCaptureUtilsTest {

	static class BooleanContainer {
		private boolean value;
		public void setValue(boolean value) {
			this.value = value;
		}
		public boolean getValue() {
			return value;
		}
	}

	@Test
	void assertCaptureUtilsCallsWriteMethod() throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream("Test input".getBytes());

		// We need something effectively final here, we can't use a boolean directly unfortunately.
		BooleanContainer container = new BooleanContainer();

		ByteArrayOutputStream baos = new ByteArrayOutputStream() {
			@Override
			public void write(byte[] b, int off, int len) {
				container.setValue(true);
				super.write(b, off, len);
			}
		};

		InputStream inputStream = StreamCaptureUtils.captureInputStream(bis, baos, 16);
		inputStream.close();

		Assertions.assertTrue(container.getValue());
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

		//D, E, prefix has already been written before the first skip has been called.
		assertEquals("D, E, B, C, E, F, H, J, K, M, N, P, R, S, U, V, X, Z", new String(baos.toByteArray()));
	}

	@Test
	public void testCaptureWithMarkSupportedOutputStream() throws Exception {
		URL input = ClassLoaderUtils.getResourceURL("/ForEachChildElementPipe/bulk2.xml");

		int bufferSize = 2048;
		Message message = new Message(input.openStream()); //non-repeatable
		ByteArrayOutputStream boas = message.captureBinaryStream();
		byte[] magic = message.getMagic(bufferSize);

		message.asString(); //read twice after the magic has been fetched
		message.asString();

		byte[] capture = Arrays.copyOf(boas.toByteArray(), bufferSize); //it is possible more characters have been written to the captured stream
		assertEquals(new String(magic), new String(capture));
		assertEquals(message.asString(), new UrlMessage(input).asString()); //verify that the message output, after reading the magic, has not changed
	}
}
