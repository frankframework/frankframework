/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
 * Puts the input or the <code>{@link #setValue(String) value}</code> in the PipeLineSession, under the key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>. Additionally, stores parameter values in the PipeLineSession.
 *
 *
 * @ff.parameters the result of each parameter defined will be we stored in the PipeLineSession, under the key specified by the parameter name
 *
 * @author Johan Verrips
 */
@ElementType(ElementTypes.SESSION)
public class PutInSession extends FixedForwardPipe {

	private @Getter String sessionKey;
	private @Getter String value;

	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if(StringUtils.isNotEmpty(getSessionKey())) {
			Message v;
			if (getValue() == null) {
				try {
					message.preserve();
				} catch (IOException e) {
					throw new PipeRunException(this,"cannot preserve message", e);
				}
				v = message;
			} else {
				v = Message.asMessage(getValue());
			}
			session.put(getSessionKey(), v);
			log.debug("stored [{}] in pipeLineSession under key [{}]", v, getSessionKey());
		}

		ParameterList parameterList = getParameterList();
		if (!parameterList.isEmpty()) {
			try {
				ParameterValueList pvl = parameterList.getValues(message, session);
				if (pvl != null) {
					for(ParameterValue pv : pvl) {
						String name  = pv.getName();
						Object value = pv.getValue();
						session.put(name, value);
						log.debug("stored [{}] in pipeLineSession under key [{}]", value, name);
					}
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
