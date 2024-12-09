package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class DummyAuthenticatorTest extends ServletAuthenticatorTest {

	public static class DummyAuthenticator extends AbstractServletAuthenticator {
		@Override
		protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
			return http.build();
		}
	}

	@Override
	protected DummyAuthenticator createAuthenticator() {
		return new DummyAuthenticator();
	}

	@Test
	void testRequestMatchersMultilineUrl() throws Exception {
		// Arrange
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/iaf/api/*, !/iaf/api/server/health");
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		// Act
		DefaultSecurityFilterChain filterChain = (DefaultSecurityFilterChain) authenticator.configureHttpSecurity(httpSecurity);

		// Assert
		assertArrayEquals(new String[] {"/iaf/api/*"}, authenticator.getPrivateEndpoints().toArray());
		RequestMatcher requestMatcher = filterChain.getRequestMatcher();
		assertInstanceOf(URLRequestMatcher.class, requestMatcher);
		assertTrue(requestMatcher.toString().contains("[/iaf/api/]")); // dirty check (for now)
	}
}
