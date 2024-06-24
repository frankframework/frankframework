/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.management.web.socket;

import jakarta.annotation.Nonnull;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.management.web.FrankApiBase;
import org.frankframework.management.web.RequestMessageBuilder;
import org.frankframework.util.JacksonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class FrankApiWebSocketBase extends FrankApiBase {

	@Autowired
	protected SimpMessagingTemplate messagingTemplate;

	@Nonnull
	protected Message<?> sendSyncMessageWithoutHttp(RequestMessageBuilder input) {
		try {
			Message<?> message = getGateway().sendSyncMessage(input.buildWithoutHttp());
			if (message == null) {
				throw createErrorMessage(input.getTopic(), input.getAction());
			}
			return message;
		} catch (Exception e) {
			return new StringMessage("{\"error\":\"" + e.getMessage() + "\"}");
		}
	}

	protected Message<?> convertMessageToDiff(String busMessageName, Message<?> latestMessage) {
		Message<?> cachedMessage = messageCacheStore.getCachedMessage(busMessageName);

		if (cachedMessage != null) {
			messageCacheStore.putMessage(busMessageName, latestMessage);
			String diffPayload = JacksonUtils.difference((String) cachedMessage.getPayload(), (String) latestMessage.getPayload());
			return new StringMessage(diffPayload);
		}
		messageCacheStore.putMessage(busMessageName, latestMessage);
		return latestMessage;
	}

}
