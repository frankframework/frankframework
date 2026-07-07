package org.frankframework.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReplacingInputStreamTest {

	public static Stream<Arguments> testReplacingInputStream() {
		return Stream.of(
				Arguments.of("xyz", "abc", "hello xyz world.", "hello abc world."),
				Arguments.of("xyz", "", "hello xyz world.", "hello  world."),
				Arguments.of("xyz", null, "hello xyz world.", "hello  world."),
				Arguments.of("xyz", "xyzxyz", "hello xyz world.", "hello xyzxyz world.")
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testReplacingInputStream(String search, String replacement, String input, String expected) throws Exception {
		try (ReplacingInputStream ris = new ReplacingInputStream(getByteArrayInputStream(input), search, replacement)) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			int b;
			while (-1 != (b = ris.read())) {
				bos.write(b);
			}

			assertEquals(expected, bos.toString());
		}
	}

	private ByteArrayInputStream getByteArrayInputStream(String input) {
		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		return new ByteArrayInputStream(bytes);
	}
}
