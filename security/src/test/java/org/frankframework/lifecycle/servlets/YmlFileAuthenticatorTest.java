package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

class YmlFileAuthenticatorTest extends ServletAuthenticatorTest {

	@Override
	protected ServletAuthenticatorBase createAuthenticator() {
		YmlFileAuthenticator auth = new YmlFileAuthenticator();
		auth.setFile("localUsers.yml");
		return auth;
	}

	@Test
	public void cannotFindFile() throws Exception {
		((YmlFileAuthenticator) authenticator).setFile("tralala");

		// Arrange
		ServletConfiguration config = new ServletConfiguration();
		config.afterPropertiesSet();
		config.setUrlMapping("/iaf/api/*, !/iaf/api/server/health");
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		assertThrows(FileNotFoundException.class, () -> authenticator.configureHttpSecurity(httpSecurity));
	}
}
