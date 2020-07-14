/*
message   Copyright 2018 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.AllowAllSecurityHandler;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

/**
 * Puts a security handler, which declares that each role is valid, under a key in the {@link nl.nn.adapterframework.core.IPipeLineSession pipeLineSession}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>key of session variable to store result in</td><td>systemDate</td></tr>
 * </table>
 * </p>
 * @author  Peter Leeuwenburgh
 */
public class PutAllowAllSecurityHandlerInSession extends FixedForwardPipe {
	private String sessionKey = "securityHandler";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		// check the presence of a sessionKey
		if (getSessionKey() == null) {
			throw new ConfigurationException(
					getLogPrefix(null) + "has a null value for sessionKey");
		}
	}

	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		AllowAllSecurityHandler allowAllSecurityHandler = new AllowAllSecurityHandler(false);

		session.put(this.getSessionKey(), allowAllSecurityHandler);

		if (log.isDebugEnabled()) {
			log.debug(
					getLogPrefix(session) + "stored [" + allowAllSecurityHandler
							+ "] in pipeLineSession under key ["
							+ getSessionKey() + "]");
		}

		return new PipeRunResult(getForward(), message);
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
}
