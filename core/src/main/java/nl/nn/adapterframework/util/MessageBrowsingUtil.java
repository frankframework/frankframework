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
	private static Logger log = LogUtil.getLogger(MessageBrowsingUtil.class);

	public static String getMessageText(Object rawmsg, IListener listener) throws IOException {
		if (rawmsg == null) {
			return null;
		}

		if(rawmsg instanceof MessageWrapper) {
			MessageWrapper<?> msgsgs = (MessageWrapper<?>) rawmsg;
			return msgsgs.getMessage().asString();
		} else if(rawmsg instanceof Message) { // For backwards compatibility: earlier MessageLog messages were stored as Message.
			return ((Message)rawmsg).asString();
		} else if(rawmsg instanceof String) { // For backwards compatibility: earlier MessageLog messages were stored as String.
			return (String)rawmsg;
		} else if (listener != null) {
			RawMessageWrapper<?> rawMessageWrapper;
			if (rawmsg instanceof RawMessageWrapper<?>) {
				rawMessageWrapper = (RawMessageWrapper<?>) rawmsg;
			} else {
				rawMessageWrapper = new RawMessageWrapper<>(rawmsg);
			}
			String msg = null;
			try {
				msg = listener.extractMessage(rawMessageWrapper, new HashMap<>()).asString();
			} catch (Exception e) {
				log.warn(ClassUtils.nameOf(listener) + " cannot extract raw message [" + rawmsg + "] (" + ClassUtils.nameOf(e) + "): " + e.getMessage(), e);
			}
			if (StringUtils.isEmpty(msg)) {
				msg = Message.asString(rawmsg);
			}
			return msg;
		} else {
			return Message.asString(rawmsg);
		}
	}
}
