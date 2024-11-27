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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.management.bus.BusMessageUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SecurityLogFilter extends OncePerRequestFilter {

	private static final Logger SEC_LOG = LogManager.getLogger("SEC");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		// Log POST, PUT and DELETE requests at info level
		final String method = request.getMethod();
		if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
			SEC_LOG.debug("received http request with URI [{}:{}] issued by [{}]", () -> method, request::getRequestURI, BusMessageUtils::getUserPrincipalName);
		} else {
			SEC_LOG.info("received http request with URI [{}:{}] issued by [{}]", () -> method, request::getRequestURI, BusMessageUtils::getUserPrincipalName);
		}

		filterChain.doFilter(request, response);
	}
}