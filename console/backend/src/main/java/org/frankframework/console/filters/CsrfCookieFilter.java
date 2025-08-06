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
package org.frankframework.console.filters;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class CsrfCookieFilter extends OncePerRequestFilter {

	@Value("${csrf.enabled:true}")
	private boolean csrfEnabled;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		if(csrfEnabled) {
			CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
			if(csrfToken == null) {
				response.sendError(500, "CSRF is enabled but cannot be found in the Spring Context");
				return;
			}

			// Required to retrieve the `deferred token` and store it as a Cookie, see SpaCsrfTokenRequestHandler
			// and CookieCsrfTokenRepository#loadDeferredToken.
			csrfToken.getToken();
		}
		filterChain.doFilter(request, response);
	}
}
