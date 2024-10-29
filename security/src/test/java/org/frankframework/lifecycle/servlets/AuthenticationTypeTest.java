package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.lifecycle.servlets.ServletAuthenticatorTest.SpringRootInitializer;

@SpringJUnitConfig(initializers = { SpringRootInitializer.class })
public class AuthenticationTypeTest {

	@Autowired
	ApplicationContext applicationContext;

	private static DynamicPropertyRegistry ENV;

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		AuthenticationTypeTest.ENV = registry;
	}

	private synchronized void setEnvironment(String dtapStage, Boolean authEnabled) {
		ENV.add("dtap.stage", () -> dtapStage);
		ENV.add(SecuritySettings.AUTH_ENABLED_KEY, () -> authEnabled);
		SecuritySettings.resetSecuritySettings();
		SecuritySettings.setupDefaultSecuritySettings(applicationContext.getEnvironment());
	}

	@ParameterizedTest
	@ValueSource(strings = {"LOC", "DEV", "TST", "ACC", "PRD", "Pietje", "Pannenkoek"})
	@DisplayName("When AUTH_ENABLED_KEY has been set to true, always use SEALED authentication")
	public void createAuthenticatorWhenTrueAlwaysSEALED(String stage) {
		setEnvironment(stage, true);

		assertTrue(SecuritySettings.isWebSecurityEnabled());
		if(stage.equals("LOC")) {
			assertEquals(TransportGuarantee.NONE, SecuritySettings.getDefaultTransportGuarantee());
		}
		else {
			assertEquals(TransportGuarantee.CONFIDENTIAL, SecuritySettings.getDefaultTransportGuarantee());
		}

		IAuthenticator authenticator = AuthenticatorUtils.createAuthenticator(applicationContext, "dummy.");
		assertInstanceOf(SealedAuthenticator.class, authenticator);
	}

	@ParameterizedTest
	@ValueSource(strings = {"LOC", "DEV", "TST", "ACC", "PRD", "Pietje", "Pannenkoek"})
	@DisplayName("When AUTH_ENABLED_KEY has been set to false, always use NO authentication")
	public void createAuthenticatorWhenAuthEnabledFalseAlwaysNOOP(String stage) {
		setEnvironment(stage, false);

		assertFalse(SecuritySettings.isWebSecurityEnabled());
		if(stage.equals("LOC")) {
			assertEquals(TransportGuarantee.NONE, SecuritySettings.getDefaultTransportGuarantee());
		}
		else {
			assertEquals(TransportGuarantee.CONFIDENTIAL, SecuritySettings.getDefaultTransportGuarantee());
		}

		IAuthenticator authenticator = AuthenticatorUtils.createAuthenticator(applicationContext, "dummy.");
		assertInstanceOf(NoOpAuthenticator.class, authenticator);
	}

	@ParameterizedTest
	@ValueSource(strings = {"DEV", "TST", "ACC", "PRD", "Pietje", "Pannenkoek"})
	@DisplayName("When AUTH_ENABLED_KEY is undefined (empty) and stage != LOC, always use SEALED authentication")
	public void createAuthenticatorWhenAuthEnabledNotSpecified(String stage) {
		setEnvironment(stage, null);

		assertTrue(SecuritySettings.isWebSecurityEnabled());
		assertEquals(TransportGuarantee.CONFIDENTIAL, SecuritySettings.getDefaultTransportGuarantee());
		IAuthenticator authenticator = AuthenticatorUtils.createAuthenticator(applicationContext, "dummy.");
		assertInstanceOf(SealedAuthenticator.class, authenticator);
	}

	@Test
	@DisplayName("When AUTH_ENABLED_KEY is undefined (empty) and stage == LOC, always use NO authentication")
	public void createAuthenticatorWhenLocNoAuthentication() {
		setEnvironment("LOC", null);

		assertFalse(SecuritySettings.isWebSecurityEnabled());
		assertEquals(TransportGuarantee.NONE, SecuritySettings.getDefaultTransportGuarantee());
		IAuthenticator authenticator = AuthenticatorUtils.createAuthenticator(applicationContext, "dummy.");
		assertInstanceOf(NoOpAuthenticator.class, authenticator);
	}

}
