package org.frankframework.management.web.socket;

import jakarta.annotation.Nonnull;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.management.web.FrankApiBase;
import org.frankframework.management.web.RequestMessageBuilder;
import org.frankframework.util.JacksonUtils;
import org.springframework.messaging.Message;

public class FrankApiWebSocketBase extends FrankApiBase {

	@Nonnull
	protected Message<?> sendSyncMessageWithoutHttp(RequestMessageBuilder input) {
		try {
			Message<?> message = getGateway().sendSyncMessage(input.buildWithoutHttp());
			if (message == null) {
				throw createErrorMessage(input.getTopic(), input.getAction());
			}
//			return convertMessageToDiff(input.getBusMessageName(), message);
			return message;
		} catch (Exception e) {
			return new StringMessage("{\"error\":\"" + e.getMessage() + "\"}");
		}
	}

	protected Message<?> convertMessageToDiff(String busMessageName, Message<?> latestMessage) {
		Message<?> cachedMessage = messageCacheStore.getCachedMessage(busMessageName);

		if (cachedMessage != null) {
			String cachedMessageJson = JacksonUtils.convertToJson(cachedMessage);
			String latestMessageJson = JacksonUtils.convertToJson(latestMessage);
			messageCacheStore.putMessage(busMessageName, latestMessage);

			if (latestMessageJson.length() != cachedMessageJson.length()) {
				String diffPayload = JacksonUtils.difference(cachedMessageJson, latestMessageJson);
				return new StringMessage(diffPayload);
			}
			return new StringMessage("{}");
		}

		messageCacheStore.putMessage(busMessageName, latestMessage);
		return latestMessage;
	}

}
