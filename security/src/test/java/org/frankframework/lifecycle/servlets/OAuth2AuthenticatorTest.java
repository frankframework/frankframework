package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OAuth2AuthenticatorTest extends ServletAuthenticatorTest<OAuth2Authenticator> {

	@Override
	protected OAuth2Authenticator createAuthenticator() {
		return new OAuth2Authenticator();
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
}
