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
package org.frankframework.management.security;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;

import lombok.Setter;

public class JwtSecurityFilter implements Filter, InitializingBean { // Should we use the OncePerRequestFilter?
	private static final String JWT_TOKEN_CONTEXT_KEY = "JWT_TOKEN_CONTEXT_KEY";
	private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
	private JwtVerifier jwtVerifier;
	private final Logger log = LogManager.getLogger(JwtSecurityFilter.class);

	@Value("${management.gateway.http.jwks.endpoint}")
	private @Setter String jwksEndpoint;

	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to init
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			org.springframework.security.core.context.SecurityContext newContext = this.securityContextHolderStrategy.createEmptyContext();
			newContext.setAuthentication(getAuthenticationToken((HttpServletRequest) request));
			securityContextHolderStrategy.setContext(newContext);
		} catch (IOException e) {
			this.securityContextHolderStrategy.clearContext();
			log.warn("failed to process authentication request", e);
			throw e;
		}

		chain.doFilter(request, response);
	}

	private Authentication getAuthenticationToken(HttpServletRequest req) throws IOException {
		String jwtHeader = req.getHeader("Authentication");
		if(StringUtils.isEmpty(jwtHeader) || !jwtHeader.contains("Bearer")) {
			this.securityContextHolderStrategy.clearContext();
			log.debug("Failed to process authentication request");
			throw new IOException("no (valid) JWT provided");
		}

		String jwt = jwtHeader.substring(7);
		HttpSession session = req.getSession(true);
		JwtAuthenticationToken storedJWT = (JwtAuthenticationToken) session.getAttribute(JWT_TOKEN_CONTEXT_KEY);

		if(storedJWT != null && storedJWT.verifyJWT(jwt)) {
			log.debug("using stored authentication token [{}]", storedJWT);
			return storedJWT;
		}


		Authentication newToken = jwtVerifier.verify(jwt);
		log.debug("created new authentication token [{}]", newToken);
		session.setAttribute(JWT_TOKEN_CONTEXT_KEY, newToken);
		return newToken;
	}

	@Override
	public void destroy() {
		jwtVerifier = null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(StringUtils.isBlank(jwksEndpoint)) {
			throw new IllegalStateException("no JWKS endpoint specified");
		}

		URL url = new URI(jwksEndpoint).toURL();
		jwtVerifier = new JwtVerifier(JWSAlgorithmFamilyJWSKeySelector.fromJWKSetURL(url));
	}
}
