/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle.servlets;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Getter;

public abstract class ServletAuthenticatorBase implements IAuthenticator, ApplicationContextAware {
	private static final String HTTP_SECURITY_BEAN_NAME = "org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration.httpSecurity";
	public static final List<String> DEFAULT_IBIS_ROLES = Collections.unmodifiableList(Arrays.asList("IbisObserver", "IbisAdmin", "IbisDataAdmin", "IbisTester", "IbisWebService"));

	public static final String ALLOW_OPTIONS_REQUESTS_KEY = "application.security.http.allowUnsecureOptionsRequests";

	protected final Logger log = LogManager.getLogger(this);

	private Set<String> endpoints = new HashSet<>();
	private @Getter ApplicationContext applicationContext;
	private @Getter Set<String> securityRoles = new HashSet<>();
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
				if (propertySource instanceof MapPropertySource) {
					applicationConstants.putAll(((MapPropertySource) propertySource).getSource());
				}
			}
		}
		return applicationConstants;
	}

	@Override
	public final void registerServlet(ServletConfiguration config) {
		setEndpoints(config.getUrlMapping());
		setSecurityRoles(config.getSecurityRoles());
	}

	private void setSecurityRoles(List<String> securityRoles) {
		if(securityRoles == null || securityRoles.isEmpty()) {
			this.securityRoles.addAll(DEFAULT_IBIS_ROLES);
		} else {
			this.securityRoles.addAll(securityRoles);
		}
	}

	private void setEndpoints(List<String> urlMappings) {
		for(String url : urlMappings) {
			if(endpoints.contains(url)) {
				throw new IllegalStateException("endpoint already configured");
			}

			log.info("registering url pattern [{}]", url);
			endpoints.add(url);
		}
	}

	protected Set<String> getEndpoints() {
		return Collections.unmodifiableSet(endpoints);
	}

	@Override
	public void build() {
		if(applicationContext == null) {
			throw new IllegalStateException("Authenticator is not wired through local BeanFactory");
		}
		if(endpoints.isEmpty()) { //No servlets registered so no need to build/enable this Authenticator
			log.info("no url matchers found, ignoring Authenticator [{}]", this::getClass);
			return;
		}

		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext)applicationContext).getBeanFactory();
		String name = "HttpSecurityChain-"+this.getClass().getSimpleName()+"-"+this.hashCode();

		//Register the SecurityFilter in the (WebXml)BeanFactory so the WebSecurityConfiguration can configure them
		beanFactory.registerSingleton(name, createSecurityFilterChain());
	}

	private SecurityFilterChain createSecurityFilterChain() {
		HttpSecurity httpSecurityConfigurer = applicationContext.getBean(HTTP_SECURITY_BEAN_NAME, HttpSecurity.class);
		return configureHttpSecurity(httpSecurityConfigurer);
	}

	@Override
	public SecurityFilterChain configureHttpSecurity(HttpSecurity http) {
		try {
			//Apply defaults to disable bloated filters, see DefaultSecurityFilterChain.getFilters for the actual list.
			http.headers().frameOptions().sameOrigin(); //Allow same origin iframe request
			http.csrf().disable(); //Disable because the front-end doesn't support CSFR tokens (yet!)
			http.securityMatcher(new URLRequestMatcher(endpoints)); //Triggers the SecurityFilterChain
			http.formLogin().disable(); //Disable the form login filter
			http.anonymous().disable(); //Disable the default anonymous filter
			http.logout().disable(); //Disable the logout endpoint on every filter
//			http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); //Disables cookies
			http.authorizeHttpRequests().requestMatchers(this::authorizationRequestMatcher).authenticated();

			return configure(http);
		} catch (Exception e) {
			throw new IllegalStateException("unable to configure Spring Security", e);
		}
	}

	/**
	 * RequestMatcher which determines when a client has to log in.
	 * @return when !(property {@value #ALLOW_OPTIONS_REQUESTS_KEY} == true, and request == OPTIONS).
	 */
	protected boolean authorizationRequestMatcher(HttpServletRequest request) {
		return !(allowUnsecureOptionsRequest && "OPTIONS".equals(request.getMethod()));
	}

	/** Before building, configure the FilterChain. */
	protected abstract SecurityFilterChain configure(HttpSecurity http) throws Exception;
}
