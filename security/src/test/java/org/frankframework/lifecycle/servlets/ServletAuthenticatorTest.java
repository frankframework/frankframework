package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.util.Map;

import org.frankframework.lifecycle.servlets.ServletAuthenticatorTest.SpringRootInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;


@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
abstract class ServletAuthenticatorTest {

	@Autowired
	public ApplicationContext applicationContext;

	protected ServletAuthenticatorBase authenticator;
	protected HttpSecurity httpSecurity;

	public static class SpringRootInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			// Empty method, nothing to initialize
		}

	}

	private static class AllAuthenticatedProvider implements AuthenticationProvider {

		@Override
		public Authentication authenticate(Authentication authentication) throws AuthenticationException {
			return authentication;
		}

		@Override
		public boolean supports(Class<?> authentication) {
			return true;
		}
	}

	@BeforeEach
	public void setup() {
		authenticator = createAuthenticator();
		httpSecurity = createHttpSecurity();
	}

	protected abstract ServletAuthenticatorBase createAuthenticator();

	private HttpSecurity createHttpSecurity() {
		ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
			@Override
			public <O> O postProcess(O object) {
				return object;
			}
		};
		AuthenticationManagerBuilder authMgrBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		authMgrBuilder.authenticationProvider(new AllAuthenticatedProvider());
		Map<Class<?>, Object> sharedObjects = Map.of(ApplicationContext.class, applicationContext);
		return spy(new HttpSecurity(objectPostProcessor, authMgrBuilder, sharedObjects));
	}

	@Test
	void testRequestMatchersMultilineUrl() throws Exception {
		// Arrange
		ServletConfiguration config = new ServletConfiguration();
		config.afterPropertiesSet();
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
