/*
   Copyright 2026 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.pipes;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;

/**
 * Helper class for OpenApiValidator that contains the logic for resolving the correct schema to validate against based on the operation, path, method and
 * exit code (for response validation), and for performing the actual validation of the message against the resolved schema.
 *
 * @author evandongen
 */
public class OpenApiValidationHelper {
	private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final Operation operation;

	public OpenApiValidationHelper(Operation operation) {
		this.operation = operation;
		// This setting is important to make sure that resolving of references works correctly
		this.objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
	}

	/**
	 * Determine whether to validate as input or output and calls the appropriate method. For response validation, the schema to validate against can be
	 * determined based on the exit code in the session, and the path and method defined in the pipe configuration.
	 */
	OpenApiValidator.SchemaValidationResult validateMessage(Message message, boolean responseMode, PipeLineSession session) throws IOException {
		JsonSchema jsonSchema = resolveJsonSchema(operation, responseMode, session);

		try {
			Set<ValidationMessage> validationMessages = jsonSchema.validate(
					message.asString(), InputFormat.JSON,
					executionContext -> executionContext.getExecutionConfig().setFormatAssertionsEnabled(true)
			);

			ValidationResult result = validationMessages.isEmpty() ? ValidationResult.VALID : ValidationResult.INVALID;

			return new OpenApiValidator.SchemaValidationResult(result, validationMessages);
		} catch (IllegalArgumentException e) {
			return new OpenApiValidator.SchemaValidationResult(
					ValidationResult.PARSER_ERROR, Set.of(ValidationMessage.builder()
					.message(e.getMessage())
					.build())
			);
		}
	}

	private JsonSchema resolveJsonSchema(Operation operation, boolean responseMode, PipeLineSession session) {
		if (responseMode) {
			return getResponseSchema(operation, session.getString(PipeLineSession.EXIT_CODE_CONTEXT_KEY));
		}

		return getRequestSchema(operation);
	}

	/**
	 * Get the response for the given operation and exit code. If no response is defined for the given exit code, look for a default response.
	 * If neither is found, an exception will be thrown when trying to get the schema from the null response.
	 */
	private JsonSchema getResponseSchema(Operation operation, String exitCode) {
		ApiResponse apiResponse = operation.getResponses().get(exitCode);

		if (apiResponse == null) {
			apiResponse = operation.getResponses().get("default");
		}

		return getJsonSchemaFromContent(apiResponse.getContent());
	}

	/**
	 * Gets the request body for the given operation and returns the schema defined for the "application/json" content type.
	 */
	private JsonSchema getRequestSchema(Operation operation) {
		RequestBody requestBody = operation.getRequestBody();

		return getJsonSchemaFromContent(requestBody.getContent());
	}

	private JsonSchema getJsonSchemaFromContent(Content content) {
		Schema<?> schema = content.get("application/json")
				.getSchema();

		JsonNode schemaNode = objectMapper.valueToTree(schema);

		return jsonSchemaFactory.getSchema(schemaNode);
	}
}
