package org.frankframework.management.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.InboundGatewayFactory;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.OutboundGatewayFactory;
import org.frankframework.management.security.DefaultJwtKeyGenerator;
import org.frankframework.util.CloseUtils;

@Tag("slow")
public class HazelcastMultiNodeEndToEndTest {
	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

	private void setAuthentication(final @NonNull String authority) {
		SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated("user", "password", authorities);
		context.setAuthentication(authentication);
		this.securityContextHolderStrategy.setContext(context);
	}

	@Test
	public void testMultipleNodesHazelcastMessage() throws ParseException {
		// Arrange
		try (AnnotationConfigApplicationContext context1 = new AnnotationConfigApplicationContext(HazelcastOutbound.class)) {
			setAuthentication("ROLE_IbisTester");
			OutboundGateway outboundGateway1 = context1.getBean(OutboundGateway.class);
			Message<String> request = new GenericMessage<>("sync-string", new MessageHeaders(null));

			// Create a 2nd spring context with both an inbound and outbound gateway.
			AnnotationConfigApplicationContext context2 = new AnnotationConfigApplicationContext(HazelcastOutbound.class);
			OutboundGateway outboundGateway2 = context2.getBean(OutboundGateway.class);

			// Assert 2 JWK keys.
			assertEquals(2, getJWKSet(context1).size());
			assertEquals(2, getJWKSet(context2).size());

			// No inbound handler, messages won't be answered.
			BusException e = assertThrows(BusException.class, () -> outboundGateway1.sendSyncMessage(request));
			assertTrue(e.getMessage().startsWith("no response found on temporary reply-queue"));

			// Create the inbound handler.
			AnnotationConfigApplicationContext context3 = new AnnotationConfigApplicationContext(HazelcastInbound.class);

			try (context3) {
				// Assert still 2 JWK keys.
				assertEquals(2, getJWKSet(context1).size());
				assertEquals(2, getJWKSet(context2).size());

				// Act, messages should get an answer now.
				assertEquals("response-string", outboundGateway1.sendSyncMessage(request).getPayload());
				assertEquals("response-string", outboundGateway2.sendSyncMessage(request).getPayload());

				// Close the 2nd outbound gateway.
				context2.close();

				// Assert only 1 JWK should remain, messages should still be answered.
				assertEquals(1, getJWKSet(context1).size());
				assertEquals("response-string", outboundGateway1.sendSyncMessage(request).getPayload());
			}
		}
	}

	@Test
	@SuppressWarnings("java:S2093")
	public void testWithMultipleClusters() throws ParseException {
		AnnotationConfigApplicationContext inbound1 = null;
		AnnotationConfigApplicationContext inbound2 = null;

		AnnotationConfigApplicationContext context1 = null;
		AnnotationConfigApplicationContext context2 = null;
		AnnotationConfigApplicationContext context3 = null;

		try {
			// Arrange
			setAuthentication("ROLE_IbisTester");
			inbound1 = new AnnotationConfigApplicationContext(HazelcastInbound.class);
			Message<String> request = new GenericMessage<>("sync-string", new MessageHeaders(null));

			context1 = new AnnotationConfigApplicationContext(HazelcastOutbound.class);
			OutboundGateway outboundGateway1 = context1.getBean(OutboundGateway.class);
			assertEquals("response-string", outboundGateway1.sendSyncMessage(request).getPayload());

			// Create a 2nd spring context with an outbound gateway.
			context2 = new AnnotationConfigApplicationContext(HazelcastOutbound.class);
			OutboundGateway outboundGateway2 = context2.getBean(OutboundGateway.class);
			assertEquals("response-string", outboundGateway2.sendSyncMessage(request).getPayload());

			// Create a 3rd spring context with an outbound gateway.
			context3 = new AnnotationConfigApplicationContext(HazelcastOutbound.class);
			OutboundGateway outboundGateway3 = context3.getBean(OutboundGateway.class);
			assertEquals("response-string", outboundGateway3.sendSyncMessage(request).getPayload());

			inbound2 = new AnnotationConfigApplicationContext(HazelcastInbound.class);

			// Assert
			assertEquals(3, getJWKSet(context1).size());
			assertEquals(3, getJWKSet(context2).size());
			assertEquals(3, getJWKSet(context3).size());

			assertEquals("response-string", outboundGateway1.sendSyncMessage(request).getPayload());
			assertEquals("response-string", outboundGateway2.sendSyncMessage(request).getPayload());
			assertEquals("response-string", outboundGateway3.sendSyncMessage(request).getPayload());

			context2.close();

			assertEquals("response-string", outboundGateway1.sendSyncMessage(request).getPayload());
			assertEquals("response-string", outboundGateway3.sendSyncMessage(request).getPayload());

			assertEquals(2, getJWKSet(context1).size());

			context3.close();
			inbound1.close();

			// Test that when original inbound is closed, the 2nd will take over
			assertEquals(1, getJWKSet(context1).size());
			assertEquals("response-string", outboundGateway1.sendSyncMessage(request).getPayload());
		} finally {
			// Cleanup
			CloseUtils.closeSilently(inbound1, inbound2, context1, context2, context3);
		}
	}

	private List<JWK> getJWKSet(ApplicationContext context) throws ParseException {
		HazelcastInstance hzInstance = context.getBean("hazelcastOutboundInstance", HazelcastInstance.class);
		IMap<String, String> config = hzInstance.getMap(HazelcastConfig.FRANK_APPLICATION_CONFIG);
		String jwks = config.get(HazelcastConfig.FRANK_APPLICATION_KEYSET);

		if (StringUtils.isEmpty(jwks)) {
			return List.of();
		}
		return JWKSet.parse(jwks).getKeys();
	}

	@Configuration
	public static class HazelcastInbound {

		@Bean(name = "frank-management-bus")
		public MessageChannel createDefaultChannel() {
			return new PublishSubscribeChannel();// mock(PollableChannel.class);
		}

		@Bean
		public MessageHandler createMessageHandler(PublishSubscribeChannel channel) {
			MessageHandler handler = new MessageHandler() {
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
			return handler;
		}

		@Bean
		public InboundGatewayFactory createInboundGatewayFactory() {
			InboundGatewayFactory factory = new InboundGatewayFactory();
			factory.setGatewayClassnames(HazelcastInboundGateway.class.getCanonicalName());
			return factory;
		}
	}

	@Configuration
	public static class HazelcastOutbound {
		@Bean
		public OutboundGatewayFactory createOutboundGatewayFactory() {
			OutboundGatewayFactory factory = new OutboundGatewayFactory();
			factory.setGatewayClassname(HazelcastOutboundGateway.class.getCanonicalName());
			return factory;
		}

		@Bean
		public DefaultJwtKeyGenerator createJwtKeyGenerator() {
			return new DefaultJwtKeyGenerator();
		}
	}
}
