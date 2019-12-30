/*
   Copyright 2013 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;

/**
 * Puts the input in the PipeLineSession, under the key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>.
 *
 * @author Johan Verrips
 */
public class PutInSession extends FixedForwardPipe {

	private String sessionKey;
	private String value;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getSessionKey())) {
			throw new ConfigurationException(getLogPrefix(null) + "attribute sessionKey must be specified");
		}
	}

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		Object v;
		if (getValue() == null) {
			v = input;
		} else {
			v = value;
		}
		session.put(getSessionKey(), v);
		if (log.isDebugEnabled()) log.debug(getLogPrefix(session) + "stored [" + v + "] in pipeLineSession under key [" + getSessionKey() + "]");
		return new PipeRunResult(getForward(), input);
	}

	@IbisDoc({"1", "Key of the session variable to store the input in", "" })
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
	public String getSessionKey() {
		return sessionKey;
	}

	@IbisDoc({"2", "Value to store in the <code>pipeLineSession</code>. If not set, the input of the pipe is stored", "" })
	public void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}

}
