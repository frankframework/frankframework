package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import org.frankframework.credentialprovider.ICredentials;
import org.frankframework.util.SpringUtils;

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

	@Test
	void testInteractiveLoginChainIsStateful() throws Exception {
		// Arrange
		authenticator.setClientId("clientID");
		authenticator.setClientSecret("clientSecret");
		authenticator.setProvider("github");

		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/webcontent/*");
		config.setSecurityRoles(new String[]{ "IbisTester" });
		authenticator.registerServlet(config);

		// Act
		authenticator.configureHttpSecurity(httpSecurity);

		// Assert: the login chain saves the requested URL and persists the authentication
		assertInstanceOf(HttpSessionRequestCache.class, httpSecurity.getSharedObject(RequestCache.class));
		assertInstanceOf(HttpSessionSecurityContextRepository.class, httpSecurity.getSharedObject(SecurityContextRepository.class));
	}

	@Test
	void testSessionIsNotSharedBetweenServlets() throws Exception {
		// Arrange: two OAuth2 chains protecting different servlets, sharing one HttpSession.
		// Like the console and ladybug chains, the second authenticator lives in its own (child) ApplicationContext.
		configureGithubLogin(authenticator, "/webcontent/*");
		authenticator.configureHttpSecurity(httpSecurity);

		try (GenericApplicationContext otherContext = new GenericApplicationContext(applicationContext)) {
			otherContext.refresh();
			OAuth2Authenticator otherAuthenticator = createAuthenticator();
			SpringUtils.autowireByType(otherContext, otherAuthenticator);
			configureGithubLogin(otherAuthenticator, "/iaf/gui/*");
			HttpSecurity otherHttpSecurity = createHttpSecurity();
			otherAuthenticator.configureHttpSecurity(otherHttpSecurity);

			SecurityContextRepository contextRepository = httpSecurity.getSharedObject(SecurityContextRepository.class);
			SecurityContextRepository otherContextRepository = otherHttpSecurity.getSharedObject(SecurityContextRepository.class);
			RequestCache requestCache = httpSecurity.getSharedObject(RequestCache.class);
			RequestCache otherRequestCache = otherHttpSecurity.getSharedObject(RequestCache.class);

			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/webcontent/page");
			request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);
			MockHttpServletResponse response = new MockHttpServletResponse();
			SecurityContext context = new SecurityContextImpl(new TestingAuthenticationToken("user", "password", "ROLE_IbisTester"));

			// Act: log in and save the requested URL on the first servlet
			contextRepository.saveContext(context, request, response);
			requestCache.saveRequest(request, response);

			// Assert: the first chain finds its own authentication and saved request, the other chain does not
			assertTrue(contextRepository.containsContext(request));
			assertNotNull(requestCache.getRequest(request, response));
			assertFalse(otherContextRepository.containsContext(request));
			assertNull(otherRequestCache.getRequest(request, response));
		}
	}

	@Test
	void testApiRequestIsNotSavedAndCreatesNoSession() throws Exception {
		// Arrange
		configureGithubLogin(authenticator, "/webcontent/*");
		authenticator.configureHttpSecurity(httpSecurity);
		RequestCache requestCache = httpSecurity.getSharedObject(RequestCache.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/webcontent/api/status");
		request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		MockHttpServletResponse response = new MockHttpServletResponse();

		// Act
		requestCache.saveRequest(request, response);

		// Assert
		assertNull(request.getSession(false));
	}

	@Test
	void testSessionScopeIsStableAndIndependentOfMappingOrder() throws Exception {
		// Arrange
		configureGithubLogin(authenticator, "/test/*,/webcontent/*");

		OAuth2Authenticator otherAuthenticator = createAuthenticator();
		configureGithubLogin(otherAuthenticator, "/webcontent/*,/test/*");

		// Act & Assert
		assertEquals("/test/*,/webcontent/*", authenticator.getSessionScope());
		assertEquals(authenticator.getSessionScope(), otherAuthenticator.getSessionScope());
	}

	private ServletConfiguration configureGithubLogin(OAuth2Authenticator oauth2Authenticator, String urlMapping) {
		oauth2Authenticator.setClientId("clientID");
		oauth2Authenticator.setClientSecret("clientSecret");
		oauth2Authenticator.setProvider("github");

		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping(urlMapping);
		config.setSecurityRoles(new String[]{ "IbisTester" });
		oauth2Authenticator.registerServlet(config);
		return config;
	}
}
