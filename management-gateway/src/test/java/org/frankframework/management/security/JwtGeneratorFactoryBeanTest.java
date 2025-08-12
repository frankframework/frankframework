package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import org.frankframework.management.bus.LocalGateway;

class JwtGeneratorFactoryBeanTest {

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
