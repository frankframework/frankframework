package nl.nn.adapterframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.nn.adapterframework.lifecycle.servlets.ServletAuthenticatorTest.SpringRootInitializer;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = {SpringRootInitializer.class})
public class ServletAuthenticatorTest {

	@Autowired
	public ApplicationContext applicationContext;

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

	@Test
	public void testMultilineUrl() throws Exception {
		// Arrange
		DummyAuthenticator authenticator = new DummyAuthenticator();

		ServletConfiguration config = new ServletConfiguration();
		config.afterPropertiesSet();
		config.setUrlMapping("/iaf/api/*, !/iaf/api/server/health");
		config.setSecurityRoles(new String[] {"IbisTester"});
		authenticator.registerServlet(config);

		ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<Object>() {
			@Override
			public <O> O postProcess(O object) {
				return object;
			}
		};
		AuthenticationManagerBuilder authMgrBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		authMgrBuilder.authenticationProvider(new AllAuthenticatedProvider());
		Map<Class<?>, Object> sharedObjects = Map.of(ApplicationContext.class, applicationContext);
		HttpSecurity httpSecurity = spy(new HttpSecurity(objectPostProcessor, authMgrBuilder, sharedObjects));

		// Act
		DefaultSecurityFilterChain filterChain = (DefaultSecurityFilterChain) authenticator.configureHttpSecurity(httpSecurity);

		// Assert
		assertArrayEquals(new String[] {"/iaf/api/*"}, authenticator.getPrivateEndpoints().toArray());
		RequestMatcher requestMatcher = filterChain.getRequestMatcher();
		assertInstanceOf(URLRequestMatcher.class, requestMatcher);
		assertTrue(requestMatcher.toString().contains("[/iaf/api/]")); // dirty check (for now)
	}

	private static class DummyAuthenticator extends ServletAuthenticatorBase {
		@Override
		protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
			return http.build();
		}
	}
}
