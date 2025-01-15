/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.util;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.core.IListener;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

public class MessageBrowsingUtil {
	private static final Logger log = LogUtil.getLogger(MessageBrowsingUtil.class);

	public static String getMessageText(RawMessageWrapper<?> rawMessageWrapper, IListener listener) throws IOException {
		if (rawMessageWrapper == null || rawMessageWrapper.getRawMessage() == null) {
			return null;
		}

		if (rawMessageWrapper instanceof MessageWrapper messageWrapper) {
			return messageWrapper.getMessage().asString();
		}
		Object rawMessage = rawMessageWrapper.getRawMessage();
		if (rawMessage instanceof Message message) { // For backwards compatibility: earlier MessageLog messages were stored as Message.
			return message.asString();
		} else if (rawMessage instanceof String string) { // For backwards compatibility: earlier MessageLog messages were stored as String.
			return string;
		} else if (listener != null) {
			try {
				String msg = listener.extractMessage(rawMessageWrapper, new HashMap<>()).asString();
				if (StringUtils.isNotEmpty(msg)) {
					return msg;
				}
			} catch (Exception e) {
				log.warn("{} cannot extract raw message [{}] ({}): {}", ClassUtils.nameOf(listener), rawMessageWrapper, ClassUtils.nameOf(e), e.getMessage(), e);
			}
		}

		return MessageUtils.asString(rawMessage);
	}
}
