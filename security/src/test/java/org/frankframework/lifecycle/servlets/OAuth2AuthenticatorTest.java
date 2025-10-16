package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		// Act
		authenticator.configureHttpSecurity(httpSecurity);

		// Assert
		assertArrayEquals(new String[] {"/iaf/gui/*"}, authenticator.getPrivateEndpoints().toArray());
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
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		// Act
		authenticator.configureHttpSecurity(httpSecurity);

		// Assert
		assertArrayEquals(new String[] {"/test/*", "/"}, authenticator.getPrivateEndpoints().toArray());
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
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		// Act
		authenticator.configureHttpSecurity(httpSecurity);

		// Assert
		assertArrayEquals(new String[] {"/iaf/gui/*"}, authenticator.getPrivateEndpoints().toArray());
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
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);
		ArgumentCaptor<ICredentials> credentialCapture = ArgumentCaptor.captor();

		// Act
		authenticator.configureHttpSecurity(httpSecurity);
		verify(authenticator).getRegistration(eq("azure"), credentialCapture.capture());


		// Assert
		assertArrayEquals(new String[] {"/iaf/gui/*"}, authenticator.getPrivateEndpoints().toArray());
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
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);
		ArgumentCaptor<ICredentials> credentialCapture = ArgumentCaptor.captor();

		// Act
		authenticator.configureHttpSecurity(httpSecurity);
		verify(authenticator).getRegistration(eq("azure"), credentialCapture.capture());


		// Assert
		assertArrayEquals(new String[] {"/iaf/gui/*"}, authenticator.getPrivateEndpoints().toArray());
		assertEquals("http://my-base.com/context/oauth2/code/{registrationId}", authenticator.getRedirectUri());
		ICredentials credentials = credentialCapture.getValue();
		assertEquals("clientID", credentials.getUsername());
		assertEquals("clientSecret", credentials.getPassword());
		assertEquals("doesnt-exist", credentials.getAlias());
	}
}
