package org.frankframework.management.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.util.SpringRootInitializer;

@SpringJUnitConfig(classes = {SpringRootInitializer.class})
@DirtiesContext
public class HazelcastMultiNodeEndToEndTest {
	@Autowired
	private HazelcastOutboundGateway defaultOutboundGateway;

	@Autowired
	@Qualifier("frank-management-bus")
	private PublishSubscribeChannel channel;

	@Autowired
	private ApplicationContext context;

	private MessageHandler handler;

	@BeforeEach
	public void setup() {
		handler = new MessageHandler() {
			@Override
			@RolesAllowed("IbisTester")
			public void handleMessage(@NonNull Message<?> message) throws MessagingException {
				assertTrue(BusMessageUtils.hasRole("IbisTester"));

				Message<String> response = new GenericMessage<>("response-string", new MessageHeaders(null));
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				assertNotNull(replyChannel);
				replyChannel.send(response);
			}
		};
		channel.subscribe(handler);
	}

	@AfterEach
	public void teardown() {
		if(handler != null) {
			channel.unsubscribe(handler);
			channel.destroy();
		}
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void testMultipleNodesHazelcastMessage() throws ParseException {
		// Arrange
		Message<String> request = new GenericMessage<>("sync-string", new MessageHeaders(null));

		// Create a 2nd spring context with both an inbound and outbound gateway.
		AnnotationConfigApplicationContext context2 = new AnnotationConfigApplicationContext(SpringRootInitializer.class);
		OutboundGateway outboundGateway = context2.getBean(OutboundGateway.class);

		// Act ?

		// Assert
		assertEquals(2, getJWKSet(context).size());
		assertEquals(2, getJWKSet(context2).size());

		assertEquals("response-string", outboundGateway.sendSyncMessage(request).getPayload());
		assertEquals("response-string", defaultOutboundGateway.sendSyncMessage(request).getPayload());

		context2.close();

		assertEquals(1, getJWKSet(context).size());
		assertEquals("response-string", defaultOutboundGateway.sendSyncMessage(request).getPayload());
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void testWithMultipleClusters() throws ParseException {
		// Arrange
		Message<String> request = new GenericMessage<>("sync-string", new MessageHeaders(null));

		// Create a 2nd spring context with both an inbound and outbound gateway.
		AnnotationConfigApplicationContext context2 = new AnnotationConfigApplicationContext(SpringRootInitializer.class);
		OutboundGateway outboundGateway2 = context2.getBean(OutboundGateway.class);
		assertEquals("response-string", outboundGateway2.sendSyncMessage(request).getPayload());

		// Create a 3rd spring context with both an inbound and outbound gateway.
		AnnotationConfigApplicationContext context3 = new AnnotationConfigApplicationContext(SpringRootInitializer.class);
		OutboundGateway outboundGateway3 = context3.getBean(OutboundGateway.class);
		assertEquals("response-string", outboundGateway3.sendSyncMessage(request).getPayload());

		// Assert
		assertEquals(3, getJWKSet(context).size());
		assertEquals(3, getJWKSet(context2).size());
		assertEquals(3, getJWKSet(context3).size());

		assertEquals("response-string", outboundGateway2.sendSyncMessage(request).getPayload());
		assertEquals("response-string", outboundGateway3.sendSyncMessage(request).getPayload());
		assertEquals("response-string", defaultOutboundGateway.sendSyncMessage(request).getPayload());

		context2.close();

		assertEquals("response-string", outboundGateway3.sendSyncMessage(request).getPayload());
		assertEquals("response-string", defaultOutboundGateway.sendSyncMessage(request).getPayload());

		assertEquals(2, getJWKSet(context).size());

		context3.close();

		assertEquals(1, getJWKSet(context).size());
		assertEquals("response-string", defaultOutboundGateway.sendSyncMessage(request).getPayload());
	}

	private List<JWK> getJWKSet(ApplicationContext context) throws ParseException {
		HazelcastInstance hzInstance = context.getBean("hazelcastInboundInstance", HazelcastInstance.class);
		IMap<String, String> config = hzInstance.getMap(HazelcastConfig.FRANK_APPLICATION_CONFIG);
		String jwks = config.get(HazelcastConfig.FRANK_APPLICATION_KEYSET);

		if (StringUtils.isEmpty(jwks)) {
			return List.of();
		}
		return JWKSet.parse(jwks).getKeys();
	}
}
