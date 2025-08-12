package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.frankframework.util.StreamUtil;

public class JwtSecurityFilterTest {

	@TempDir
	private File tempDirectory;

	private JwtKeyGenerator keyGenerator;
	private String jwksUrl;

	@BeforeEach
	public void setUp() throws Exception {
		keyGenerator = new JwtKeyGenerator();
		File jwksFile = new File(tempDirectory, "jwks.txt");
		try (OutputStream fileOut = Files.newOutputStream(jwksFile.toPath())) {
			StreamUtil.streamToStream(new ByteArrayInputStream(keyGenerator.getPublicJwkSet().getBytes(StandardCharsets.UTF_8)), fileOut);
		}
		jwksUrl = jwksFile.toURI().toURL().toExternalForm();

		Authentication authentication = new TestingAuthenticationToken("dummy", null, "ROLE_role1", "ROLE_role2");
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@Test
	public void testNoJwtProvided() throws Exception {
		JwtSecurityFilter filter = new JwtSecurityFilter();
		filter.setJwksEndpoint(jwksUrl);
		filter.afterPropertiesSet();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dummy");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act + Assert
		assertThrows(IOException.class, () -> filter.doFilter(request, response, chain));
	}

	@Test
	public void createJwtAndReadAsAuthenticationToken() throws Exception {
		// Arrange
		JwtSecurityFilter filter = new JwtSecurityFilter();
		filter.setJwksEndpoint(jwksUrl);
		filter.afterPropertiesSet();
		String jwt = keyGenerator.create();
		assertNotNull(jwt);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dummy");
		request.addHeader("Authentication", "Bearer " + jwt);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		List<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
		assertAll(
				() -> assertInstanceOf(JwtAuthenticationToken.class, authentication),
				() -> assertEquals("dummy", authentication.getPrincipal(), "principal not mapped correctly"),
				() -> assertEquals(2, authorities.size()),
				() -> assertTrue(authorities.contains("ROLE_role1")),
				() -> assertTrue(authorities.contains("ROLE_role2")),
				() -> assertTrue(authentication.isAuthenticated())
		);
	}

}
