/*
   Copyright 2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.webcontrol.api.ApiException;

public class MessageBrowsingUtil {
	private static Logger log = LogUtil.getLogger(MessageBrowsingUtil.class);

	public static String getMessageText(Object rawmsg, IListener listener) throws IOException {
		String msg = null;
		if (rawmsg != null) {
			if(rawmsg instanceof MessageWrapper) {
				try {
					MessageWrapper<?> msgsgs = (MessageWrapper<?>) rawmsg;
					msg = msgsgs.getMessage().asString();
				} catch (IOException e) {
					throw new ApiException(e);
				}
			} else if(rawmsg instanceof Message) { // For backwards compatibility: earlier MessageLog messages were stored as Message.
				try {
					msg = ((Message)rawmsg).asString();
				} catch (IOException e) {
					throw new ApiException(e);
				}
			} else if(rawmsg instanceof String) { // For backwards compatibility: earlier MessageLog messages were stored as String.
				msg = (String)rawmsg;
			} else {
				if (listener!=null) {
					try {
						msg = listener.extractMessage(rawmsg, null).asString();
					} catch (Exception e) {
						log.warn(ClassUtils.nameOf(listener)+" cannot extract raw message ["+rawmsg+"] ("+ClassUtils.nameOf(e)+"): "+e.getMessage());
					}
				}
				if (StringUtils.isEmpty(msg)) {
					msg = Message.asString(rawmsg);
				}
			}
		}

		return msg;
	}

}
