package org.frankframework.ladybug.tibet2;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.DefaultMessageBuilderFactory;

import org.frankframework.management.bus.OutboundGateway;

@Configuration
public class TestBusConfiguration {

	@Bean
	SpringUnitTestLocalGateway outboundGateway() {
		return Mockito.spy(SpringUnitTestLocalGateway.class);
	}
	@Bean
	DefaultMessageBuilderFactory messageBuilderFactory() {
		return new DefaultMessageBuilderFactory();
	}

	@Bean
	Tibet2ToFrameworkDispatcher tibet2ToFrameworkDispatcher(OutboundGateway outboundGateway) {
		return new Tibet2ToFrameworkDispatcher(outboundGateway);
	}
}
