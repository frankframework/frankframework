package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class BearerOnlyAuthenticatorTest extends ServletAuthenticatorTest<BearerOnlyAuthenticator> {

	@Override
	protected BearerOnlyAuthenticator createAuthenticator() {
		return new BearerOnlyAuthenticator();
	}

	@Test
	void testConfigureHttpSecurity() {
		// Set jwks argument and expect no exception
		authenticator.setJwkSetUri("http://localhost:8080/realms/myrealm/.well-known/jwks.json");
		assertDoesNotThrow(() -> authenticator.configure(httpSecurity));
	}

	@Test
	void testConfigureHttpSecurityWithUnreachableIssuerUri() {
		// Set issuerUri and expect no exception
		authenticator.setIssuerUri("http://localhost:8080/realms/myrealm");
		assertThrows(IllegalArgumentException.class, () -> authenticator.configure(httpSecurity));
	}

	@Test
	void testConfigureHttpSecurityWithoutRequiredUri() {
		// Set no issuerUri or jwkSetUri and expect exception
		assertThrows(IllegalArgumentException.class, () -> authenticator.configure(httpSecurity));
	}
}
