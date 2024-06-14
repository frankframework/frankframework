package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReplacingVariablesInputStreamTest {

	public static Stream<Arguments> testReplacingVariablesInputStream() {
		return Stream.of(
				Arguments.of("$", Map.of("param", "parameterValue"), "hello ${param} world.", "hello parameterValue world."),
				Arguments.of("?", Map.of("param", "parameterValue"), "hello ${param} / ?{param} world.", "hello ${param} / parameterValue world."),
				Arguments.of("?", Map.of("param", "parameterValue"), "hello ?{param} world.", "hello parameterValue world."),
				Arguments.of("?", Map.of("param", "parameterValue", "param2", "value2"), "hello ?{param} world with an unclosed ?{param.", "hello parameterValue world with an unclosed ?{param.")
		);
	}

	@ParameterizedTest
	@MethodSource
	void testReplacingVariablesInputStream(String prefix, Map<String, String> keyValueMap, String input, String expected) throws IOException {
		ReplacingVariablesInputStream replacingVariablesInputStreams = new ReplacingVariablesInputStream(getByteArrayInputStream(input), prefix, keyValueMap);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		int b;
		while (-1 != (b = replacingVariablesInputStreams.read())) {bos.write(b);}

		assertEquals(expected, bos.toString());
	}

	private ByteArrayInputStream getByteArrayInputStream(String input) {
		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		return new ByteArrayInputStream(bytes);
	}
}
