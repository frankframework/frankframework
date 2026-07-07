package org.frankframework.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReplaceNonXmlCharsInputStreamTest {

	public static Stream<Arguments> testReplaceNonXmlCharsInputStream() {
		StringBuilder encoded = new StringBuilder(16);
		encoded.append("hello ");
		encoded.appendCodePoint(0x000C); // non printable Unicode character
		encoded.append(" world.");

		return Stream.of(
				Arguments.of("R", false, "hello \bxyz world.", "hello Rxyz world."),
				Arguments.of("", false, "hello \bxyz world.", "hello xyz world."),
				Arguments.of(null, false, "hello \bxyz world.", "hello xyz world."),
				Arguments.of(null, false, encoded.toString(), "hello  world."),
				Arguments.of("", true, encoded.toString(), "hello  world."),
				Arguments.of("", false, encoded.toString(), "hello  world."),
				Arguments.of(null, true, encoded.toString(), "hello  world."),
				Arguments.of("a", false, encoded.toString(), "hello a world."),
				Arguments.of("a", true, encoded.toString(), "hello a world.")
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testReplaceNonXmlCharsInputStream(String nonXmlReplacementCharacter, boolean allowUnicodeSupplementaryCharacters, String input, String expected) throws Exception {
		try (InputStream ris = new ReplaceNonXmlCharsInputStream(getByteArrayInputStream(input), nonXmlReplacementCharacter, allowUnicodeSupplementaryCharacters)) {
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
