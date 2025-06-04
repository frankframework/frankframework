/*
   Copyright 2022-2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.lifecycle.servlets;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AnonymousConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public abstract class ServletAuthenticatorBase implements IAuthenticator, ApplicationContextAware {
	private static final String HTTP_SECURITY_BEAN_NAME = "org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration.httpSecurity";
	public static final List<String> DEFAULT_IBIS_ROLES = Collections.unmodifiableList(Arrays.asList("IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester", "IbisWebService"));

	public static final String ALLOW_OPTIONS_REQUESTS_KEY = "application.security.http.allowUnsecureOptionsRequests";

	protected final Logger log = LogManager.getLogger(this);

	private final Set<String> publicEndpoints = new HashSet<>();
	private final Set<String> privateEndpoints = new HashSet<>();
	private @Getter ApplicationContext applicationContext;
	private final @Getter Set<String> securityRoles = new HashSet<>();
	private Properties applicationConstants = null;
	private boolean allowUnsecureOptionsRequest = false;

	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		Environment env = applicationContext.getEnvironment();
		allowUnsecureOptionsRequest = env.getProperty(ALLOW_OPTIONS_REQUESTS_KEY, boolean.class, false);
	}

	protected final synchronized Properties getEnvironmentProperties() {
		if(applicationConstants == null) {
			applicationConstants = new Properties();

			PropertySources pss = ((ConfigurableEnvironment) applicationContext.getEnvironment()).getPropertySources();
			for(PropertySource<?> propertySource : pss) {
				if (propertySource instanceof MapPropertySource source) {
					applicationConstants.putAll(source.getSource());
				}
			}
		}
		return applicationConstants;
	}

	@Override
	public final void registerServlet(ServletConfiguration config) {
		addEndpoints(config);
		addSecurityRoles(config.getSecurityRoles());
	}

	/**
	 * For SpringSecurity we MUST register all (required) roles when Anonymous authentication is used.
	 * The SecurityRoles may be used in the implementing class to configure Spring Security with.
	 *
	 * See {@link #configureHttpSecurity(HttpSecurity)} for the configuring process.
	 */
	private void addSecurityRoles(List<String> securityRoles) {
		if(securityRoles.isEmpty()) {
			this.securityRoles.addAll(DEFAULT_IBIS_ROLES);
		} else {
			this.securityRoles.addAll(securityRoles);
		}
	}

	/**
	 * We need to make a distinct difference between public and private endpoints as on public endpoints
	 * you don't want Spring Security to trigger the pre-authentication filters/providers.
	 *
	 * See {@link #configureHttpSecurity(HttpSecurity)} for the configuring process.
	 */
	private void addEndpoints(ServletConfiguration config) {
		for(String url : config.getUrlMapping()) {
			if(publicEndpoints.contains(url) || privateEndpoints.contains(url)) {
				throw new IllegalStateException("endpoint already configured");
			}

			boolean isExcludedUrl = url.charAt(0) == '!';
			if(isExcludedUrl || config.getSecurityRoles().isEmpty()) {
				String publicUrl = isExcludedUrl ? url.substring(1) : url;
				log.info("registering public endpoint with url [{}]", publicUrl);
				publicEndpoints.add(publicUrl);
			} else {
				log.info("registering private endpoint with url pattern [{}]", url);
				privateEndpoints.add(url);
			}
		}
	}

	/**
	 * List of endpoints as well as potential exclusions.
	 */
	protected Set<String> getPrivateEndpoints() {
		return Collections.unmodifiableSet(privateEndpoints);
	}

	@Override
	public void build() {
		if(applicationContext == null) {
			throw new IllegalStateException("Authenticator is not wired through local BeanFactory");
		}
		if (privateEndpoints.isEmpty()) { // No servlets registered so no need to build/enable this Authenticator
			log.info("no url matchers found, ignoring Authenticator [{}]", this::getClass);
			return;
		}

		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext)applicationContext).getBeanFactory();
		String name = "HttpSecurityChain-"+this.getClass().getSimpleName()+"-"+this.hashCode();

		// Register the SecurityFilter in the (WebXml)BeanFactory so the WebSecurityConfiguration can configure them
		beanFactory.registerSingleton(name, createSecurityFilterChain());
	}

	/**
	 * Create a new HttpSecurity object, and disable default filters not required for webservice endpoints.
	 */
	private SecurityFilterChain createSecurityFilterChain() {
		HttpSecurity httpSecurityConfigurer = applicationContext.getBean(HTTP_SECURITY_BEAN_NAME, HttpSecurity.class);

		try {
			httpSecurityConfigurer.csrf(CsrfConfigurer::disable); // Disable CSRF, post requests should be possible.
			httpSecurityConfigurer.formLogin(FormLoginConfigurer::disable); // Disable the form login filter
			httpSecurityConfigurer.logout(LogoutConfigurer::disable); // Disable the logout filter
			httpSecurityConfigurer.headers(h -> h.frameOptions(o -> o.sameOrigin()));

			// STATELESS prevents session from leaking over multiple servlets.
			// Spring Security will never use the cookie to obtain the SecurityContext for webservices.
			httpSecurityConfigurer.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

			return configureHttpSecurity(httpSecurityConfigurer);
		} catch (Exception e) {
			throw new IllegalStateException("unable to configure Spring Security", e);
		}

	}

	@Override
	public SecurityFilterChain configureHttpSecurity(HttpSecurity http) throws Exception {
		RequestMatcher securityRequestMatcher = new URLRequestMatcher(privateEndpoints);
		http.securityMatcher(securityRequestMatcher); // Triggers the SecurityFilterChain, also for OPTIONS requests!

		if (!publicEndpoints.isEmpty()) { // Enable anonymous access on public endpoints
			http.authorizeHttpRequests(requests -> requests.requestMatchers(new URLRequestMatcher(publicEndpoints)).permitAll());
			http.anonymous(withDefaults());
		} else {
			http.anonymous(AnonymousConfigurer::disable); // Disable the default anonymous filter and thus disallow all anonymous access
		}

		// Enables security for all servlet endpoints
		RequestMatcher authorizationRequestMatcher = new AndRequestMatcher(securityRequestMatcher, this::authorizationRequestMatcher);
		http.authorizeHttpRequests(requests -> requests.requestMatchers(authorizationRequestMatcher).access(getAuthorizationManager()));

		return configure(http);
	}

	/**
	 * AuthorizationManager to use for the {@link IAuthenticator}.
	 */
	protected AuthorizationManager<RequestAuthorizationContext> getAuthorizationManager() {
		return AuthenticatedAuthorizationManager.authenticated();
	}

	/**
	 * RequestMatcher which determines when a client has to log in.
	 * @return when !(property {@value #ALLOW_OPTIONS_REQUESTS_KEY} == true, and request == OPTIONS).
	 */
	private boolean authorizationRequestMatcher(HttpServletRequest request) {
		return !(allowUnsecureOptionsRequest && "OPTIONS".equals(request.getMethod()));
	}

	/** Before building, configure the FilterChain. */
	protected abstract SecurityFilterChain configure(HttpSecurity http) throws Exception;
}
