/*
   Copyright 2022-2025 WeAreFrank!

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

import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.AbstractResponseValidatorWrapper;
import org.frankframework.core.IDualModeValidator;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Forward;
import org.frankframework.stream.Message;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;
import org.frankframework.validation.XmlValidatorException;


/**
 * Pipe that validates the input message against a schema.
 *
 * @author Gerrit van Brakel
 */
@Forward(name = "parserError", description = "a parser exception occurred, probably caused by a non-well-formed document. If not specified, <code>failure</code> is used in such a case.")
@Forward(name = "failure", description = "the document is not valid according to the configured schema.")
@Forward(name = "warnings", description = "warnings occurred. If not specified, <code>success</code> is used.")
@Forward(name = "outputParserError", description = "a <code>parserError</code> when validating a response. If not specified, <code>parserError</code> is used.")
@Forward(name = "outputFailure", description = "a <code>failure</code> when validating a response. If not specified, <code>failure</code> is used.")
@Forward(name = "outputWarnings", description = "warnings occurred when validating a response. If not specified, <code>warnings</code> is used.")
public abstract class AbstractValidator extends FixedForwardPipe implements IDualModeValidator {

	private @Getter String schemaSessionKey;
	private @Getter String root;
	private @Getter String responseRoot;
	private @Getter boolean forwardFailureToSuccess = false;

	private final @Getter boolean throwException = false;


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


	protected final PipeForward determineForward(ValidationResult validationResult, boolean responseMode, Supplier<String> errorMessageProvider) throws PipeRunException {
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

	/**
	 * Wrapper for the response validator.
	 */
	public static class ResponseValidatorWrapper extends AbstractResponseValidatorWrapper<AbstractValidator> implements IValidator {
		public ResponseValidatorWrapper(AbstractValidator owner) {
			super(owner);
		}
	}

	/** Session key for retrieving a schema. */
	public void setSchemaSessionKey(String schemaSessionKey) {
		this.schemaSessionKey = schemaSessionKey;
	}

	/** Name of the root element. */
	public void setRoot(String root) {
		this.root = root;
	}
	/** Name of the response root element. */
	public void setResponseRoot(String responseRoot) {
		this.responseRoot = responseRoot;
	}

	/** If {@code true}, the failure forward is replaced by the success forward (like a warning mode). */
	@Deprecated
	@ConfigurationWarning("please specify a forward with name=failure instead")
	public void setForwardFailureToSuccess(boolean b) {
		this.forwardFailureToSuccess = b;
	}
}
