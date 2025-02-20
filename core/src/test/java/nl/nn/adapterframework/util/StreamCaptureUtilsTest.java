package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.stream.Message;

class StreamCaptureUtilsTest {

	@Test
	public void testMarkSupportedOutputStream() throws Exception {
		byte[] bytes = "A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z".getBytes(); //76 chars
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		BufferedOutputStream bout = new BufferedOutputStream(boas, 16);
		try (StreamCaptureUtils.MarkCompensatingOutputStream out = new StreamCaptureUtils.MarkCompensatingOutputStream(bout)) {

			out.write(bytes, 9, 6);
			out.flush();
			assertEquals("D, E, ", boas.toString());

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

	@Test
	public void testCaptureWithMarkSupportedOutputStream() throws Exception {
		URL input = ClassLoaderUtils.getResourceURL("/ForEachChildElementPipe/bulk2.xml");

		int bufferSize = 2048;
		Message message = new Message(input.openStream()); //non-repeatable
		ByteArrayOutputStream boas = message.captureBinaryStream();
		String magic = message.peek(bufferSize);

		message.asString(); //read twice after the magic has been fetched
		message.asString();

		byte[] capture = Arrays.copyOf(boas.toByteArray(), bufferSize); //it is possible more characters have been written to the captured stream
		assertEquals(magic, new String(capture));
		assertEquals(message.asString(), Message.asMessage(input).asString()); //verify that the message output, after reading the magic, has not changed
	}
}
