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
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
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
	public static final String DEFAULT_EXIT_CODE = "200";
	private final SchemaRegistry schemaRegistry;
	private final ObjectMapper objectMapper;
	private final Operation operation;
	private final boolean useAsOutputValidator;

	public OpenApiValidationHelper(Operation operation, boolean useAsOutputValidator) {
		this.operation = operation;
		this.useAsOutputValidator = useAsOutputValidator;
		this.schemaRegistry = createCustomSchemaRegistry();

		// This setting is important to make sure that resolving of references works correctly
		this.objectMapper = new ObjectMapper()
				.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
	}

	private SchemaRegistry createCustomSchemaRegistry() {
		Dialect dialect = Dialect.builder(Dialects.getDraft202012())
				.format(new OpenApiLenientDateTimeFormat())
				.build();

		return SchemaRegistry.withDialect(dialect);
	}

	/**
	 * Determine whether to validate as input or output and calls the appropriate method. For response validation, the schema to validate against can be
	 * determined based on the exit code in the session, and the path and method defined in the pipe configuration.
	 */
	OpenApiValidator.SchemaValidationResult validateMessage(Message message, PipeLineSession session) throws IOException {
		Schema jsonSchema = resolveJsonSchema(operation, session);

		try {
			List<Error> validationMessages = jsonSchema.validate(
					message.asString(), InputFormat.JSON,
					executionContext -> executionContext.executionConfig(builder -> builder.formatAssertionsEnabled(true))
			);

			ValidationResult result = validationMessages.isEmpty() ? ValidationResult.VALID : ValidationResult.INVALID;

			return new OpenApiValidator.SchemaValidationResult(result, validationMessages);
		} catch (IOException | UncheckedIOException e) {
			// Jackson throws UncheckedIOExceptions
			return new OpenApiValidator.SchemaValidationResult(
					ValidationResult.PARSER_ERROR, List.of(Error.builder()
					.message(e.getMessage())
					.build())
			);
		}
	}

	private Schema resolveJsonSchema(Operation operation, PipeLineSession session) {
		if (useAsOutputValidator) {
			return getResponseSchema(operation, session.getString(PipeLineSession.EXIT_CODE_CONTEXT_KEY));
		}

		return getRequestSchema(operation);
	}

	/**
	 * Get the response for the given operation and exit code. If no response is defined for the given exit code, look for a default response.
	 * If neither is found, an exception will be thrown when trying to get the schema from the null response.
	 */
	private Schema getResponseSchema(Operation operation, String exitCode) {
		ApiResponse apiResponse = operation.getResponses().get(StringUtils.defaultIfBlank(exitCode, DEFAULT_EXIT_CODE));

		if (apiResponse == null) {
			apiResponse = operation.getResponses().get("default");
		}

		return getJsonSchemaFromContent(apiResponse.getContent());
	}

	/**
	 * Gets the request body for the given operation and returns the schema defined for the "application/json" content type.
	 */
	private Schema getRequestSchema(Operation operation) {
		RequestBody requestBody = operation.getRequestBody();

		return getJsonSchemaFromContent(requestBody.getContent());
	}

	private Schema getJsonSchemaFromContent(Content content) {
		io.swagger.v3.oas.models.media.Schema<?> schema = null;

		if (content.containsKey("application/json")) {
			 schema = content.get("application/json").getSchema();
		} else if (content.containsKey("*/*")) {
			// try to get */*
			schema = content.get("*/*").getSchema();
		}

		if (schema == null) {
			throw new IllegalStateException("Could not find 'application/json' or '*/*' content type in OpenAPI definition. " +
					"Available content types: %s".formatted(content.keySet()));
		}

		JsonNode schemaNode = objectMapper.valueToTree(schema);

		return schemaRegistry.getSchema(schemaNode);
	}
}
