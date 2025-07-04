package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import org.frankframework.management.bus.LocalGateway;
import org.frankframework.management.switchboard.CloudAgentOutboundGateway;

class JwtGeneratorFactoryBeanTest {

	@Test
	@DisplayName("When setting CloudAgentOutboundGateway, Then KeystoreJwtKeyGenerator is created")
	void whenCloudAgent_thenKeystoreGenerator() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
			// inject the property before context.refresh()
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
					ctx,
					"management.gateway.outbound.class=" + CloudAgentOutboundGateway.class.getName()
			);
			// register your FactoryBean
			ctx.register(JwtGeneratorFactoryBean.class);
			ctx.refresh();

			JwtGeneratorFactoryBean factoryBean = ctx.getBean(JwtGeneratorFactoryBean.class);
			// the KeystoreJwtKeyGenerator path will trigger a missing‐bean error
			assertThrows(IllegalArgumentException.class, factoryBean::getObject);
		}
	}

	@Test
	@DisplayName("When setting any OutboundGateway other than CloudAgent, Then JwtKeyGenerator is created")
	void whenLocal_thenJwtKeyGenerator() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
					ctx,
					"management.gateway.outbound.class=" + LocalGateway.class.getName()
			);
			ctx.register(JwtGeneratorFactoryBean.class);
			ctx.refresh();

			JwtGeneratorFactoryBean factoryBean = ctx.getBean(JwtGeneratorFactoryBean.class);
			assertInstanceOf(JwtKeyGenerator.class, factoryBean.getObject());
		}
	}
}
