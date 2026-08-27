package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import org.frankframework.credentialprovider.ICredentials;

public class OAuth2AuthenticatorTest extends ServletAuthenticatorTest<OAuth2Authenticator> {

	@Override
	protected OAuth2Authenticator createAuthenticator() {
		return spy(new OAuth2Authenticator());
	}

	@Test
	void testDefaultBaseUrl() throws Exception {
		// Arrange
		authenticator.setClientId("clientID");
		authenticator.setClientSecret("clientSecret");
		authenticator.setProvider("github");

		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/iaf/gui/*");
		config.setSecurityRoles(new String[]{ "IbisTester" });
		authenticator.registerServlet(config);

		// Act
		authenticator.configureHttpSecurity(httpSecurity);

		// Assert
		assertArrayEquals(new String[]{ "/iaf/gui/*" }, authenticator.getPrivateEndpoints().toArray());
		assertEquals("{baseUrl}/iaf/gui/oauth2/code/{registrationId}", authenticator.getRedirectUri());
	}

	@Test
	void testDefaultBaseUrlWithMultipleMappings() throws Exception {
		// Arrange
		authenticator.setClientId("clientID");
		authenticator.setClientSecret("clientSecret");
		authenticator.setProvider("github");

		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/test/*,/"); // Uses the first entry
		config.setSecurityRoles(new String[]{ "IbisTester" });
		authenticator.registerServlet(config);

		// Act
		authenticator.configureHttpSecurity(httpSecurity);

		// Assert
		assertArrayEquals(new String[]{ "/test/*", "/" }, authenticator.getPrivateEndpoints().toArray());
		assertEquals("{baseUrl}/test/oauth2/code/{registrationId}", authenticator.getRedirectUri());
	}

	@Test
	void testAbsoluteBaseUrl() throws Exception {
		// Arrange
		authenticator.setBaseUrl("http://my-base.com/context/");
		authenticator.setClientId("clientID");
		authenticator.setClientSecret("clientSecret");
		authenticator.setProvider("github");

		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/iaf/gui/*");
		config.setSecurityRoles(new String[]{ "IbisTester" });
		authenticator.registerServlet(config);

		// Act
		authenticator.configureHttpSecurity(httpSecurity);

		// Assert
		assertArrayEquals(new String[]{ "/iaf/gui/*" }, authenticator.getPrivateEndpoints().toArray());
		assertEquals("http://my-base.com/context/oauth2/code/{registrationId}", authenticator.getRedirectUri());
	}

	@Test
	void testAuthAlias() throws Exception {
		// Arrange
		authenticator.setBaseUrl("http://my-base.com/context/");
		authenticator.setClientAuthAlias("alias1");
		authenticator.setTenantId("tenantId");
		authenticator.setProvider("azure");

		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/iaf/gui/*");
		config.setSecurityRoles(new String[]{ "IbisTester" });
		authenticator.registerServlet(config);
		ArgumentCaptor<ICredentials> credentialCapture = ArgumentCaptor.captor();

		// Act
		authenticator.configureHttpSecurity(httpSecurity);
		verify(authenticator).getRegistration(eq("azure"), credentialCapture.capture());


		// Assert
		assertArrayEquals(new String[]{ "/iaf/gui/*" }, authenticator.getPrivateEndpoints().toArray());
		assertEquals("http://my-base.com/context/oauth2/code/{registrationId}", authenticator.getRedirectUri());
		ICredentials credentials = credentialCapture.getValue();
		assertEquals("username1", credentials.getUsername());
		assertEquals("password1", credentials.getPassword());
		assertEquals("alias1", credentials.getAlias());
	}

	@Test
	void testAuthAliasDoesNotExist() throws Exception {
		// Arrange
		authenticator.setBaseUrl("http://my-base.com/context/");
		authenticator.setClientAuthAlias("doesnt-exist");
		authenticator.setClientId("clientID");
		authenticator.setTenantId("tenantId");
		authenticator.setClientSecret("clientSecret");
		authenticator.setProvider("azure");

		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/iaf/gui/*");
		config.setSecurityRoles(new String[]{ "IbisTester" });
		authenticator.registerServlet(config);
		ArgumentCaptor<ICredentials> credentialCapture = ArgumentCaptor.captor();

		// Act
		authenticator.configureHttpSecurity(httpSecurity);
		verify(authenticator).getRegistration(eq("azure"), credentialCapture.capture());


		// Assert
		assertArrayEquals(new String[]{ "/iaf/gui/*" }, authenticator.getPrivateEndpoints().toArray());
		assertEquals("http://my-base.com/context/oauth2/code/{registrationId}", authenticator.getRedirectUri());
		ICredentials credentials = credentialCapture.getValue();
		assertEquals("clientID", credentials.getUsername());
		assertEquals("clientSecret", credentials.getPassword());
		assertEquals("doesnt-exist", credentials.getAlias());
	}

	@Test
	void testKeycloakUsesOidcDiscovery() throws Exception {
		// Arrange
		authenticator.setBaseUrl("http://keycloak.example.com");
		authenticator.setTenantId("my-realm");
		authenticator.setClientId("clientID");
		authenticator.setClientSecret("clientSecret");
		authenticator.setProvider("keycloak");

		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/iaf/gui/*");
		config.setSecurityRoles(new String[]{ "IbisTester" });
		authenticator.registerServlet(config);

		String expectedIssuerUri = "http://keycloak.example.com/realms/my-realm";

		// Simulate what fromOidcIssuerLocation would return after fetching .well-known/openid-configuration
		ClientRegistration.Builder discoveredBuilder = ClientRegistration.withRegistrationId("keycloak")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationUri(expectedIssuerUri + "/protocol/openid-connect/auth")
				.tokenUri(expectedIssuerUri + "/protocol/openid-connect/token")
				.jwkSetUri(expectedIssuerUri + "/protocol/openid-connect/certs")
				.userInfoUri(expectedIssuerUri + "/protocol/openid-connect/userinfo")
				.issuerUri(expectedIssuerUri);

		try (MockedStatic<ClientRegistrations> clientRegistrationsMock = mockStatic(ClientRegistrations.class)) {
			clientRegistrationsMock.when(() -> ClientRegistrations.fromOidcIssuerLocation(expectedIssuerUri))
					.thenReturn(discoveredBuilder);

			// Act
			authenticator.configureHttpSecurity(httpSecurity);

			// Assert: the discovery endpoint was called with the correct issuer URI
			clientRegistrationsMock.verify(() -> ClientRegistrations.fromOidcIssuerLocation(expectedIssuerUri));

			ClientRegistration registration = authenticator.getOrCreateClientRegistrationRepository()
					.findByRegistrationId("keycloak");

			assertNotNull(registration);
			assertEquals("keycloak", registration.getRegistrationId());
			assertEquals("keycloak", registration.getClientName());
			assertEquals("preferred_username", registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
			assertTrue(registration.getScopes().containsAll(Set.of("openid", "profile", "email")));
		}
	}
}
