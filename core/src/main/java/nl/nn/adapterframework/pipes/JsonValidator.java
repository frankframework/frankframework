/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;
import org.springframework.context.ApplicationContext;

import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IDualModeValidator;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;


/**
 * Pipe that validates the input message against a JSON Schema.
 *
 * @ff.forward parserError a parser exception occurred, probably caused by non-well-formed JSON. If not specified, <code>failure</code> is used in such a case.
 * @ff.forward failure The document is not valid according to the configured schema.
 * @ff.forward warnings warnings occurred. If not specified, <code>success</code> is used.
 * @ff.forward outputParserError a <code>parserError</code> when validating a response. If not specified, <code>parserError</code> is used.
 * @ff.forward outputFailure a <code>failure</code> when validating a response. If not specified, <code>failure</code> is used.
 * @ff.forward outputWarnings warnings occurred when validating a response. If not specified, <code>warnings</code> is used.
 *
 * @author Gerrit van Brakel
 */
@Category("Basic")
public class JsonValidator extends FixedForwardPipe implements IDualModeValidator {

	private @Getter String schemaLocation;
	private @Getter String schemaSessionKey;
	private @Getter String root;
	private @Getter String responseRoot;
	private @Getter boolean forwardFailureToSuccess = false;
	private @Getter String rootElementSessionKey;
	private @Getter String importedSchemaLocationsToIgnore;
	private @Getter boolean useBaseImportedSchemaLocationsToIgnore = false;

	private @Getter boolean throwException = false;
	private @Getter String reasonSessionKey = "failureReason";
	private @Getter String jsonSchemaVersion=null;


	private JsonValidationService service = JsonValidationService.newInstance();
	private JsonSchema schema;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getSchemaLocation()) && StringUtils.isNotEmpty(getSchemaSessionKey())) {
			throw new ConfigurationException("cannot have schemaSessionKey together with schemaLocation");
		}
		checkSchemaSpecified();

//		if (!isForwardFailureToSuccess() && !isThrowException()){
//			if (findForward("failure")==null) {
//				throw new ConfigurationException("must either set throwException=true or have a forward with name [failure]");
//			}
//		}

		registerEvent(ValidationResult.PARSER_ERROR.getEvent());
		registerEvent(ValidationResult.INVALID.getEvent());
		registerEvent(ValidationResult.VALID_WITH_WARNINGS.getEvent());
		registerEvent(ValidationResult.VALID.getEvent());
		if (getRoot() == null) {
			ConfigurationWarnings.add(this, log, "root not specified");
		}
	}

	@Override
	public void start() throws PipeStartException {
		try {
			super.start();
			schema = getSchema();
		} catch (IOException e) {
			throw new PipeStartException("unable to start validator", e);
		}
	}

	@Override
	public void stop() {
		super.stop();
	}

	protected void checkSchemaSpecified() throws ConfigurationException {
		if (StringUtils.isEmpty(getSchemaLocation()) && StringUtils.isEmpty(getSchemaSessionKey())) {
			throw new ConfigurationException("must have either schemaSessionKey, schemaLocation");
		}
	}

	@Override
	public final PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		return doPipe(message, session, false, null);
	}

	@Override
	public PipeRunResult validate(Message message, PipeLineSession session, String messageRoot) throws PipeRunException {
		return doPipe(message, session, false, messageRoot);
	}

	public PipeRunResult doPipe(Message input, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		try {
			input.preserve();
			PipeForward forward = validate(input, session, responseMode, messageRoot);
			return new PipeRunResult(forward, input);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session), e);
		}

	}

	protected final PipeForward validate(String messageToValidate, PipeLineSession session) throws PipeRunException {
		return validate(new Message(messageToValidate), session, false, null);
	}

	protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		final List<String> problems = new LinkedList<>();
		// Problem handler which will print problems found.
		ProblemHandler handler = service.createProblemPrinter(problems::add);
		ValidationResult resultEvent;
		try {
			messageToValidate.preserve();
			JsonSchema curSchema = schema;
			if (StringUtils.isNotEmpty(messageRoot)) {
				curSchema = schema.getSubschemaAt("/definitions/"+messageRoot);
			}
			// Parses the JSON instance by JsonParser
			try (JsonParser parser = service.createParser(messageToValidate.asInputStream(), curSchema, handler)) {
				while (parser.hasNext()) {
					JsonParser.Event event = parser.next();
					// Could do something useful here, like posting the event on a JsonEventHandler.
				}
				resultEvent = problems.isEmpty()? ValidationResult.VALID : ValidationResult.INVALID;
			} catch (JsonParsingException e) {
				resultEvent = ValidationResult.PARSER_ERROR;
				problems.add(e.getMessage());
			}
			if (StringUtils.isNotEmpty(getReasonSessionKey())) {
				session.put(getReasonSessionKey(), problems.toString());
			}
			return determineForward(resultEvent, session, responseMode, problems.toString());
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot validate", e);
		}
	}


	protected PipeForward determineForward(ValidationResult validationResult, PipeLineSession session, boolean responseMode, String errorMessage) throws PipeRunException {
		throwEvent(validationResult.getEvent());
		PipeForward forward = null;
		switch(validationResult) {
			case VALID_WITH_WARNINGS:
				if (responseMode) {
					forward = findForward("outputWarnings");
				}
				if (forward == null) {
					forward = findForward("warnings");
				}
				if (forward == null) {
					forward = getSuccessForward();
				}
				return forward;
			case VALID:
				return getSuccessForward();
			case PARSER_ERROR:
				if (responseMode) {
					forward = findForward("outputParserError");
				}
				if (forward == null) {
					forward = findForward("parserError");
				}
				//$FALL-THROUGH$
			case INVALID:
				if (forward == null) {
					if (responseMode) {
						forward = findForward("outputFailure");
					}
					if (forward == null) {
						forward = findForward("failure");
					}
				}
				if (forward == null) {
					if (isForwardFailureToSuccess()) {
						forward = getSuccessForward();
					} else {
						throw new PipeRunException(this, errorMessage);
					}
				}
				return forward;
			default:
				throw new IllegalStateException("Unknown validationResult ["+validationResult+"]");
		}
	}


	protected JsonSchema getSchema() throws IOException {
		String schemaName = getSchemaLocation();
		Resource schemaRes = Resource.getResource(schemaName);
		return service.readSchema(schemaRes.openStream());
	}


	protected boolean isConfiguredForMixedValidation() {
		return StringUtils.isNotEmpty(responseRoot);
	}

	@Override
	public IValidator getResponseValidator() {
		if (isConfiguredForMixedValidation()) {
			return new ResponseValidatorWrapper(this);
		}
		return null;
	}

	public class ResponseValidatorWrapper implements IValidator {

		private @Getter @Setter String name;

		private Map<String, PipeForward> forwards=new HashMap<String, PipeForward>();

		protected JsonValidator owner;
		public ResponseValidatorWrapper(JsonValidator owner) {
			super();
			this.owner=owner;
			name="ResponseValidator of "+owner.getName();
		}

		@Override
		public void configure() throws ConfigurationException {
		}

		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
			return owner.doPipe(message, session, true, null);
		}

		@Override
		public PipeRunResult validate(Message message, PipeLineSession session, String messageRoot) throws PipeRunException {
			return owner.doPipe(message, session, true, messageRoot);
		}

		@Override
		public int getMaxThreads() {
			return 0;
		}

		@Override
		public Map<String, PipeForward> getForwards() {
			return forwards;
		}

		@Override
		public void registerForward(PipeForward forward) {
			forwards.put(forward.getName(), forward);
		}

		@Override
		public void start() throws PipeStartException {
		}

		@Override
		public void stop() {
		}

		@Override
		public ApplicationContext getApplicationContext() {
			return owner.getApplicationContext();
		}

		@Override
		public ClassLoader getConfigurationClassLoader() {
			return owner.getConfigurationClassLoader();
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			//Can ignore this as it's not set through Spring
		}

		@Override
		public boolean consumesSessionVariable(String sessionKey) {
			return owner.consumesSessionVariable(sessionKey);
		}
	}

	/**
	 * Reference to schema
	 */
	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}

	@IbisDoc({"4", "session key for retrieving a schema", ""})
	public void setSchemaSessionKey(String schemaSessionKey) {
		this.schemaSessionKey = schemaSessionKey;
	}

	@IbisDoc({"5", "Name of the root element, or a comma separated list of element names. The validation fails if the root element is not present in the list. N.B. for WSDL generation only the first element is used", ""})
	public void setRoot(String root) {
		this.root = root;
	}

	@IbisDoc({"7", "If set <code>true</code>, the failure forward is replaced by the success forward (like a warning mode)", "false"})
	@Deprecated
	@ConfigurationWarning("please specify a forward with name=failure instead")
	public void setForwardFailureToSuccess(boolean b) {
		this.forwardFailureToSuccess = b;
	}

//
//	@IbisDocRef({ABSTRACTXMLVALIDATOR})
//	public void setCharset(String string) {
//		validator.setCharset(string);
//	}
//	public String getCharset() {
//		return  validator.getCharset();
//	}



	@IbisDoc({"40", "key of session variable to store the name of the root element",""})
	public void setRootElementSessionKey(String rootElementSessionKey) {
		this.rootElementSessionKey = rootElementSessionKey;
	}

}