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
package org.frankframework.pipes;

import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.SneakyThrows;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StringUtil;

/**
 * Removes a key specified by <code>{@link #setSessionKey(String) sessionKey}</code>
 * from the {@link PipeLineSession pipeLineSession}.
 *
 * @author Peter Leeuwenburgh
 * @see PipeLineSession
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.SESSION)
public class RemoveFromSession extends FixedForwardPipe {
	private String sessionKey;

	public RemoveFromSession() {
		super.setPreserveInput(true);
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
						if (skResult == null) {
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

	@SneakyThrows(PipeRunException.class) // SneakyThrows because the method is called from a Lambda
	private String objectToString(final Object skResult) {
		try {
			return MessageUtils.asString(skResult);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
	}

	public String getSessionKey() {
		return sessionKey;
	}

	/** name of the key of the entry in the <code>pipelinesession</code> to remove. if this key is empty the input message is interpretted as key. for multiple keys use ',' as delimiter */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
}
