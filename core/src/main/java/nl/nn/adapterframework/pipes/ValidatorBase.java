/*
   Copyright 2013, 2015-2017 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IDualModeValidator;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;
import nl.nn.adapterframework.validation.RootValidations;
import nl.nn.adapterframework.validation.XmlValidatorException;


public abstract class ValidatorBase extends FixedForwardPipe implements IDualModeValidator {

//	private @Getter String schema;
//	private @Getter String schemaSessionKey;
	private @Getter String root;
	private @Getter String responseRoot;
	private @Getter boolean forwardFailureToSuccess = false;
	private @Getter String rootElementSessionKey;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		checkSchemaSpecified();

		if (!isForwardFailureToSuccess() && !isThrowException()){
			if (findForward("failure")==null) {
				throw new ConfigurationException("must either set throwException=true or have a forward with name [failure]");
			}
		}

		registerEvent(ValidationResult.PARSER_ERROR.getEvent());
		registerEvent(ValidationResult.INVALID.getEvent());
		registerEvent(ValidationResult.VALID_WITH_WARNINGS.getEvent());
		registerEvent(ValidationResult.VALID.getEvent());
		if (getRoot() == null) {
			ConfigurationWarnings.add(this, log, "root not specified");
		}
	}


	protected abstract void checkSchemaSpecified() throws ConfigurationException;


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
			Message messageToValidate;
			input.preserve();
			if (StringUtils.isNotEmpty(getSoapNamespace())) {
				messageToValidate = getMessageToValidate(input, session);
			} else {
				messageToValidate = input;
			}

			PipeForward forward = validate(messageToValidate, session, responseMode, messageRoot);
			return new PipeRunResult(forward, input);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session), e);
		}

	}

	protected final PipeForward validate(String messageToValidate, PipeLineSession session) throws XmlValidatorException, PipeRunException, ConfigurationException {
		return validate(new Message(messageToValidate), session, false, null);
	}

	protected abstract PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws XmlValidatorException, PipeRunException, ConfigurationException;

	protected RootValidations createRootValidation(String messageRoot) {
		return new RootValidations(messageRoot);
	}

	protected PipeForward determineForward(ValidationResult validationResult, PipeLineSession session, boolean responseMode) throws PipeRunException {
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
						String errorMessage = session.get(getReasonSessionKey(), null);
						if (StringUtils.isEmpty(errorMessage)) {
							errorMessage = session.get(getXmlReasonSessionKey(), "unknown error");
						}
						throw new PipeRunException(this, errorMessage);
					}
				}
				return forward;
			default:
				throw new IllegalStateException("Unknown validationResult ["+validationResult+"]");
		}
	}

	protected PipeRunResult getErrorResult(ValidationResult result, String reason, PipeLineSession session, boolean responseMode) throws PipeRunException {
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			session.put(getReasonSessionKey(), reason);
		}
		PipeForward forward = determineForward(ValidationResult.PARSER_ERROR, session, responseMode);
		return new PipeRunResult(forward, Message.nullMessage());
	}

	@Deprecated
	protected abstract Message getMessageToValidate(Message message, PipeLineSession session);

	protected abstract boolean isConfiguredForMixedValidation();

	public String getMessageRoot(boolean responseMode) {
		return responseMode ? getResponseRoot() : getMessageRoot();
	}



	@Override
	public abstract IValidator getResponseValidator();

	public boolean isMixedValidator(Object outputValidator) {
		return outputValidator==null && isConfiguredForMixedValidation();
	}

	@Deprecated
	public String getSoapNamespace() {
		return null;
	}


	@IbisDoc({"5", "Name of the root element, or a comma separated list of element names. The validation fails if the root element is not present in the list. N.B. for WSDL generation only the first element is used", ""})
	public void setRoot(String root) {
		this.root = root;
	}
	@IbisDoc({"6", "Name of the response root element, or a comma separated list of element names. The validation fails if the root element is not present in the list. N.B. for WSDL generation only the first element is used", ""})
	public void setResponseRoot(String responseRoot) {
		this.responseRoot = responseRoot;
	}

	@IbisDoc({"7", "If set <code>true</code>, the failure forward is replaced by the success forward (like a warning mode)", "false"})
	@Deprecated
	@ConfigurationWarning("please specify a forward with name=failure instead")
	public void setForwardFailureToSuccess(boolean b) {
		this.forwardFailureToSuccess = b;
	}

//	@IbisDocRef({ABSTRACTXMLVALIDATOR})
//	public void setThrowException(boolean throwException) {
//		validator.setThrowException(throwException);
//	}
//	public boolean isThrowException() {
//		return validator.isThrowException();
//	}
//
//	@IbisDocRef({ABSTRACTXMLVALIDATOR})
//	public void setReasonSessionKey(String reasonSessionKey) {
//		validator.setReasonSessionKey(reasonSessionKey);
//	}
//	public String getReasonSessionKey() {
//		return validator.getReasonSessionKey();
//	}
//
//	@IbisDocRef({ABSTRACTXMLVALIDATOR})
//	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
//		validator.setXmlReasonSessionKey(xmlReasonSessionKey);
//	}
//	public String getXmlReasonSessionKey() {
//		return validator.getXmlReasonSessionKey();
//	}
//
//	@IbisDocRef({ABSTRACTXMLVALIDATOR})
//	public void setCharset(String string) {
//		validator.setCharset(string);
//	}
//	public String getCharset() {
//		return  validator.getCharset();
//	}


}