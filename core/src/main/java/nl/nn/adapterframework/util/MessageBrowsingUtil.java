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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;

public class MessageBrowsingUtil {
	private static final Logger log = LogUtil.getLogger(MessageBrowsingUtil.class);

	public static String getMessageText(RawMessageWrapper<?> rawMessageWrapper, IListener listener) throws IOException {
		if (rawMessageWrapper == null || rawMessageWrapper.getRawMessage() == null) {
			return null;
		}

		if(rawMessageWrapper instanceof MessageWrapper) {
			MessageWrapper<?> messageWrapper = (MessageWrapper<?>) rawMessageWrapper;
			return messageWrapper.getMessage().asString();
		}
		Object rawMessage = rawMessageWrapper.getRawMessage();
		if(rawMessage instanceof Message) { // For backwards compatibility: earlier MessageLog messages were stored as Message.
			return ((Message)rawMessage).asString();
		} else if(rawMessage instanceof String) { // For backwards compatibility: earlier MessageLog messages were stored as String.
			return (String)rawMessage;
		} else if (listener != null) {
			String msg = null;
			try {
				msg = listener.extractMessage(rawMessageWrapper, new HashMap<>()).asString();
			} catch (Exception e) {
				log.warn(ClassUtils.nameOf(listener) + " cannot extract raw message [" + rawMessageWrapper + "] (" + ClassUtils.nameOf(e) + "): " + e.getMessage(), e);
			}
			if (StringUtils.isEmpty(msg)) {
				msg = Message.asString(rawMessage);
			}
			return msg;
		} else {
			return Message.asString(rawMessage);
		}
	}
}
