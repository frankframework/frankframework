/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.jms;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Protected;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.StringUtil;

/**
 * Send messages to another {@link org.frankframework.core.Adapter}, including message-metadata
 * (the {@link org.frankframework.stream.MessageContext} of the input {@link Message}), and optionally
 * a number of {@link PipeLineSession} keys. This is the JMS-based alternative for the database-based
 * {@link org.frankframework.jdbc.MessageStoreSender}.
 */
public class MessageQueueSender extends JmsSender {

	private @Getter String sessionKeys = "";

	@Override
	public jakarta.jms. @NonNull Message createMessage(@NonNull Session session, @Nullable String correlationID, @NonNull Message message, @Nullable PipeLineSession pipeLineSession) throws JMSException, IOException {
		MessageWrapper<?> wrapper = new MessageWrapper<>(message, null, correlationID);
		if (pipeLineSession != null) {
			wrapper.getContext().putAll(StringUtil.splitToStream(sessionKeys)
					.map(key -> {
						Object v = pipeLineSession.get(key);
						return v == null ? null : Map.entry(key, v);
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
		}

		ObjectMessage objectMessage = session.createObjectMessage(wrapper);
		if (correlationID != null) {
			objectMessage.setJMSCorrelationID(correlationID);
		}
		return objectMessage;
	}

	/**
	 * This sender ignores the Message-Class and always sends messages as JMS {@link ObjectMessage}.
	 */
	@Protected
	@Override
	public void setMessageClass(MessageClass messageClass) {
		super.setMessageClass(messageClass);
	}

	/**
	 * Comma separated list of sessionKey's to be stored together with the message. In the {@link MessageQueueListener},
	 * all these keys and values will be added to the {@link PipeLineSession} on invocation.
	 */
	public void setSessionKeys(String sessionKeys) {
		this.sessionKeys = sessionKeys;
	}
}
