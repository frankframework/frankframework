package org.frankframework.management.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.frankframework.util.SpringRootInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import jakarta.annotation.security.RolesAllowed;

@SpringJUnitConfig(classes = {SpringRootInitializer.class})
public class HazelcastEndToEndTest {

	@Autowired
	private HazelcastOutboundGateway<String> outboundGateway;

	@Autowired
	@Qualifier("frank-management-bus")
	private PublishSubscribeChannel channel;

	private MessageHandler handler;

	@BeforeEach
	public void setup() {
		handler = new MessageHandler() {
			@Override
			@RolesAllowed("IbisTester")
			public void handleMessage(Message<?> message) throws MessagingException {
				Message<String> response = new GenericMessage<>("response-string", new MessageHeaders(null));
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(response);
			}
		};
		channel.subscribe(handler);
	}

	@AfterEach
	public void teardown() {
		if(handler != null) {
			channel.unsubscribe(handler);
		}
	}

	@Test
	public void testHazelcastGatewaysWithoutAuthObject() {
		Message<String> request = new GenericMessage<>("request-string", new MessageHeaders(null));
		AuthenticationException ex = assertThrows(AuthenticationException.class, () -> outboundGateway.sendSyncMessage(request));
		assertEquals("no Authentication object found in SecurityContext", ex.getMessage());
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void testHazelcastGateways() {
		Message<String> request = new GenericMessage<>("request-string", new MessageHeaders(null));
		Message<String> response = outboundGateway.sendSyncMessage(request);

		assertEquals("response-string", response.getPayload());
	}

}
