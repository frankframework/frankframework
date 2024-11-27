package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReplacingVariablesInputStreamTest {

	public static Stream<Arguments> testReplacingVariablesInputStream() {
		Properties properties = new Properties();
		properties.put("param", "parameterValue");

		return Stream.of(
				Arguments.of("$", properties, "hello ${param} world.", "hello parameterValue world."),
				Arguments.of("?", properties, "hello ${param} / ?{param} world.", "hello ${param} / parameterValue world."),
				Arguments.of("?", properties, "hello ?{param} world.", "hello parameterValue world."),
				Arguments.of("?", properties, "hello ?{param} world ?{unusedParam}.", "hello parameterValue world ?{unusedParam}."),
				Arguments.of("?", properties, "hello ?{param} world with an unclosed ?{param.", "hello parameterValue world with an unclosed ?{param.")
		);
	}

	@ParameterizedTest
	@MethodSource
	void testReplacingVariablesInputStream(String prefix, Properties properties, String input, String expected) throws IOException {
		ReplacingVariablesInputStream replacingVariablesInputStreams = new ReplacingVariablesInputStream(getByteArrayInputStream(input), prefix, properties);
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
