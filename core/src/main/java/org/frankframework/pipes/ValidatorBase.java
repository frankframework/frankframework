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
package org.frankframework.pipes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IDualModeValidator;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.stream.Message;
import org.frankframework.util.Locker;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;
import org.frankframework.validation.XmlValidatorException;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;


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

	/**
	 * Wrapper for the response validator. It has its own name and forwards, but delegates the actual work to the original validator.
	 * It overrides the stop and start method to prevent the original validator from being started and stopped.
	 */
	public static class ResponseValidatorWrapper implements IValidator {

		private @Getter @Setter String name;

		private final Map<String, PipeForward> forwards = new HashMap<>();

		protected ValidatorBase owner;

		public ResponseValidatorWrapper(ValidatorBase owner) {
			super();
			this.owner = owner;
			name = "ResponseValidator of " + owner.getName();
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

		@Override
		public void setPipeLine(PipeLine pipeline) {
			owner.setPipeLine(pipeline);
		}

		@Override
		public void setGetInputFromSessionKey(String string) {
			owner.setGetInputFromSessionKey(string);
		}

		@Override
		public String getGetInputFromSessionKey() {
			return owner.getGetInputFromSessionKey();
		}

		@Override
		public void setGetInputFromFixedValue(String string) {
			owner.setGetInputFromFixedValue(string);
		}

		@Override
		public String getGetInputFromFixedValue() {
			return owner.getGetInputFromFixedValue();
		}

		@Override
		public void setEmptyInputReplacement(String string) {
			owner.setEmptyInputReplacement(string);
		}

		@Override
		public String getEmptyInputReplacement() {
			return owner.getEmptyInputReplacement();
		}

		@Override
		public void setPreserveInput(boolean preserveInput) {
			owner.setPreserveInput(preserveInput);
		}

		@Override
		public boolean isPreserveInput() {
			return owner.isPreserveInput();
		}

		@Override
		public void setStoreResultInSessionKey(String string) {
			owner.setStoreResultInSessionKey(string);
		}

		@Override
		public String getStoreResultInSessionKey() {
			return owner.getStoreResultInSessionKey();
		}

		@Override
		public void setChompCharSize(String string) {
			owner.setChompCharSize(string);
		}

		@Override
		public String getChompCharSize() {
			return owner.getChompCharSize();
		}

		@Override
		public void setElementToMove(String string) {
			owner.setElementToMove(string);
		}

		@Override
		public String getElementToMove() {
			return owner.getElementToMove();
		}

		@Override
		public void setElementToMoveSessionKey(String string) {
			owner.setElementToMoveSessionKey(string);
		}

		@Override
		public String getElementToMoveSessionKey() {
			return owner.getElementToMoveSessionKey();
		}

		@Override
		public void setElementToMoveChain(String string) {
			owner.setElementToMoveChain(string);
		}

		@Override
		public String getElementToMoveChain() {
			return owner.getElementToMoveChain();
		}

		@Override
		public void setRemoveCompactMsgNamespaces(boolean b) {
			owner.setRemoveCompactMsgNamespaces(b);
		}

		@Override
		public boolean isRemoveCompactMsgNamespaces() {
			return owner.isRemoveCompactMsgNamespaces();
		}

		@Override
		public void setRestoreMovedElements(boolean restoreMovedElements) {
			owner.setRestoreMovedElements(restoreMovedElements);
		}

		@Override
		public boolean isRestoreMovedElements() {
			return owner.isRestoreMovedElements();
		}

		@Override
		public void setDurationThreshold(long maxDuration) {
			owner.setDurationThreshold(maxDuration);
		}

		@Override
		public long getDurationThreshold() {
			return owner.getDurationThreshold();
		}

		@Override
		public void setLocker(Locker locker) {
			owner.setLocker(locker);
		}

		@Override
		public Locker getLocker() {
			return owner.getLocker();
		}

		@Override
		public void setWriteToSecLog(boolean b) {
			owner.setWriteToSecLog(b);
		}

		@Override
		public boolean isWriteToSecLog() {
			return owner.isWriteToSecLog();
		}

		@Override
		public void setSecLogSessionKeys(String string) {
			owner.setSecLogSessionKeys(string);
		}

		@Override
		public String getSecLogSessionKeys() {
			return owner.getSecLogSessionKeys();
		}

		@Override
		public void registerEvent(String description) {
			owner.registerEvent(description);
		}

		@Override
		public void throwEvent(String event, Message eventMessage) {
			owner.throwEvent(event, eventMessage);
		}

		@Override
		public boolean sizeStatisticsEnabled() {
			return owner.sizeStatisticsEnabled();
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
