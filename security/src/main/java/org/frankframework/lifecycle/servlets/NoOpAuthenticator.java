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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * NoOp authenticator, all requests are allowed.
 * <p>
 * This authenticator is used to allow all requests without authentication.
 * </p>
 * <p>
 * This is useful for testing purposes or when no authentication is required.
 * </p>
 * <p>
 * This authenticator should be configured by setting its type to 'NONE', for example:
 * <pre>{@code
 * application.security.console.authentication.type=NONE
 * }</pre>
 * </p>
 */
public class NoOpAuthenticator extends AbstractServletAuthenticator {
	private static final String ROLE_PREFIX = "ROLE_"; // See AuthorityAuthorizationManager#ROLE_PREFIX

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		http.anonymous(anonymous -> anonymous.authorities(getAuthorities()).principal("anonymous"));
		return http.build();
	}

	@Override
	protected AuthorizationManager<RequestAuthorizationContext> getAuthorizationManager() {
		return AuthenticatedAuthorizationManager.anonymous();
	}

	private List<GrantedAuthority> getAuthorities() {
		Set<String> securityRoles = getSecurityRoles();
		List<GrantedAuthority> grantedAuthorities = new ArrayList<>(securityRoles.size());
		for (String role : securityRoles) {
			grantedAuthorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
		}
		return grantedAuthorities;
	}
}
