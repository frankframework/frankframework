package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.frankframework.credentialprovider.ICredentials;

public class InMemoryAuthenticatorTest extends ServletAuthenticatorTest<InMemoryAuthenticator> {

	@Override
	protected InMemoryAuthenticator createAuthenticator() {
		return spy(new InMemoryAuthenticator());
	}

	@Test
	public void noPassword() {
		authenticator.setUsername("but-no password");

		// Arrange
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/dummy");
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authenticator.configureHttpSecurity(httpSecurity));
		assertEquals("Username and Password must be provided either directly or via an AuthAlias", ex.getMessage());
	}

	@Test
	public void defaultSetup() {
		authenticator.setUsername("tra");
		authenticator.setPassword("lala");

		// Arrange
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/dummy");
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		ArgumentCaptor<ICredentials> credentialCapture = ArgumentCaptor.captor();

		// Act
		assertDoesNotThrow(() -> authenticator.configureHttpSecurity(httpSecurity));
		verify(authenticator).createUserDetails(credentialCapture.capture());
		assertEquals("tra", credentialCapture.getValue().getUsername());
		assertEquals("lala", credentialCapture.getValue().getPassword());
	}

	@Test
	public void testAuthAlias() {
		authenticator.setUsername("tra");
		authenticator.setAuthAlias("alias1");

		// Arrange
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/dummy");
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		ArgumentCaptor<ICredentials> credentialCapture = ArgumentCaptor.captor();

		// Act
		assertDoesNotThrow(() -> authenticator.configureHttpSecurity(httpSecurity));
		verify(authenticator).createUserDetails(credentialCapture.capture());
		assertEquals("username1", credentialCapture.getValue().getUsername());
		assertEquals("password1", credentialCapture.getValue().getPassword());
	}
}
