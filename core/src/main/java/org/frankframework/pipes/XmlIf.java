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

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationWarning;
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
 */
@Forward(name = "*", description = "when {@literal thenForwardName} or {@literal elseForwardName} are used")
@Forward(name = "then", description = "the configured condition is met")
@Forward(name = "else", description = "the configured condition is not met")
@EnterpriseIntegrationPattern(Type.ROUTER)
@Deprecated
public class XmlIf extends IfPipe {

	private @Getter String sessionKey = null;
//	private @Getter String regex = null;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException { // TODO regex
		return super.doPipe(getMessageToUse(message, session), session);
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
			log.debug("taking input from sessionKey [{}]", getSessionKey());
			String sessionString = session.getString(getSessionKey());
			if (sessionString == null) {
				throw new PipeRunException(this, "unable to resolve session key [" + getSessionKey() + "]");
			}

			return Optional.of(sessionString);
		}

		return Optional.empty();
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey) || sessionKey.equals(getSessionKey());
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
