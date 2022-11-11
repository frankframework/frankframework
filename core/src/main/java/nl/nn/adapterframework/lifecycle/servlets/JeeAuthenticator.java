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

import java.util.Arrays;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.MappableAttributesRetriever;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;
import org.springframework.security.web.authentication.preauth.j2ee.J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.j2ee.J2eePreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.j2ee.WebXmlMappableAttributesRetriever;

import lombok.Setter;
import nl.nn.adapterframework.util.SpringUtils;

public class JeeAuthenticator implements IAuthenticator, ApplicationContextAware {
	private @Setter ApplicationContext applicationContext;

	@Override
	public SecurityFilterChain configure(ServletConfiguration config, HttpSecurity http) throws Exception {
		AuthenticationManager authManager = getAuthenticationManager(http);
		http.addFilter(getProcessingFilter(authManager));
		http.authenticationManager(authManager);
		return http.build();
	}

	//see AuthenticationManagerFactoryBean
	private AuthenticationManager getAuthenticationManager(HttpSecurity http) {
		AuthenticationProvider provider = getAuthenticationProvider(http);
		return new ProviderManager(Arrays.<AuthenticationProvider>asList(provider));
	}

	//see JeeConfigurer.init
	private PreAuthenticatedAuthenticationProvider getAuthenticationProvider(HttpSecurity http) {
		PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
		authenticationProvider.setPreAuthenticatedUserDetailsService(new PreAuthenticatedGrantedAuthoritiesUserDetailsService());
		http.authenticationProvider(authenticationProvider).setSharedObject(AuthenticationEntryPoint.class, new Http403ForbiddenEntryPoint());
		return authenticationProvider;
	}

	private J2eePreAuthenticatedProcessingFilter getProcessingFilter(AuthenticationManager authManager) {
		J2eePreAuthenticatedProcessingFilter filter = new J2eePreAuthenticatedProcessingFilter();
		filter.setAuthenticationDetailsSource(getAuthenticationDetailsSource());
		filter.setAuthenticationManager(authManager);
		return filter;
	}

	private J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource getAuthenticationDetailsSource() {
		J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource authenticationDetailSource = new J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource();
		authenticationDetailSource.setMappableRolesRetriever(getWebXmlSecurityRoles());
		return authenticationDetailSource;
	}

	private MappableAttributesRetriever getWebXmlSecurityRoles() {
		return SpringUtils.createBean(applicationContext, WebXmlMappableAttributesRetriever.class);
	}
}
