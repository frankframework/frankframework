/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Forward;
import org.frankframework.doc.Protected;
import org.frankframework.stream.Message;

/**
 * Selects a forward, based on XPath evaluation
 *
 * @author Peter Leeuwenburgh
 * @since 4.3
 * @deprecated please use the {@link IfPipe} for if (else/then) behaviour. If you need regular expresssions, see the @{@link RegExPipe} as well.
 */
@Forward(name = "*", description = "when {@literal thenForwardName} or {@literal elseForwardName} are used")
@Forward(name = "then", description = "the configured condition is met")
@Forward(name = "else", description = "the configured condition is not met")
@EnterpriseIntegrationPattern(Type.ROUTER)
@Deprecated(since = "9.0.0", forRemoval = true)
public class XmlIf extends IfPipe {
	private String sessionKey = null;
	private String regex = null;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Message messageToUse = getMessageToUse(message, session);

		if (transformationNeeded()) {
			return super.doPipe(messageToUse, session);
		}

		return new PipeRunResult(getForwardForStringInput(messageToUse), session);
	}

	/**
	 * Works slightly different compared to super() when using a regex.
	 */
	@Override
	PipeForward getForwardForStringInput(Message message) throws PipeRunException {
		try {
			String inputString = message.asString();

			if (StringUtils.isNotEmpty(regex)) {
				return inputString.matches(regex) ? getThenForward() : getElseForward();
			} else if (StringUtils.isNotEmpty(getExpressionValue())) {
				return inputString.equals(getExpressionValue()) ? getThenForward() : getElseForward();
			}

			// If the input is not empty, use then forward.
			return StringUtils.isNotEmpty(inputString) ? getThenForward() : getElseForward();
		} catch (IOException e) {
			throw new PipeRunException(this, "error reading message");
		}
	}

	private Message getMessageToUse(Message message, PipeLineSession session) throws PipeRunException {
		Optional<String> inputFromSessionKey = getInputFromSessionKey(session);

		if (inputFromSessionKey.isEmpty()) {
			if (Message.isEmpty(message)) {
				return Message.nullMessage();
			}
		} else {
			return new Message(inputFromSessionKey.get());
		}

		return message;
	}

	private Optional<String> getInputFromSessionKey(PipeLineSession session) throws PipeRunException {
		if (StringUtils.isNotEmpty(sessionKey)) {
			log.debug("taking input from sessionKey [{}]", sessionKey);
			String sessionString = session.getString(sessionKey);
			if (sessionString == null) {
				throw new PipeRunException(this, "unable to resolve session key [" + sessionKey + "]");
			}

			return Optional.of(sessionString);
		}

		return Optional.empty();
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey) || sessionKey.equals(this.sessionKey);
	}

	/**
	 * Regular expression to be applied to the input-message (ignored if either <code>xpathExpression</code> or <code>jsonPathExpression</code> is specified).
	 * The input-message <b>fully</b> matching the given regular expression leads to the 'then'-forward
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "Please use the RegExPipe instead")
	public void setRegex(String regex) {
		this.regex = regex;
	}

	/** name of the key in the <code>pipelinesession</code> to retrieve the input-message from. if not set, the current input message of the pipe is taken. n.b. same as <code>getinputfromsessionkey</code> */
	@Deprecated(forRemoval = true, since = "7.7.0")
	@ConfigurationWarning("Please use getInputFromSessionKey instead.")
	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	/**
	 * Hide this method since it should not be able to set this from within this Pipe
	 */
	@Protected
	@Override
	public void setJsonPathExpression(String jsonPathExpression) {
		super.setJsonPathExpression(jsonPathExpression);
	}
}
