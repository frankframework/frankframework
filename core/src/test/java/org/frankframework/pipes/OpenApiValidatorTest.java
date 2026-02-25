package org.frankframework.pipes;

import static org.frankframework.core.PipeLineSession.EXIT_CODE_CONTEXT_KEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

public class OpenApiValidatorTest extends PipeTestBase<OpenApiValidator> {

	@Override
	public OpenApiValidator createPipe() {
		return new OpenApiValidator();
	}

	@Test
	void canReadPathAndMethod() {
		pipe.setOpenApiDefinition("/OpenApiValidator/petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("post");

		assertDoesNotThrow(this::configureAndStartPipe);
	}

	static Stream<Arguments> missingMandatoryValue() {
		return Stream.of(
				Arguments.of(true, true, false),
				Arguments.of(true, false, true),
				Arguments.of(false, true, true)
		);
	}

	@ParameterizedTest
	@MethodSource
	void missingMandatoryValue(boolean setOpenApiDefinition, boolean setPath, boolean setMethod) {
		if (setOpenApiDefinition) {
			pipe.setOpenApiDefinition("/OpenApiValidator/petstore-openapi.json");
		}

		if (setPath) {
			pipe.setPath("/pets");
		}

		if (setMethod) {
			pipe.setMethod("post");
		}

		// assert that a ConfigurationException is thrown when trying to configure the pipe without all mandatory values set
		assertThrows(ConfigurationException.class, this::configureAndStartPipe);
	}

	@Test
	void invalidPathGiven() {
		pipe.setOpenApiDefinition("/OpenApiValidator/petstore-openapi.json");
		pipe.setPath("/petsBoem"); // doesn't exist in the OpenAPI definition
		pipe.setMethod("post");

		ConfigurationException configurationException = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertEquals("Exception configuring OpenApiValidator [OpenApiValidator under test]: No schema found for path [/petsBoem]", configurationException.getMessage());
	}

	@Test
	void missingRequestBodySchema() {
		pipe.setOpenApiDefinition("/OpenApiValidator/invalid-petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("delete");

		ConfigurationException configurationException = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertEquals("Exception configuring OpenApiValidator [OpenApiValidator under test]: No schema found for path [/pets] and method [delete]", configurationException.getMessage());
	}

	@Test
	void testInvalidRefInOpenApiDefinition() throws Exception {
		pipe.setOpenApiDefinition("/OpenApiValidator/invalid-petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("post");
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/OpenApiValidator/newPet.json");

		PipeRunException pipeRunException = assertThrows(PipeRunException.class, () -> doPipe(input));
		assertEquals("Pipe [OpenApiValidator under test] Could not validate: Pipe [OpenApiValidator under test] [Malformed escape pair at index 0: %/components/schemas/AsdfPet]",
				pipeRunException.getMessage());
	}

	@Test
	void validNewPet() throws Exception {
		pipe.setOpenApiDefinition("/OpenApiValidator/petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("post");
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/OpenApiValidator/newPet.json");
		PipeRunResult result = doPipe(input);

		assertEquals("success", result.getPipeForward().getName());
	}

	@Test
	void invalidNewPet() throws Exception {
		pipe.setOpenApiDefinition("/OpenApiValidator/petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("post");
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/OpenApiValidator/invalidNewPet.json");
		PipeRunException pipeRunException = assertThrows(PipeRunException.class, () -> doPipe(input));

		assertEquals("Pipe [OpenApiValidator under test] Could not validate: Pipe [OpenApiValidator under test] [$: required property 'name' not found]", pipeRunException.getMessage());
	}

	@Test
	void invalidNewPetWithForwards() throws Exception{
		pipe.setOpenApiDefinition("/OpenApiValidator/petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("post");

		// Define a forward for the failure case, so that instead of throwing an exception, the pipe will forward to "failure" and put the reason in the session
		pipe.addForward(new PipeForward("failure", null));

		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/OpenApiValidator/invalidNewPet.json");
		PipeRunResult result = doPipe(input);

		assertEquals("failure", result.getPipeForward().getName());
		assertEquals("[$: required property 'name' not found]", session.get("failureReason"));
	}

	@Test
	void outputValidatorHappyFlow() throws Exception{
		pipe.setOpenApiDefinition("/OpenApiValidator/petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("post");
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/OpenApiValidator/savedPet.json");
		session.put(EXIT_CODE_CONTEXT_KEY, 200); // set exit code to 200, so that the outputValidator will be used to validate the body

		PipeForward forward = pipe.validate(new Message(input), session, true, null);
		assertEquals("success", forward.getName());
	}

	@Test
	void outputValidatorInvalidResponse() throws Exception{
		pipe.setOpenApiDefinition("/OpenApiValidator/petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("post");

		// Define a forward for the outputFailure case, so that instead of throwing an exception, the pipe will forward to "outputFailure" and put the reason in the session
		pipe.addForward(new PipeForward("outputFailure", null));

		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/OpenApiValidator/invalidNewPet.json");
		session.put(EXIT_CODE_CONTEXT_KEY, 200); // set exit code to 200, so that the outputValidator will be used to validate the body

		PipeForward forward = pipe.validate(new Message(input), session, true, null);

		assertEquals("outputFailure", forward.getName());
		assertEquals("[$: required property 'id' not found, $: required property 'name' not found]", session.get("failureReason"));
	}
}
