package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.frankframework.management.bus.LocalGateway;
import org.frankframework.management.switchboard.CloudAgentException;
import org.frankframework.management.switchboard.CloudAgentOutboundGateway;

class JwtGeneratorFactoryBeanTest {

	private JwtGeneratorFactoryBean factoryBean;

	@BeforeEach
	void beforeEach() {
		factoryBean = new JwtGeneratorFactoryBean();
	}

	@Test
	@DisplayName("When setting CloudAgentOutboundGateway, Then KeystoreJwtKeyGenerator is created")
	void keyStoreKeyGeneratorTest() throws NoSuchFieldException, IllegalAccessException {
		Field outboundClassField = factoryBean.getClass().getDeclaredField("outboundClass");
		outboundClassField.setAccessible(true);
		outboundClassField.set(factoryBean, CloudAgentOutboundGateway.class.getName());

		// Could not do easy assertion of created class, so it checks the exception that occurs when creating the class without the dependencies it needs.
		assertThrows(CloudAgentException.class, () -> factoryBean.getObject());
	}

	@Test
	@DisplayName("When setting any OutboundGateway other than CloudAgent, Then JwtKeyGenerator is created")
	void jwtKeyGeneratorTest() throws NoSuchFieldException, IllegalAccessException {
		Field outboundClassField = factoryBean.getClass().getDeclaredField("outboundClass");
		outboundClassField.setAccessible(true);
		outboundClassField.set(factoryBean, LocalGateway.class.getName());
		assertInstanceOf(JwtKeyGenerator.class, factoryBean.getObject());
	}
}
