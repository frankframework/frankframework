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

import static org.frankframework.management.security.JwtKeyGenerator.JWT_DEFAULT_SIGNING_ALGORITHM;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import lombok.Setter;

public class JwtSecurityFilter implements Filter, InitializingBean { //OncePerRequestFilter
	private static final String JWT_TOKEN_CONTEXT_KEY = "JWT_TOKEN_CONTEXT_KEY";
	private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
	private final Logger log = LogManager.getLogger(JwtSecurityFilter.class);

	@Value("${management.gateway.http.jwks.endpoint}")
	private @Setter String jwksEndpoint;

	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// nothing to init
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


		Authentication newToken = createAuthenticationToken(jwt);
		log.debug("created new authentication token [{}]", newToken);
		session.setAttribute(JWT_TOKEN_CONTEXT_KEY, newToken);
		return newToken;
	}

	private Authentication createAuthenticationToken(String jwt) throws IOException {
		JWTClaimsSet claimsSet;
		try {
			claimsSet = jwtProcessor.process(jwt, null);
		} catch (JOSEException | ParseException | BadJOSEException e) {
			throw new IOException("unable to parse JWT", e);
		}

		try {
			return new JwtAuthenticationToken(claimsSet, jwt);
		} catch (ParseException e) {
			throw new IOException("unable to create AuthenticationToken", e);
		}
	}

	@Override
	public void destroy() {
		jwtProcessor = null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(StringUtils.isBlank(jwksEndpoint)) {
			throw new IllegalStateException("no JWKS endpoint specified");
		}

		URL url = new URL(jwksEndpoint);
		JWKSource<SecurityContext> keySource = JWKSourceBuilder.create(url).cacheForever().build();
		jwtProcessor = new DefaultJWTProcessor<>();
		JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWT_DEFAULT_SIGNING_ALGORITHM, keySource);
		jwtProcessor.setJWSKeySelector(keySelector);
	}
}
