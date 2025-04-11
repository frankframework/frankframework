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
package org.frankframework.lifecycle.servlets;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.JeeConfigurer;
import org.springframework.security.core.authority.mapping.MappableAttributesRetriever;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;
import org.springframework.security.web.authentication.preauth.j2ee.J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.j2ee.J2eePreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.j2ee.WebXmlMappableAttributesRetriever;

import org.frankframework.util.SpringUtils;

/**
 * Authenticator for J2EE authentication.
 * <p>
 * This authenticator can be used to authenticate users using J2EE authentication.
 * It delegates authentication to the Java EE container, using container-managed security
 * defined in web.xml or through other container-specific mechanisms.
 * </p>
 * <p>
 * The authenticator extracts security roles from the container and maps them to
 * Frank! Framework roles. No additional login form is required, as authentication
 * is handled entirely by the container.
 * </p>
 * <p>
 * This authenticator should be configured by setting its type to 'CONTAINER', for example:
 * <pre>{@code
 * application.security.console.authentication.type=CONTAINER
 * }</pre>
 * </p>
 *
 * @see <a href="https://docs.spring.io/spring-security/site/docs/3.0.x/reference/introduction.html">Spring Security Introduction</a>
 * @see <a href="https://docs.spring.io/spring-security/site/docs/3.0.x/reference/authz-arch.html">Spring Security Authorization Architecture</a>
 * @see <a href="https://stackoverflow.com/questions/9831268/how-to-use-j2eepreauthenticatedprocessingfilter-and-a-custom-authentication-prov">StackOverflow J2EE PreAuthenticatedProcessingFilter</a>
 */
public class JeeAuthenticator extends AbstractServletAuthenticator {

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		AuthenticationManager authManager = getAuthenticationManager(http);
		http.addFilter(getProcessingFilter(authManager));
		http.authenticationManager(authManager);
		return http.build();
	}

	// See AuthenticationManagerFactoryBean
	private AuthenticationManager getAuthenticationManager(HttpSecurity http) {
		AuthenticationProvider provider = getAuthenticationProvider(http);
		return new ProviderManager(List.of(provider));
	}

	/**
	 * The J2EE authentication provider. The JeeConfigurer isn't used because of the custom AuthenticationDetailsSource.
	 * See {@link JeeConfigurer#init(org.springframework.security.config.annotation.web.HttpSecurityBuilder)}
	 */
	private PreAuthenticatedAuthenticationProvider getAuthenticationProvider(HttpSecurity http) {
		PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider(); // Converts the AuthenticationToken into UserDetails
		authenticationProvider.setPreAuthenticatedUserDetailsService(new PreAuthenticatedGrantedAuthoritiesUserDetailsService());
		http.authenticationProvider(authenticationProvider).setSharedObject(AuthenticationEntryPoint.class, getEntryPoint());
		return authenticationProvider;
	}

	// When using JEE the container authenticates clients (401) we therefore only have to authorize them. If not authorized, return a 403.
	private AuthenticationEntryPoint getEntryPoint() {
		return new Http403ForbiddenEntryPoint();
	}

	// Checks if the user has been logged in and returns the HttpRequest.getUserPrincipal
	private J2eePreAuthenticatedProcessingFilter getProcessingFilter(AuthenticationManager authManager) {
		J2eePreAuthenticatedProcessingFilter filter = new J2eePreAuthenticatedProcessingFilter();
		filter.setAuthenticationDetailsSource(getAuthenticationDetailsSource());
		filter.setAuthenticationManager(authManager);
		return filter;
	}

	// Checks which roles the user(principal) has by performing HttpRequest.isUserInRole
	private J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource getAuthenticationDetailsSource() {
		J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource authenticationDetailSource = new J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource();
		authenticationDetailSource.setMappableRolesRetriever(getWebXmlSecurityRoles());
		return authenticationDetailSource;
	}

	// Reads the web.xml file 'security-roles'
	private MappableAttributesRetriever getWebXmlSecurityRoles() {
		DelegatedMappableAttributesRetriever attributeRetriever = new DelegatedMappableAttributesRetriever();
		try {// If no web.xml is present this (default) authenticator will fail (and prevent the application from starting up.
			MappableAttributesRetriever webXml = SpringUtils.createBean(getApplicationContext(), WebXmlMappableAttributesRetriever.class);
			attributeRetriever.addMappableAttributes(webXml.getMappableAttributes());
		} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
			log.info("unable to read web.xml file, caught exception: {}", e::getMessage);
		}
		attributeRetriever.addMappableAttributes(new HashSet<>(getSecurityRoles()));
		return attributeRetriever;
	}

	public static class DelegatedMappableAttributesRetriever implements MappableAttributesRetriever {

		private final Set<String> mappableAttributes = new HashSet<>();

		@Override
		public Set<String> getMappableAttributes() {
			return mappableAttributes;
		}

		public void addMappableAttributes(Set<String> mappableAttributes) {
			this.mappableAttributes.addAll(mappableAttributes);
		}
	}
}
