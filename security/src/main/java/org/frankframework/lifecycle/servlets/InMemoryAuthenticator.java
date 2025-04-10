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

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Setter;

/**
 * Authenticator for in-memory authentication.
 * <p>
 * This authenticator provides a simple way to authenticate using a single username and password stored in memory.
 * </p>
 * <p>
 * This authenticator should be configured by setting its type to 'IN_MEMORY', for example:
 * <pre>
 * application.security.console.authentication.type=IN_MEMORY
 * application.security.console.authentication.username=admin
 * application.security.console.authentication.password=secret
 * </pre>
 * </p>
 */
public class InMemoryAuthenticator extends AbstractServletAuthenticator {
	/**
	 * The username to use for authentication.
	 */
	private @Setter String username = null;
	/**
	 * The password to use for authentication.
	 */
	private @Setter String password = null;

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		http.httpBasic(basic -> basic.realmName("Frank")); // Uses a BasicAuthenticationEntryPoint to force users to log in

		UserDetails user = User.builder()
				.username(username)
				.password("{noop}"+password)
				.roles(getSecurityRoles().toArray(new String[0]))
				.build();

		InMemoryUserDetailsManager udm = new InMemoryUserDetailsManager(user);
		http.userDetailsService(udm);

		return http.build();
	}
}
