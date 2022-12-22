/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Pipe that increases the integer value of a session variable.
 * Can be used in combination with {@link CompareIntegerPipe} to construct loops.
 *
 * @ff.parameter increment integer value to be added to the session variable
 *
 * @author Richard Punt / Gerrit van Brakel
 */
@ElementType(ElementTypes.SESSION)
public class IncreaseIntegerPipe extends FixedForwardPipe {

	private static final String PARAMETER_INCREMENT = "increment";

	private @Getter String sessionKey=null;
	private @Getter int increment=1;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(sessionKey)) {
			throw new ConfigurationException("sessionKey must be filled");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		String sessionKeyString;
		try {
			sessionKeyString = session.getMessage(sessionKey).asString();
		} catch (IOException e1) {
			throw new PipeRunException(this, "unable to determine sessionkey from pipeline session");
		}
		Integer sessionKeyInteger = Integer.valueOf(sessionKeyString);
		int incrementBy = increment;
		ParameterList pl = getParameterList();
		if(pl != null && pl.size() > 0) {
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
		session.put(sessionKey, sessionKeyInteger.intValue() + incrementBy + "");

		log.debug("stored [{}] in pipeLineSession under key [{}]", sessionKeyString, getSessionKey());
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
