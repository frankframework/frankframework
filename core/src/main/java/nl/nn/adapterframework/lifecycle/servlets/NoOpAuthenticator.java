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
import java.util.List;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import nl.nn.adapterframework.lifecycle.ServletManager;

public class NoOpAuthenticator implements IAuthenticator {
	private static final String ROLE_PREFIX = "ROLE_"; //see AuthorityAuthorizationManager#ROLE_PREFIX

	@Override
	public SecurityFilterChain configure(ServletConfiguration config, HttpSecurity http) throws Exception {
		http.anonymous().authorities(getAuthorities(config.getSecurityRoles()));
		return http.build();
	}

	private static List<GrantedAuthority> getAuthorities(List<String> securityRoles) {
		if(securityRoles == null || securityRoles.isEmpty()) {
			securityRoles = ServletManager.DEFAULT_IBIS_ROLES;
		}
		List<GrantedAuthority> grantedAuthorities = new ArrayList<>(securityRoles.size());
		for (String role : securityRoles) {
			grantedAuthorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
		}
		return grantedAuthorities;
	}
}
