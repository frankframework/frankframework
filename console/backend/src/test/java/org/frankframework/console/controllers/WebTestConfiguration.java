package org.frankframework.console.controllers;

import org.frankframework.console.configuration.ApiExceptionHandler;
import org.frankframework.console.configuration.ClientSession;
import org.frankframework.console.configuration.WebConfiguration;
import org.frankframework.console.controllers.socket.MessageCacheStore;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.DefaultMessageBuilderFactory;

/**
 * Used in unit tests to create an outboundGateway bean which doesn't rely on http calls but simply returns what we need in the unit tests.
 *
 */
@Configuration
@Import(WebConfiguration.class)
public class WebTestConfiguration {
	@Bean
	MessageCacheStore messageCacheStore() {
		return new MessageCacheStore();
	}

	@Bean
	ClientSession createClientSession() {
		return new ClientSession();
	}

	@Bean
	SpringUnitTestLocalGateway outboundGateway() {
		return Mockito.spy(SpringUnitTestLocalGateway.class);
	}

	@Bean
	DefaultMessageBuilderFactory messageBuilderFactory() {
		return new DefaultMessageBuilderFactory();
	}

	@Bean
	ApiExceptionHandler apiExceptionHandler() {
		return new ApiExceptionHandler();
	}
}
