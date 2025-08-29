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
 * @deprecated please use the {@link IfPipe} for if (else/then) behaviour. If you need regular expressions, see the {@link RegExPipe} as well.
 */
@Forward(name = "*", description = "when {@literal thenForwardName} or {@literal elseForwardName} are used")
@Forward(name = "then", description = "the configured condition is met")
@Forward(name = "else", description = "the configured condition is not met")
@EnterpriseIntegrationPattern(Type.ROUTER)
@Deprecated(since = "9.0.0", forRemoval = true)
public class XmlIf extends IfPipe {
	private String regex = null;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (transformationNeeded()) {
			return super.doPipe(message, session);
		}

		return new PipeRunResult(getForwardForStringInput(message), message);
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


	/**
	 * Regular expression to be applied to the input-message (ignored if either <code>xpathExpression</code> or <code>jsonPathExpression</code> is specified).
	 * The input-message <b>fully</b> matching the given regular expression leads to the 'then'-forward
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "Please use the RegExPipe instead")
	public void setRegex(String regex) {
		this.regex = regex;
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
