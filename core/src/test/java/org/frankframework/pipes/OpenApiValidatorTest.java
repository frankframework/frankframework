package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
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
		assertTrue(configurationException.getMessage().endsWith("No schema found for path [/petsBoem] and method [post]"));
	}

	@Test
	void missingRequestBodySchema() {
		pipe.setOpenApiDefinition("/OpenApiValidator/invalid-petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("put");

		ConfigurationException configurationException = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertTrue(configurationException.getMessage().endsWith("No JSON requestBody schema defined for path [/pets] and method [put]"));
	}

	@Test
	void testInvalidRefInOpenApiDefinition() {
		pipe.setOpenApiDefinition("/OpenApiValidator/invalid-petstore-openapi.json");
		pipe.setPath("/pets");
		pipe.setMethod("post");

		ConfigurationException configurationException = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertTrue(configurationException.getMessage().endsWith("Only internal refs are supported. Ref [%/components/schemas/NewPet] does not start with [#/]"));
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

		assertTrue(pipeRunException.getMessage().contains("required property 'name' not found"));
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
		assertTrue(session.get("failureReason").toString().contains("required property 'name' not found"));
	}
}
