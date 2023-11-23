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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IDualModeValidator;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;
import nl.nn.adapterframework.validation.XmlValidatorException;


/**
 * Pipe that validates the input message against a Schema.
 *
 * @ff.forward parserError a parser exception occurred, probably caused by a non-well-formed document. If not specified, <code>failure</code> is used in such a case.
 * @ff.forward failure The document is not valid according to the configured schema.
 * @ff.forward warnings warnings occurred. If not specified, <code>success</code> is used.
 * @ff.forward outputParserError a <code>parserError</code> when validating a response. If not specified, <code>parserError</code> is used.
 * @ff.forward outputFailure a <code>failure</code> when validating a response. If not specified, <code>failure</code> is used.
 * @ff.forward outputWarnings warnings occurred when validating a response. If not specified, <code>warnings</code> is used.
 *
 * @author Gerrit van Brakel
 */
public abstract class ValidatorBase extends FixedForwardPipe implements IDualModeValidator {

	private @Getter String schemaSessionKey;
	private @Getter String root;
	private @Getter String responseRoot;
	private @Getter boolean forwardFailureToSuccess = false;

	private @Getter boolean throwException = false;


	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		registerEvent(ValidationResult.PARSER_ERROR.getEvent());
		registerEvent(ValidationResult.INVALID.getEvent());
		registerEvent(ValidationResult.VALID_WITH_WARNINGS.getEvent());
		registerEvent(ValidationResult.VALID.getEvent());
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
			throw new PipeRunException(this, "Could not validate", e);
		}

	}

	protected final PipeForward validate(String messageToValidate, PipeLineSession session) throws PipeRunException, XmlValidatorException, ConfigurationException {
		return validate(new Message(messageToValidate), session, false, null);
	}

	protected abstract PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException, XmlValidatorException, ConfigurationException;


	protected final PipeForward determineForward(ValidationResult validationResult, PipeLineSession session, boolean responseMode, Supplier<String> errorMessageProvider) throws PipeRunException {
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
						throw new PipeRunException(this, errorMessageProvider.get());
					}
				}
				return forward;
			default:
				throw new IllegalStateException("Unknown validationResult ["+validationResult+"]");
		}
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

	public static class ResponseValidatorWrapper implements IValidator {

		private @Getter @Setter String name;

		private Map<String, PipeForward> forwards=new HashMap<>();

		protected ValidatorBase owner;
		public ResponseValidatorWrapper(ValidatorBase owner) {
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

	/** Session key for retrieving a schema */
	public void setSchemaSessionKey(String schemaSessionKey) {
		this.schemaSessionKey = schemaSessionKey;
	}

	/** Name of the root element */
	public void setRoot(String root) {
		this.root = root;
	}
	/** Name of the response root element */
	public void setResponseRoot(String responseRoot) {
		this.responseRoot = responseRoot;
	}

	/** If set <code>true</code>, the failure forward is replaced by the success forward (like a warning mode) */
	@Deprecated
	@ConfigurationWarning("please specify a forward with name=failure instead")
	public void setForwardFailureToSuccess(boolean b) {
		this.forwardFailureToSuccess = b;
	}
}