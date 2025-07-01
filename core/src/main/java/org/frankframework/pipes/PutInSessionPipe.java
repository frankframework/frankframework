/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * Puts the input or the <code>{@link #setValue(String) value}</code> in the PipeLineSession, under the key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>. Additionally, stores parameter values in the PipeLineSession.
 *
 * @author Johan Verrips
 * @ff.parameters the result of each parameter defined will be we stored in the PipeLineSession, under the key specified by the parameter name
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.SESSION)
public class PutInSessionPipe extends FixedForwardPipe {

	private @Getter String sessionKey;
	private @Getter String value;

	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (StringUtils.isNotEmpty(getSessionKey())) {
			Message v;
			if (getValue() == null) {
				v = message;
			} else {
				v = new Message(getValue());
			}
			session.put(getSessionKey(), v);
			log.debug("stored [{}] in pipeLineSession under key [{}]", v, getSessionKey());
		}

		ParameterList parameterList = getParameterList();
		if (!parameterList.isEmpty()) {
			try {
				ParameterValueList pvl = parameterList.getValues(message, session);
				for (ParameterValue pv : pvl) {
					String name = pv.getName();
					Object paramValue = pv.getValue();
					session.put(name, paramValue);
					log.debug("stored [{}] in pipeLineSession under key [{}]", paramValue, name);
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}

		return new PipeRunResult(getSuccessForward(), message);
	}

	/** Key of the session variable to store the input in */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}

	/** Value to store in the <code>pipeLineSession</code>. If not set, the input of the pipe is stored */
	public void setValue(String value) {
		this.value = value;
	}
}
