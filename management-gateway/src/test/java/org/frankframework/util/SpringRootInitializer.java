package org.frankframework.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;

import org.frankframework.management.bus.InboundGatewayFactory;
import org.frankframework.management.bus.OutboundGatewayFactory;
import org.frankframework.management.gateway.HazelcastInboundGateway;
import org.frankframework.management.gateway.HazelcastOutboundGateway;
import org.frankframework.management.security.JwtGeneratorFactoryBean;

/**
 * A minimal Spring ApplicationContext which contains no beans and no configuration.
 * Enables the use of
 * <pre>
 * @SpringJUnitConfig(initializers = {SpringRootInitializer.class})
 * </pre>
 * And
 * <pre>
 * @WithMockUser(...)
 * </pre>
 *
 */
@Configuration
public class SpringRootInitializer {

	@Bean(name = "frank-management-bus")
	public MessageChannel createDefaultChannel() {
		return new PublishSubscribeChannel();// mock(PollableChannel.class);
	}

	@Bean
	public InboundGatewayFactory createInboundGatewayFactory() {
		InboundGatewayFactory factory = new InboundGatewayFactory();
		factory.setGatewayClassnames(HazelcastInboundGateway.class.getCanonicalName());
		return factory;
	}

	@Bean
	public OutboundGatewayFactory createOutboundGatewayFactory() {
		OutboundGatewayFactory factory = new OutboundGatewayFactory();
		factory.setGatewayClassname(HazelcastOutboundGateway.class.getCanonicalName());
		return factory;
	}

	@Bean
	public JwtGeneratorFactoryBean createJwtGeneratorFactoryBean() {
		return new JwtGeneratorFactoryBean();
	}
}
