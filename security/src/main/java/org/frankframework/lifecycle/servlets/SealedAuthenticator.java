/*
   Copyright 2024 WeAreFrank!

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

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sealed authenticator, all requests are blocked.
 */
public class SealedAuthenticator extends AbstractServletAuthenticator {

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		http.httpBasic(basic -> basic.authenticationEntryPoint(new Http401EntryPoint())); // Uses a BasicAuthenticationEntryPoint to force users to log in

		InMemoryUserDetailsManager udm = new InMemoryUserDetailsManager(); // Create an UserDetailsManager without any users.
		http.userDetailsService(udm);

		return http.build();
	}

	private static class Http401EntryPoint implements AuthenticationEntryPoint {

		@Override
		public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
			// Block all requests
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied, configure an authenticator to enable web access.");
		}
	}
}
