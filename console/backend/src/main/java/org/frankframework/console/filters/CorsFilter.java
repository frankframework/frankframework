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
package org.frankframework.console.filters;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;

import org.frankframework.util.StringUtil;

public class CorsFilter implements Filter {
	private final Logger secLog = LogManager.getLogger("SEC");
	private final Logger log = LogManager.getLogger(this);

	@Value("${cors.origin:*}")
	private String allowedCorsOrigins; // Defaults to ALL allowed

	@Value("${cors.exposeHeaders:Allow, ETag, Content-Disposition}")
	private String exposedCorsHeaders;

	@Value("${cors.allowMethods:GET, POST, PUT, DELETE, OPTIONS, HEAD}")
	private String allowedCorsMethods;

	@Value("${cors.enforced:false}")
	private boolean enforceCORS;

	private final CorsConfiguration config = new CorsConfiguration();
	private static final String SEC_LOG_MESSAGE = "host [{}] tried to access uri [{}] with origin header [{}]. The request was {} due to CORS restrictions, allowed origins [{}]";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		List<String> allowedOrigins = StringUtil.split(allowedCorsOrigins);
		for (String domain : allowedOrigins) {
			if("*".equals(domain) || !domain.contains("*")) {
				config.addAllowedOrigin(domain);
			} else {
				config.addAllowedOriginPattern(domain);
			}
		}

		StringUtil.split(allowedCorsMethods).stream().forEach(config::addAllowedMethod);
		StringUtil.split(exposedCorsHeaders).stream().forEach(config::addExposedHeader);

		config.applyPermitDefaultValues(); // Ensures the headers are set and valid.

		exposedCorsHeaders = String.join(",", config.getExposedHeaders());
		allowedCorsMethods = String.join(",", config.getAllowedMethods());

		log.debug("whitelisted CORS origins: {} and patterns: {}", config::getAllowedOrigins, config::getAllowedOriginPatterns);
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		/**
		 * Handle Cross-Origin Resource Sharing
		 */
		if (CorsUtils.isCorsRequest(request)) {
			String originHeader = request.getHeader(HttpHeaders.ORIGIN);
			String origin = config.checkOrigin(originHeader);
			if (enforceCORS) {
				if (origin != null) { // Happy Flow
					setResponseHeaders(request, response, origin);
				} else { // If origin has been set, but has not been whitelisted, report the request in security log.
					secLog.info(SEC_LOG_MESSAGE, request::getRemoteHost, request::getPathInfo, () -> originHeader, () -> "BLOCKED", () -> allowedCorsOrigins);
					log.warn("blocked request with origin [{}]", originHeader);
					response.setStatus(400);
					return; // Actually block the request
				}
			} else if (origin == null) { // FLAG the request
				secLog.info(SEC_LOG_MESSAGE, request::getRemoteHost, request::getPathInfo, () -> originHeader, () -> "FLAGGED", () -> allowedCorsOrigins);
				log.warn("flagged request with origin [{}]", originHeader);
			}
		}

		/**
		 * Pass request down the chain, except for OPTIONS
		 */
		// Return standard response if OPTIONS request w/o Origin header
		if(CorsUtils.isPreFlightRequest(request)) {
			response.setHeader("Allow", allowedCorsMethods);
			response.setStatus(200);
		} else {
			chain.doFilter(request, response);
		}
	}

	/** Set the CORS headers on the HTTP response */
	private void setResponseHeaders(HttpServletRequest request, HttpServletResponse response, String origin) {
		// If we pass one of the valid domains, it can be used to spoof the connection
		response.setHeader("Access-Control-Allow-Origin", origin);

		String requestHeaders = request.getHeader("Access-Control-Request-Headers");
		if (requestHeaders != null) {
			response.setHeader("Access-Control-Allow-Headers", requestHeaders);
		}

		response.setHeader("Access-Control-Expose-Headers", exposedCorsHeaders);
		response.setHeader("Access-Control-Allow-Methods", allowedCorsMethods);

		// Allow caching cross-domain permission
		response.setHeader("Access-Control-Max-Age", "3600");
	}

	@Override
	public void destroy() {
		// nothing to destroy
	}
}
