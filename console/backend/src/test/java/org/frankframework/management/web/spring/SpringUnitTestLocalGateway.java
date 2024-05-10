package org.frankframework.management.web.spring;

import java.lang.String;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.frankframework.management.bus.OutboundGateway;

import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

/**
 * Used in the unit tests to just return the input in the correct format to be able to test only the controllers
 *
 * @see "src/test/stubbedBusApplicationContext.xml" for bean definition
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
				TestGatewayMessageResponse.MessageResult messageResult = new TestGatewayMessageResponse.MessageResult(
						getHeaders().get("topic", String.class), getHeaders().get("action", String.class));
				TestGatewayMessageResponse messageResponse = new TestGatewayMessageResponse(messageResult, "SUCCESS", (String) in.getPayload());

				ObjectMapper mapper = new ObjectMapper();
				try {
					return (T) mapper.writeValueAsString(messageResponse);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public MessageHeaders getHeaders() {
				return in.getHeaders();
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
