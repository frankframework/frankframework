/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.web;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

import nl.nn.adapterframework.lifecycle.DynamicRegistration;

@Configuration
@EnableWebSecurity //Enables Spring Security (classpath)
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = false) //Enables JSR 250 (JAX-RS) annotations
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityChainConfigurer {

	private SecurityFilterChain configureAuthenticator(HttpSecurity http) throws Exception {
		http.anonymous().authorities(getRolePrefixedAuthorities());
		return http.build();
	}

	// Adds AuthorityAuthorizationManager#ROLE_PREFIX
	private List<GrantedAuthority> getRolePrefixedAuthorities() {
		return Arrays.asList(DynamicRegistration.ALL_IBIS_USER_ROLES).stream().map(e -> "ROLE_" + e).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}

	@Bean
	public SecurityFilterChain configureChain(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.securityMatcher( new MatchAllButJWKS() );
		http.headers().frameOptions().sameOrigin(); //Allow same origin iframe request
		http.csrf().disable();
		http.formLogin().disable(); //Disable the form login filter
		http.anonymous().disable(); //Disable the default anonymous filter

		return configureAuthenticator(http);
	}

	private static class MatchAllButJWKS implements RequestMatcher {
		private static final String JWKS_ENDPOINT = "/iaf/management/jwks";

		@Override
		public boolean matches(HttpServletRequest request) {
			return !JWKS_ENDPOINT.equals(request.getServletPath());
		}

		@Override
		public String toString() {
			return "All endpoints except ["+JWKS_ENDPOINT+"]";
		}
	}
}
