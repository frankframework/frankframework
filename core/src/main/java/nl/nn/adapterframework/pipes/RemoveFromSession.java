/*
   Copyright 2013, 2019, 2020 Nationale-Nederlanden, 2023 WeAreFrank!

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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StringUtil;

/**
 * Removes a key specified by <code>{@link #setSessionKey(String) sessionKey}</code>
 * from the {@link PipeLineSession pipeLineSession}.
 *
 * @author Peter Leeuwenburgh
 *
 * @see PipeLineSession
 */
@ElementType(ElementTypes.SESSION)
public class RemoveFromSession extends FixedForwardPipe {
	private String sessionKey;

	public RemoveFromSession() {
		super.setPreserveInput(true);
	}

	/**
	 * Checks whether the proper forward is defined.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		/*
		 * if (null== getSessionKey()) { throw new ConfigurationException("Pipe [" +
		 * getName() + "]" + " has a null value for sessionKey"); }
		 */
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result;

		String sessionKeys = getSessionKey();
		if (StringUtils.isEmpty(sessionKeys)) {
			try {
				sessionKeys = message.asString();
			} catch (IOException e) {
				throw new PipeRunException(this, "cannot open stream", e);
			}
		}
		if (StringUtils.isEmpty(sessionKeys)) {
			log.warn("no key specified");
			result = "[null]";
		} else {
			result = StringUtil.splitToStream(sessionKeys)
					.map(sk -> {
						Object skResult = session.remove(sk);
						if (skResult==null) {
							log.warn("key [{}] not found", sk);
							return "[null]";
						} else {
							log.debug("key [{}] removed", sk);
							return objectToString(skResult);
						}
					})
					.collect(Collectors.joining(","));
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	@SneakyThrows
	private String objectToString(final Object skResult) {
		try {
			return Message.asString(skResult);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
	}

	/** name of the key of the entry in the <code>pipelinesession</code> to remove. if this key is empty the input message is interpretted as key. for multiple keys use ',' as delimiter */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
	public String getSessionKey() {
		return sessionKey;
	}
}
