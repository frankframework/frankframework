package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.frankframework.core.SpringSecurityHandler;

class SpringSecuritayHandlerTest {

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

	@Test
	void testIsUserInRole_UserHasRole_ReturnsTrue() {
		boolean result = springSecurityHandler.isUserInRole("ADMIN");

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
