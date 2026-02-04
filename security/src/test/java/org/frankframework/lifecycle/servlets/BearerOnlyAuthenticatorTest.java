package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
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

	public static List<Arguments> data() {
		return Arrays.asList(
				// Null or empty
				Arguments.of("roles", "roles", Collections.emptyMap(), 1),
				Arguments.of("roles", "roles", List.of(), 1),
				Arguments.of("roles", "roles", List.of(""), 1),
				Arguments.of("roles", "roles", null, 1),
				Arguments.of("roles", "roles", "", 1),

				// Single and multi-line
				Arguments.of("roles", "roles", "IbisObserver", 2),
				Arguments.of("roles", "roles", "IbisObserver IbisAdmin", 3),
				Arguments.of("roles", "roles", "IbisObserver,IbisAdmin", 3),
				Arguments.of("roles", "roles", "IbisObserver, IbisAdmin", 3),

				// Collections and single-entry collection with multi-line String
				Arguments.of("roles", "roles", List.of("IbisObserver"), 2),
				Arguments.of("roles", "roles", List.of("IbisObserver,"), 2),
				Arguments.of("roles", "roles", List.of("IbisObserver IbisAdmin"), 2), // Not seen as multi-value
				Arguments.of("roles", "roles", List.of("IbisObserver,IbisAdmin"), 3),

				// Nested claim
				Arguments.of("realm_access.roles", "realm_access", Map.of("roles", "IbisObserver"), 2),
				Arguments.of("realm_access.roles", "realm_access", Map.of("roles", "IbisObserver, IbisAdmin"), 3),
				Arguments.of("realm_access.roles", "realm_access", Map.of("roles", List.of("IbisObserver")), 2),
				Arguments.of("realm_access.roles", "realm_access", Map.of("roles", List.of("IbisObserver", "IbisAdmin")), 3),
				Arguments.of("realm_access.roles", "realm_access", Map.of("roles", List.of("IbisObserver, IbisAdmin")), 3)
		);
	}

	@MethodSource("data")
	@ParameterizedTest
	void testJwtCollectionConverterWithAuthoritiesClaimName(String authoritiesClaimName, String rolesClaim, Object authorityClaims, int amtRoles) {
		// Expect our custom role mapper, by using the nested authoritiesClaimName
		authenticator.setAuthoritiesClaimName(authoritiesClaimName);
		authenticator.setUserNameAttributeName("sub");
		Converter<Jwt, AbstractAuthenticationToken> jwtCollectionConverter = authenticator::jwtAuthenticationTokenConverter;

		Jwt jwt = jwt()
				.claim(rolesClaim, authorityClaims)
				.build();

		AbstractAuthenticationToken token = jwtCollectionConverter.convert(jwt);
		Collection<GrantedAuthority> authorities = token.getAuthorities();

		assertTrue(token.isAuthenticated());
		assertNotNull(authorities, "Converted authorities should not be null");
		assertEquals(amtRoles, authorities.size());

		// Ensure we've read the correct 'UserNameAttribute'
		assertEquals("mock-test-subject", token.getName());
		Jwt jwtToken = assertInstanceOf(Jwt.class, token.getPrincipal());
		assertEquals("mock-test-subject", jwtToken.getSubject());

		// Ensure the token hasn't changed
		assertEquals("token", jwtToken.getTokenValue());

		// Ensure these values are copied over
		assertEquals(Instant.MAX, jwtToken.getExpiresAt());
		assertEquals(Instant.MIN, jwtToken.getIssuedAt());
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
