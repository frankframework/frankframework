package org.frankframework.parameters;

import static org.frankframework.testutil.TestAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.StringReader;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.stream.Message;

class JsonParameterTest {

	public static Stream<Arguments> getValueAsType() {
		return Stream.of(
				arguments(new Message("<root><child>value</child></root>"), """
						{
						  "root": {
						    "child": "value"
						  }
						}
						"""),
				arguments(new Message(new StringReader("""
						<root>
						  <child>value</child>
						</root>
						""")), """
						{
						  "root": {
						    "child": "value"
						  }
						}"""),
				arguments(Message.asMessage("abc"), """
						{"p1": "abc"}"""),
				arguments(Message.asMessage(new StringReader("abc")), """
						{"p1": "abc"}"""),
				arguments(Message.asMessage(1), """
						{"p1": 1}"""),
				arguments(Message.asMessage("1"), """
						{"p1": 1}"""),
				arguments(Message.asMessage(new StringReader("1")), """
						{"p1": 1}""")
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
			return; // "return" statement is needed for the compiler so it knows `message` is in scope later
		}
		assertEquals("json", message.getContext().getMimeType().getSubtype());
		assertJsonEquals(expected, message.asString());
	}
}
