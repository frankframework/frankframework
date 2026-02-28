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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import com.networknt.schema.ValidationMessage;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
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

	private OpenApiValidationHelper openApiValidationHelper;
	private @Getter String openApiDefinition;
	private @Getter String path;
	private @Getter String method;
	private @Getter String reasonSessionKey = "failureReason";
	private @Getter boolean useAsOutputValidator;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isAnyEmpty(openApiDefinition, path, method)) {
			throw new ConfigurationException("openApiDefinition, path and method are required");
		}

		try {
			Operation operation = getOperation();

			openApiValidationHelper = new OpenApiValidationHelper(operation);

		} catch (IOException e) {
			throw new ConfigurationException("unable to configure OpenApiValidator", e);
		}
	}

	/**
	 * Tries to load the given OpenApi schema and find the schema for the given path and method. If any of these steps fail, a ConfigurationException is thrown.
	 */
	private @NonNull Operation getOperation() throws ConfigurationException, IOException {
		OpenAPI openApi = readOpenApiDefinition();
		PathItem pathItem = openApi.getPaths().get(path);

		if (pathItem == null) {
			throw new ConfigurationException("No schema found for path [" + path + "]");
		}

		Operation operation = switch (method.toLowerCase()) {
			case "get" -> pathItem.getGet();
			case "post" -> pathItem.getPost();
			case "put" -> pathItem.getPut();
			case "delete" -> pathItem.getDelete();
			default -> throw new ConfigurationException("Method [" + method + "] is not supported. Supported methods are GET, POST, PUT and DELETE");
		};

		if (operation == null) {
			throw new ConfigurationException("No schema found for path [" + path + "] and method [" + method + "]");
		}
		return operation;
	}

	@Override
	protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		try {
			if (messageToValidate.isEmpty()) {
				messageToValidate = new Message("{}");
			}

			SchemaValidationResult result = openApiValidationHelper.validateMessage(messageToValidate, responseMode, session);

			if (StringUtils.isNotEmpty(getReasonSessionKey())) {
				session.put(getReasonSessionKey(), result.validationMessages.toString());
			}

			return determineForward(result.result, responseMode, result.validationMessages::toString);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot validate", e);
		}
	}

	private OpenAPI readOpenApiDefinition() throws IOException {
		String openApiDefinitionPath = getOpenApiDefinition();
		Resource resource = Resource.getResource(this, openApiDefinitionPath);

		if (resource == null) {
			throw new FileNotFoundException("Cannot find OpenAPI definition [" + openApiDefinitionPath + "]");
		}

		// Parse options are set to resolve all $ref in the OpenAPI definition, so that we can validate against the fully resolved schema.
		ParseOptions options = new ParseOptions();
		options.setResolve(true);
		options.setResolveFully(true);

		return new OpenAPIV3Parser()
				.read(openApiDefinitionPath, null, options);
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

	/**
	 * Mark this Validator as configured for mixed validation, meaning it will be used for both request and response validation. Depending solely on the
	 * responseRoot being set is not sufficient, since in case of OpenApiValidator, the responseRoot is not used, but it is still a mixed validator.
	 */
	public void setUseAsOutputValidator(boolean useAsOutputValidator) {
		this.useAsOutputValidator = useAsOutputValidator;
	}

	record SchemaValidationResult(ValidationResult result, Set<ValidationMessage> validationMessages) {
	}
}
