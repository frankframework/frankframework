package org.frankframework.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.testutil.BooleanParameterBuilder;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.testutil.ParameterBuilder;

public class ReplacingParameterVariablesInputStreamTest {

	public static Stream<Arguments> testParamReplacingInputStream() {
		return Stream.of(
				Arguments.of("xyz", "abc", "hello ?{xyz} world.", "hello abc world."),
				Arguments.of("xyz", "", "hello ?{xyz} world.", "hello  world."),
				Arguments.of("xyz", null, "hello ?{xyz} world.", "hello  world."),
				Arguments.of("xyz", null, "hello ?{} world.", "hello  world."),
				Arguments.of("xyz", "abc", "hello ?{abc} world.", "hello  world."),
				Arguments.of("loooooooooooooooooooooooooonger than the input", "", "hello ?{abc} world.", "hello  world."),
				Arguments.of("xyz", "value", "hello ${abc} world.", "hello ${abc} world.")
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testParamReplacingInputStream(String paramName, String paramValue, String input, String expected) throws Exception {
		ParameterList params = new ParameterList();
		params.add(new Parameter(paramName, paramValue));
		params.configure();
		ParameterValueList pvl = params.getValues(Message.nullMessage(), new PipeLineSession());

		try (InputStream ris = new ReplacingParameterVariablesInputStream(getByteArrayInputStream(input), pvl)) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			int b;
			while (-1 != (b = ris.read())) {
				bos.write(b);
			}

			assertEquals(expected, bos.toString());
		}
	}

	@Test
	public void testNoParams() throws Exception {
		ParameterList params = new ParameterList();
		params.configure();
		ParameterValueList pvl = params.getValues(Message.nullMessage(), new PipeLineSession());

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
			new ReplacingParameterVariablesInputStream(getByteArrayInputStream("in"), pvl)
		);
		assertEquals("no parameters provided", e.getMessage());
	}

	// Not all Params are used, some are used twice, and some are typed.
	@Test
	public void testMultipleParamsMatch() throws Exception {
		ParameterList params = new ParameterList();

		params.add(new Parameter("one", "1"));
		params.add(new Parameter("two", "2"));
		params.add(NumberParameterBuilder.create("three", 3));
		params.add(new Parameter("four", "4"));
		params.add(new Parameter("five", "5"));
		params.add(BooleanParameterBuilder.create("yes", true));
		params.add(ParameterBuilder.create().withName("inputstream").withSessionKey("stream"));

		params.configure();
		try (PipeLineSession session = new PipeLineSession()) {
			session.put("stream", new Message("tralalalla").asInputStream());

			ParameterValueList pvl = params.getValues(Message.nullMessage(), session);

			String input = """
					hello ?{five}?{three} world.
					?{one}?{one} skip a few and ?{yes}!
					?{inputstream}""";
			try (InputStream ris = new ReplacingParameterVariablesInputStream(getByteArrayInputStream(input), pvl)) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();

				int b;
				while (-1 != (b = ris.read())) {
					bos.write(b);
				}

				assertEquals("hello 53 world.\n"
						+ "11 skip a few and true!\n"
						+ "tralalalla", bos.toString());
			}
		}
	}

	private ByteArrayInputStream getByteArrayInputStream(String input) {
		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		return new ByteArrayInputStream(bytes);
	}
}
