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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.Resource;
import org.frankframework.doc.Category;
import org.frankframework.doc.Mandatory;
import org.frankframework.stream.Message;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;

/**
 * Pipe that validates the input message against an OpenAPI specification.
 *
 * @author evandongen
 */
@Category(Category.Type.BASIC)
public class OpenApiValidator extends AbstractValidator {

	private final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
	private final ObjectMapper mapper = new ObjectMapper();
	private @Getter String openApiDefinition;
	private @Getter String path;
	private @Getter String method;
	private @Getter String reasonSessionKey = "failureReason";
	private JsonSchema openApiSchema;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		// openApiDefinition, path and method are mandatory
		if (StringUtils.isAnyEmpty(openApiDefinition, path, method)) {
			throw new ConfigurationException("openApiDefinition, path and method are required");
		}

		try {
			JsonNode schemaJsonNode = readOpenApiDefinition();
			openApiSchema = getSchema(path, method, schemaJsonNode);
		} catch (JsonSchemaException | IOException e) {
			throw new ConfigurationException("unable to configure OpenApiValidator", e);
		}
	}

	@Override
	protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		try {
			if (messageToValidate.isEmpty()) {
				messageToValidate = new Message("{}");
			}
			SchemaValidationResult result = validateJson(openApiSchema, messageToValidate);

			if (StringUtils.isNotEmpty(getReasonSessionKey())) {
				session.put(getReasonSessionKey(), result.validationMessages.toString());
			}

			return determineForward(result.result, responseMode, result.validationMessages::toString);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot validate", e);
		}
	}

	private JsonSchema getSchema(String path, String method, JsonNode schemaJsonNode) throws ConfigurationException {
		JsonNode operationNode = schemaJsonNode
				.path("paths")
				.path(path)
				.path(method);

		// .path methods will result in missing node instead of null or an exception. Check for that.
		if (operationNode.isMissingNode()) {
			throw new ConfigurationException("No schema found for path [" + path + "] and method [" + method + "]");
		}

		JsonNode schemaNode = operationNode
				.path("requestBody")
				.path("content")
				.path("application/json")
				.path("schema");

		if (schemaNode.isMissingNode()) {
			throw new ConfigurationException("No JSON requestBody schema defined for path [" + path + "] and method [" + method + "]");
		}

		if (schemaNode.has("$ref")) {
			String refValue = schemaNode.get("$ref").asText();

			if (!refValue.startsWith("#/")) {
				throw new ConfigurationException("Only internal refs are supported. Ref [" + refValue + "] does not start with [#/]");
			}

			return jsonSchemaFactory.getSchema(resolveInternalRef(schemaNode.get("$ref").asText(), schemaJsonNode));
		}

		return jsonSchemaFactory.getSchema(operationNode);
	}

	private JsonNode resolveInternalRef(String ref, JsonNode schemaJsonNode) {
		String[] parts = ref.substring(2).split("/");

		JsonNode current = schemaJsonNode;
		for (String part : parts) {
			current = current.path(part);
		}

		return current;
	}

	private SchemaValidationResult validateJson(JsonSchema jsonSchema, Message message) throws IOException {
		try {
			Set<ValidationMessage> validationMessages = jsonSchema.validate(
					message.asString(), InputFormat.JSON,
					executionContext -> executionContext.getExecutionConfig().setFormatAssertionsEnabled(true)
			);

			ValidationResult result = validationMessages.isEmpty() ? ValidationResult.VALID : ValidationResult.INVALID;

			return new SchemaValidationResult(result, validationMessages);
		} catch (IllegalArgumentException e) {
			return new SchemaValidationResult(ValidationResult.PARSER_ERROR, Set.of(ValidationMessage.builder().message(e.getMessage()).build()));
		}
	}

	protected JsonNode readOpenApiDefinition() throws IOException {
		String specPath = getOpenApiDefinition();
		Resource resource = Resource.getResource(this, specPath);

		if (resource == null) {
			throw new FileNotFoundException("Cannot find OpenAPI spec [" + specPath + "]");
		}

		try (InputStream resourceStream = resource.openStream()) {
			return mapper.readTree(resourceStream);
		}
	}

	/**
	 * The OpenAPI specification file to validate against. This should be a JSON file containing the OpenAPI specification.
	 * The file should be available on the classpath.
	 */
	@Mandatory
	public void setOpenApiDefinition(String openApiDefinition) {
		this.openApiDefinition = openApiDefinition;
	}

	/**
	 * Set the path in the OpenAPI specification to validate against. This is used in combination with the method attribute to validate against a specific
	 * operation in the OpenAPI specification.
	 */
	@Mandatory
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Set the HTTP method to validate against. This is used in combination with the path attribute to validate against a specific operation in the
	 * OpenAPI specification.
	 */
	@Mandatory
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * If set: creates a sessionKey to store any errors when validating the json output
	 *
	 * @ff.default failureReason
	 */
	public void setReasonSessionKey(String reasonSessionKey) {
		this.reasonSessionKey = reasonSessionKey;
	}

	record SchemaValidationResult(ValidationResult result, Set<ValidationMessage> validationMessages) {
	}
}
