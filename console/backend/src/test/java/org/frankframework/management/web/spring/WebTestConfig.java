package org.frankframework.management.web.spring;

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
public class WebTestConfig {
	@Bean
	SpringUnitTestLocalGateway<String> outboundGateway() {
		return new SpringUnitTestLocalGateway<>();
	}

	@Bean
	DefaultMessageBuilderFactory messageBuilderFactory() {
		return new DefaultMessageBuilderFactory();
	}
}
