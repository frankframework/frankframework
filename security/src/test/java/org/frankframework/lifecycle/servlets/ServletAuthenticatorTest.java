package org.frankframework.lifecycle.servlets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.lifecycle.servlets.ServletAuthenticatorTest.SpringRootInitializer;
import org.frankframework.util.SpringUtils;

/**
 * Base class which prepares all Spring magic that's required to use Spring Security.
 * @param <T>
 */
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
abstract class ServletAuthenticatorTest<T extends AbstractServletAuthenticator> {

	@Autowired
	public ApplicationContext applicationContext;

	protected T authenticator;
	protected HttpSecurity httpSecurity;

	public static class SpringRootInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			SecuritySettings.setupDefaultSecuritySettings(applicationContext.getEnvironment());
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
		SpringUtils.autowireByType(applicationContext, authenticator);
		httpSecurity = createHttpSecurity();
	}

	protected final ServletConfiguration createServletConfiguration() {
		ServletConfiguration config = new ServletConfiguration();
		Environment environment = mock(Environment.class);
		when(environment.getProperty(anyString())).thenReturn("CONTAINER");
		config.setEnvironment(environment);
		config.afterPropertiesSet();
		return config;
	}

	protected abstract T createAuthenticator();

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
}
