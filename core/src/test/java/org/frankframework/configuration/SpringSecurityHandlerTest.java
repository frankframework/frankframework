package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.frankframework.core.SpringSecurityHandler;

class SpringSecurityHandlerTest {

	private SpringSecurityHandler springSecurityHandler;

	@BeforeEach
	void mockAuthentication() {
		UserDetails testUser = getTestUser();

		Authentication authentication = new UsernamePasswordAuthenticationToken(testUser, testUser.getPassword(), testUser.getAuthorities());

		SecurityContextHolder.getContext().setAuthentication(authentication);

		springSecurityHandler = new SpringSecurityHandler();
	}

	private UserDetails getTestUser() {
		return new User("testuser", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "admin", "ADMIN" })
	void testIsUserInRole_UserHasRole_ReturnsTrue(String role) {
		boolean result = springSecurityHandler.isUserInRole(role);

		assertTrue(result);
	}

	@Test
	void testIsUserInRole_UserDoesNotHaveRole_ReturnsFalse() {
		boolean result = springSecurityHandler.isUserInRole("BLABLA");

		assertFalse(result);
	}

	@Test
	void testGetPrincipal_ReturnsPrincipal() {
		Principal result = springSecurityHandler.getPrincipal();

		assertEquals("testuser", result.getName());
	}
}
