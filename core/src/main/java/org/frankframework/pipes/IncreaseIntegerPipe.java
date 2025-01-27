/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * Pipe that increases the integer value of a session variable.
 * Can be used in combination with {@link CompareIntegerPipe} to construct loops.
 *
 * @ff.parameter increment integer value to be added to the session variable
 *
 * @author Richard Punt / Gerrit van Brakel
 */
@EnterpriseIntegrationPattern(Type.SESSION)
public class IncreaseIntegerPipe extends FixedForwardPipe {

	private static final String PARAMETER_INCREMENT = "increment";

	private @Getter String sessionKey = null;
	private @Getter int increment = 1;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(sessionKey)) {
			throw new ConfigurationException("sessionKey must be filled");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		Integer sessionKeyInteger = session.getInteger(sessionKey);
		if (sessionKeyInteger == null) {
			throw new PipeRunException(this, "unable to determine sessionkey from pipeline session");
		}
		int incrementBy = increment;
		ParameterList pl = getParameterList();
		if(pl != null && !pl.isEmpty()) {
			try {
				ParameterValueList pvl = pl.getValues(message, session);
				ParameterValue pv = pvl.get(PARAMETER_INCREMENT);
				if(pv != null) {
					incrementBy = pv.asIntegerValue(increment);
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}
		session.put(sessionKey, sessionKeyInteger + incrementBy + "");

		log.debug("stored [{}] in pipeLineSession under key [{}]", sessionKeyInteger + incrementBy, getSessionKey());
		return new PipeRunResult(getSuccessForward(), message);
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey) || sessionKey.equals(getSessionKey());
	}


	/** Reference to the session variable whose value is to be increased
	 * @ff.mandatory
	 */
	public void setSessionKey(String string) {
		sessionKey = string;
	}

	/**
	 * amount to increment the value. Can be set from the attribute or the parameter 'increment'
	 * @ff.default 1
	 */
	public void setIncrement(int i) {
		increment = i;
	}

}
