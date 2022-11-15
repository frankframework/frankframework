/*
   Copyright 2022 WeAreFrank!

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import lombok.Getter;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.util.LogUtil;

public abstract class ServletAuthenticatorBase implements IAuthenticator, ApplicationContextAware {
	private static final String HTTP_SECURITY_BEAN_NAME = "org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration.httpSecurity";

	protected final Logger log = LogUtil.getLogger(this);

	private @Getter ApplicationContext applicationContext;
	private @Getter Set<String> endpoints = new HashSet<>();
	private @Getter List<String> securityRoles = new ArrayList<>();

	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public final void registerServlet(ServletConfiguration config) {
		setEndpoints(config.getUrlMapping());
		setSecurityRoles(config.getSecurityRoles());
	}

	private void setSecurityRoles(List<String> securityRoles) {
		if(securityRoles == null || securityRoles.isEmpty()) {
			securityRoles = ServletManager.DEFAULT_IBIS_ROLES;
		}
		this.securityRoles.addAll(securityRoles);
	}

	private void setEndpoints(List<String> urlMappings) {
		for(String url : urlMappings) {
			String matcherUrl = url;
			if(url.endsWith("*")) {
				matcherUrl = url+"*";
			}

			if(endpoints.contains(matcherUrl)) {
				throw new IllegalStateException("endpoint already configured");
			}

			log.info("registering url [{}] with lookup pattern [{}]", url, matcherUrl);
			endpoints.add(matcherUrl);
		}
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
		String name = "HttpSecurityChain-"+this.getClass().getSimpleName();

		//Register the SecurityFilter in the (WebXml)BeanFactory so the WebSecurityConfiguration can configure them
		beanFactory.registerSingleton(name, createSecurityFilterChain());
	}

	private SecurityFilterChain createSecurityFilterChain() {
		HttpSecurity httpSecurityConfigurer = applicationContext.getBean(HTTP_SECURITY_BEAN_NAME, HttpSecurity.class);
		return configureHttpSecurity(httpSecurityConfigurer);
	}

	private SecurityFilterChain configureHttpSecurity(HttpSecurity http) {
		try {
			//Apply defaults to disable bloated filters, see DefaultSecurityFilterChain.getFilters for the actual list.
			http.headers().frameOptions().sameOrigin(); //Allow same origin iframe request
			http.csrf().disable();
			http.requestMatcher(getRequestMatcher());
			http.formLogin().disable(); //Disable the form login filter
			http.anonymous().disable(); //Disable the default anonymous filter
			http.logout().disable(); //Disable the logout endpoint on every filter
//			http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); //Disables cookies

			return configure(http);
		} catch (Exception e) {
			throw new IllegalStateException("unable to configure Spring Security", e);
		}
	}

	/** Before building it configures the Chain. */
	protected abstract SecurityFilterChain configure(HttpSecurity http) throws Exception;

	private RequestMatcher getRequestMatcher() {
		List<RequestMatcher> requestMatchers = new ArrayList<>();
		for(String url : endpoints) {
			requestMatchers.add(new AntPathRequestMatcher(url, null, false));
		}
		return (requestMatchers.size() == 1) ? requestMatchers.get(0) : new OrRequestMatcher(requestMatchers);
	}
}
