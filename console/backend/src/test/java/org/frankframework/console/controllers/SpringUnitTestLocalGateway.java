package org.frankframework.console.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.JacksonUtils;

/**
 * Used in the unit tests to just return the input in the correct format to be able to test only the controllers
 *
 * @see WebTestConfiguration
 */
public class SpringUnitTestLocalGateway implements OutboundGateway {

	@Override
	public <I, O> Message<O> sendSyncMessage(Message<I> in) {
		return new Message<>() {
			@SuppressWarnings("unchecked")
			@Override
			public O getPayload() {
				TestGatewayMessageResponse messageResponse = new TestGatewayMessageResponse(
						getHeaders().get("topic", String.class), getHeaders().get("action", String.class)
				);

				return (O) JacksonUtils.convertToJson(messageResponse);
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
	public <I> void sendAsyncMessage(Message<I> in) {
		/*
		 * Can be tested with a Mockito capture:
		 * ArgumentCaptor<Message<String>> requestCapture = ArgumentCaptor.forClass(Message.class);
		 * doCallRealMethod().when(handler).sendAsyncMessage(requestCapture.capture());
		 * Message<String> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		 */
	}

}
