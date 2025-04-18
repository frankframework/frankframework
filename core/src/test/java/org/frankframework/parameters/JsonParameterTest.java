package org.frankframework.parameters;

import static org.frankframework.testutil.TestAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.stream.Message;

class JsonParameterTest {

	public static Stream<Arguments> getValueAsType() {
		return Stream.of(
				Arguments.arguments(new Message("<root><child>value</child></root>"), """
						{
						  "root": {
						    "child": "value"
						  }
						}
						""")
		);
	}

	@ParameterizedTest
	@MethodSource
	void getValueAsType(Message input, String expected) throws Exception {

		JsonParameter param = new JsonParameter();
		param.setName("p1");

		Object result = param.getValueAsType(input, false);

		if (!(result instanceof Message message)) {
			fail(expected + " is not a Message");
			return;
		}
		assertEquals("json", message.getContext().getMimeType().getSubtype());
		assertJsonEquals(expected, message.asString());
	}
}
