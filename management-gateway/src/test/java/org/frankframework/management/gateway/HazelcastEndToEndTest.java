package org.frankframework.management.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.util.SpringRootInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import jakarta.annotation.security.RolesAllowed;

@SpringJUnitConfig(classes = {SpringRootInitializer.class})
@DirtiesContext
public class HazelcastEndToEndTest {

	@Autowired
	private HazelcastOutboundGateway outboundGateway;

	@Autowired
	@Qualifier("frank-management-bus")
	private PublishSubscribeChannel channel;

	private MessageHandler handler;

	@BeforeEach
	public void setup() {
		handler = spy(new MessageHandler() {
			@Override
			@RolesAllowed("IbisTester")
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(BusMessageUtils.hasRole("IbisTester"));
				String request = (String) message.getPayload();
				if("sync-string".equals(request)) {
					Message<String> response = new GenericMessage<>("response-string", new MessageHeaders(null));
					MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
					replyChannel.send(response);
				}
			}
		});
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
		Message<String> request = new GenericMessage<>("should-fail-string", new MessageHeaders(null));
		AuthenticationException ex = assertThrows(AuthenticationException.class, () -> outboundGateway.sendSyncMessage(request));
		assertEquals("no Authentication object found in SecurityContext", ex.getMessage());
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void testSynchronousHazelcastMessage() {
		//Arrange
		ArgumentCaptor<Message<String>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(handler).handleMessage(requestCapture.capture());
		Message<String> request = new GenericMessage<>("sync-string", new MessageHeaders(null));

		//Act
		Message<String> response = outboundGateway.sendSyncMessage(request);

		//Assert
		assertEquals("response-string", response.getPayload());
		Message<String> capturedRequest = requestCapture.getValue();
		assertEquals("sync-string", capturedRequest.getPayload());
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void testAsynchronousHazelcastMessage() {
		//Arrange
		ArgumentCaptor<Message<String>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(handler).handleMessage(requestCapture.capture());
		Message<String> request = new GenericMessage<>("async-string", new MessageHeaders(null));

		//Act
		outboundGateway.sendAsyncMessage(request);

		//Assert
		Message<String> capturedRequest = Awaitility.await()
				.atMost(1500, TimeUnit.MILLISECONDS)
				.until(requestCapture::getValue, Objects::nonNull);
		assertEquals("async-string", capturedRequest.getPayload());
	}

}
