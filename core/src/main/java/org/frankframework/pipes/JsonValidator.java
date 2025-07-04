/*
   Copyright 2022-2024 WeAreFrank!

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

import com.networknt.schema.InputFormat;
import com.networknt.schema.InvalidSchemaRefException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
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
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;


/**
 * Pipe that validates the input message against a JSON Schema. 
 * @author Gerrit van Brakel
 */
@Category(Category.Type.BASIC)
public class JsonValidator extends AbstractValidator {

	private @Getter String schema;
	//private @Getter String jsonSchemaVersion=null;
	private @Getter String subSchemaPrefix="/definitions/";
	private @Getter String reasonSessionKey = "failureReason";

	private final JsonSchemaFactory service = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

	private JsonSchema jsonSchema;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getSubSchemaPrefix()==null) {
			setSubSchemaPrefix("");
		}
	}

	@Override
	public void start() {
		try {
			super.start();
			jsonSchema = getJsonSchema();
		} catch (IOException e) {
			throw new LifecycleException("unable to start validator", e);
		}
	}

	@Override
	protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		try {
			if (messageToValidate.isEmpty()) {
				messageToValidate = new Message("{}");
			}

			JsonSchema curSchema = jsonSchema;

			if (StringUtils.isEmpty(messageRoot)) {
				messageRoot = responseMode ? getResponseRoot() : getRoot();
			}

			if (StringUtils.isNotEmpty(messageRoot)) {
				log.debug("validation to messageRoot [{}]", messageRoot);

				curSchema = getSubSchema(messageRoot);
			}

			SchemaValidationResult result = validateJson(curSchema, messageToValidate);

			if (StringUtils.isNotEmpty(getReasonSessionKey())) {
				session.put(getReasonSessionKey(), result.validationMessages.toString());
			}

			return determineForward(result.result, responseMode, result.validationMessages::toString);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot validate", e);
		}
	}

	private JsonSchema getSubSchema(String messageRoot) throws PipeRunException {
		try {
			return jsonSchema.getSubSchema(SchemaLocation.Fragment.of(subSchemaPrefix + messageRoot));
		} catch (InvalidSchemaRefException e) {
			throw new PipeRunException(this, "No schema found for ["+getSubSchemaPrefix()+ messageRoot +"]");
		}
	}

	private SchemaValidationResult validateJson(JsonSchema jsonSchema, Message message) throws IOException {
		try {
			Set<ValidationMessage> validationMessages = jsonSchema.validate(
					message.asString(), InputFormat.JSON,
					// By default, since Draft 2019-09 the format keyword only generates annotations and not assertions
					executionContext -> executionContext.getExecutionConfig().setFormatAssertionsEnabled(true)
			);

			return new SchemaValidationResult(
					validationMessages.isEmpty() ? ValidationResult.VALID : ValidationResult.INVALID,
					validationMessages
			);
		} catch (IllegalArgumentException e) {
			return new SchemaValidationResult(ValidationResult.PARSER_ERROR,
					Set.of(ValidationMessage.builder().message(e.getMessage()).build()));
		}
	}

	record SchemaValidationResult(ValidationResult result, Set<ValidationMessage> validationMessages) { }

	protected JsonSchema getJsonSchema() throws IOException {
		String schemaName = getSchema();
		Resource schemaRes = Resource.getResource(this, schemaName);

		if (schemaRes == null) {
			throw new FileNotFoundException("Cannot find schema ["+schemaName+"]");
		}

		return service.getSchema(schemaRes.openStream());
	}

	/** The JSON Schema to validate to */
	@Mandatory
	public void setSchema(String schema) {
		this.schema=schema;
	}

	/**
	 * The subSchemaPrefix is an optional attribute used when referencing a subschema located in a nested structure, such as $defs or definitions. 
	 * Adding only the subSchema attribute is not enough if your schema organizes definitions in nested objects. Add root attribute to complete the process.
	 * @ff.default /definitions/
	 */
	public void setSubSchemaPrefix(String prefix) {
		subSchemaPrefix = prefix;
	}

	/**
	 * If set: creates a sessionKey to store any errors when validating the json output
	 * @ff.default failureReason
	 */
	public void setReasonSessionKey(String reasonSessionKey) {
		this.reasonSessionKey = reasonSessionKey;
	}

}
