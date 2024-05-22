package org.frankframework.management.web.spring;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.MessageBase;
import org.springframework.http.MediaType;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

/**
 * Used in the unit tests to just return the input in the correct format to be able to test only the controllers
 *
 * @see WebTestConfiguration
 * @param <T>
 */
public class SpringUnitTestLocalGateway<T> extends MessagingGatewaySupport implements OutboundGateway<T> {
	@Override
	protected void onInit() {
	}

	@Override
	public Message<T> sendSyncMessage(Message<T> in) {
		return new Message<>() {
			@Override
			public T getPayload() {
				TestGatewayMessageResponse messageResponse = new TestGatewayMessageResponse(
						getHeaders().get("topic", String.class), getHeaders().get("action", String.class)
				);

				ObjectMapper mapper = new ObjectMapper();
				try {
					return (T) mapper.writeValueAsString(messageResponse);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public MessageHeaders getHeaders() {
				Map<String, Object> headers = new HashMap<>(in.getHeaders());
				headers.put("state", "SUCCESS");
				headers.put("meta-state", "SUCCESS");
				headers.put(MessageBase.MIMETYPE_KEY, MediaType.APPLICATION_JSON_VALUE);
				headers.put("meta-" + MessageBase.MIMETYPE_KEY, MediaType.APPLICATION_JSON_VALUE);

				return new MessageHeaders(headers);
			}
		};
	}

	@Override
	public void sendAsyncMessage(Message<T> in) {
	}

	/* must (re-)throw exceptions and not publish them to a dead-letter-queue. */
	@Override
	public MessageChannel getErrorChannel() {
		return null;
	}
}
