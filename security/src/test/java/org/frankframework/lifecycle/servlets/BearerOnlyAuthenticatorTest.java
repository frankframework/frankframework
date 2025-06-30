package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

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
		// Set issuerUri and expect exception, because the URI is unreachable
		authenticator.setIssuerUri("http://localhost:8080/realms/myrealm");
		assertThrows(IllegalArgumentException.class, () -> authenticator.configure(httpSecurity));
	}

	@Test
	void testConfigureHttpSecurityWithInvalidAuthoritiesClaimName() {
		authenticator.setJwkSetUri("http://localhost:8080/realms/myrealm/.well-known/jwks.json");

		authenticator.setAuthoritiesClaimName("realm_access.roles.onetoomany");
		assertThrows(IllegalArgumentException.class, () -> authenticator.configure(httpSecurity));
	}

	/**
	 * Covers construction of JwtCollectionConverter
	 */
	@ParameterizedTest
	@ValueSource(strings = {"realm_access.roles", "roles"})
	void testConfigureHttpSecurityWithAuthoritiesClaimName(String authoritiesClaimName) {
		authenticator.setJwkSetUri("http://localhost:8080/realms/myrealm/.well-known/jwks.json");

		authenticator.setAuthoritiesClaimName(authoritiesClaimName);
		assertDoesNotThrow(() -> authenticator.configure(httpSecurity));
	}

	@Test
	void testConfigureHttpSecurityWithCustomUsernameAttributeName() {
		authenticator.setJwkSetUri("http://localhost:8080/realms/myrealm/.well-known/jwks.json");
		authenticator.setUserNameAttributeName("preferred_username");

		assertDoesNotThrow(() -> authenticator.configure(httpSecurity));
	}

	@Test
	void testConfigureHttpSecurityWithoutRequiredUri() {
		// Set no issuerUri or jwkSetUri and expect exception
		assertThrows(IllegalArgumentException.class, () -> authenticator.configure(httpSecurity));
	}

	@Test
	void testJwtCollectionConverterWithAuthoritiesClaimName() {
		// Expect our custom role mapper, by using the nested authoritiesClaimName
		authenticator.setAuthoritiesClaimName("realm_access.roles");
		Converter<Jwt, Collection<GrantedAuthority>> jwtCollectionConverter = authenticator.getJwtCollectionConverter();

		Jwt jwt = jwt()
				.claim("realm_access", Map.of("roles", List.of("IbisObserver")))
				.build();

		Collection<GrantedAuthority> convert = jwtCollectionConverter.convert(jwt);

		assertNotNull(convert, "Converted authorities should not be null");
	}

	public static Jwt.Builder jwt() {
		// @formatter:off
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.audience(List.of("https://audience.example.org"))
				.expiresAt(Instant.MAX)
				.issuedAt(Instant.MIN)
				.issuer("https://issuer.example.org")
				.jti("jti")
				.notBefore(Instant.MIN)
				.subject("mock-test-subject");
		// @formatter:on
	}
}
