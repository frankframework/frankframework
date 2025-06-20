package org.frankframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

public class JsonValidatorTest extends PipeTestBase<JsonValidator>{

	@Override
	public JsonValidator createPipe() {
		return new JsonValidator();
	}

	public static List<Arguments> testSchemas() {
		return Arrays.asList(new Arguments[]{
				Arguments.of("/Align/FamilyTree/family-compact-family.jsd", "/definitions/"),
				Arguments.of("/Align/FamilyTree/family-compact-family-2020.jsd" , "/$defs/"),
		});
	}

	@MethodSource("testSchemas")
	@ParameterizedTest
	public void basic(String schema, String subschemaPrefix) throws Exception {
		pipe.setSchema(schema);
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/Align/FamilyTree/family-compact.json");
		PipeRunResult result = doPipe(input);

		assertEquals("success", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());
	}

	@MethodSource("testSchemas")
	@ParameterizedTest
	public void basicInvalid(String schema, String subschemaPrefix) throws Exception {
		pipe.setSchema(schema);
		pipe.addForward(new PipeForward("failure", null));
		configureAndStartPipe();

		String input = "{}";
		PipeRunResult result = doPipe(input);

		assertEquals("failure", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());

		String reason = (String)session.get("failureReason");
		assertThat(reason, containsString("required property 'members' not found"));
	}

	@MethodSource("testSchemas")
	@ParameterizedTest
	public void basicNullInput(String schema, String subschemaPrefix) throws Exception {
		pipe.setSchema(schema);
		pipe.addForward(new PipeForward("failure", null));
		configureAndStartPipe();

		String input = null;
		PipeRunResult result = doPipe(input);

		assertEquals("failure", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());

		String reason = (String)session.get("failureReason");
		assertThat(reason, containsString("required property 'members' not found"));
	}

	@MethodSource("testSchemas")
	@ParameterizedTest
	public void parserError(String schema, String subschemaPrefix) throws Exception {
		pipe.setSchema(schema);
		pipe.addForward(new PipeForward("failure", null));
		pipe.addForward(new PipeForward("parserError", null));
		configureAndStartPipe();

		String input = "{asdf";
		PipeRunResult result = doPipe(input);

		assertEquals("parserError", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());

		String reason = (String)session.get("failureReason");
		assertThat(reason, containsString("Invalid input"));
	}

	@MethodSource("testSchemas")
	@ParameterizedTest
	public void basicWithRootElementSpecified(String schema, String subschemaPrefix) throws Exception {
		pipe.setSchema(schema);
		pipe.setSubSchemaPrefix(subschemaPrefix);
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/Align/FamilyTree/family-compact.json");
		PipeRunResult result = pipe.validate(new Message(input), session, "family");

		assertEquals("success", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());
	}

	@MethodSource("testSchemas")
	@ParameterizedTest
	public void overrideRootElement(String schema, String subschemaPrefix) throws Exception {
		pipe.setSchema(schema);
		pipe.setSubSchemaPrefix(subschemaPrefix);
		pipe.addForward(new PipeForward("failure", null));
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/Align/FamilyTree/address.json");
		PipeRunResult result = pipe.validate(new Message(input), session, "address");

		assertEquals("success", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());
	}

	@MethodSource("testSchemas")
	@ParameterizedTest
	public void overrideRootElementInvalid(String schema, String subschemaPrefix) throws Exception {
		pipe.setSchema(schema);
		pipe.setSubSchemaPrefix(subschemaPrefix);
		pipe.setRoot("address");
		pipe.addForward(new PipeForward("failure", null));
		configureAndStartPipe();

		String input = "{}";
		PipeRunResult result = pipe.validate(new Message(input), session, "address");

		assertEquals("failure", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());

		String reason = (String)session.get("failureReason");
		assertThat(reason, containsString("required property 'street' not found"));
	}
}
