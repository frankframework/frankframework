/*
   Copyright 2026 WeAreFrank!

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import lombok.Setter;

/**
 * Abstract base class for OAuth2-based authenticators, providing shared configuration fields
 * for JWT/OIDC-based authentication flows.
 *
 * @see OAuth2Authenticator
 * @see BearerOnlyAuthenticator
 */
public abstract class AbstractOAuth2Authenticator extends AbstractServletAuthenticator {

	/**
	 * Sets the expected audience claim of the JWT token to validate.
	 * If set, the JWT token must contain this value in its {@code aud} claim.
	 */
	@Setter
	protected String audience;

	/**
	 * The issuer identifier URI of the authorization server. This is used to validate the issuer claim in ID tokens.
	 * <p>
	 * e.g. {@code https://accounts.google.com}
	 * </p>
	 */
	@Setter
	protected String issuerUri;

	/**
	 * The URI of the JSON Web Key (JWK) set containing the public keys used to verify any JWT token issued by the authorization server.
	 * <p>
	 * e.g. {@code https://www.googleapis.com/oauth2/v3/certs}
	 * </p>
	 */
	@Setter
	protected String jwkSetUri;

	/**
	 * The URI of the user info endpoint used to retrieve information about the authenticated user.
	 * <p>
	 * This is optional, as all required user info might already be present in the JWT token.
	 * </p>
	 * <p>
	 * e.g. {@code https://www.googleapis.com/oauth2/v3/userinfo}
	 * </p>
	 */
	@Setter
	protected String userInfoUri;

	/**
	 * The attribute name used to extract the username from the OAuth2 user information or JWT token.
	 * Different OAuth2 providers may use different attribute names to identify the user.
	 * <p>
	 * Common values include:
	 * <ul>
	 *   <li>{@code sub} - The subject identifier</li>
	 *   <li>{@code email} - The user's email address</li>
	 *   <li>{@code preferred_username} - The user's preferred username</li>
	 * </ul>
	 * </p>
	 */
	@Setter
	protected String userNameAttributeName;

	/**
	 * <p>The claim name in the JWT token that contains the authorities of the user.
	 * Defaults to any of {@code JwtGrantedAuthoritiesConverter#WELL_KNOWN_AUTHORITIES_CLAIM_NAMES} when this value is not set.</p>
	 * <p>For keycloak, "realm_access.roles" is the standard claim, this is a 'nested' value. When we encounter a dot (.) in the claim name,
	 * we assume it is a nested claim and use the custom mapper.</p>
	 *
	 * @ff.tip can only contain one dot (.) to indicate a nested claim, e.g. "realm_access.roles".
	 */
	@Setter
	protected String authoritiesClaimName;

	/**
	 * Enables bearer-token (JWT) validation as an OAuth2 resource server on the given {@link HttpSecurity}.
	 * Validates the JWT configuration and installs the shared decoder + authority converter.
	 */
	protected void configureBearerTokenResourceServer(HttpSecurity http) {
		if (StringUtils.isAllBlank(issuerUri, jwkSetUri)) {
			throw new IllegalArgumentException("Configuring issuerUri and/or jwkSetUri is mandatory to validate bearer tokens");
		}

		if (StringUtils.countMatches(authoritiesClaimName, ".") > 1) {
			throw new IllegalArgumentException("The authoritiesClaimName must not contain more than one dot (.) to indicate a nested claim. Found: " + authoritiesClaimName);
		}

		if (StringUtils.isBlank(userNameAttributeName)) {
			userNameAttributeName = JwtClaimNames.SUB;
		}

		http.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.decoder(getJwtDecoder())
						.jwtAuthenticationConverter(this::jwtAuthenticationTokenConverter)));
	}

	/**
	 * Our own implementation similar to Spring's {@link JwtAuthenticationConverter}.
	 * Unlike Spring's Converter this one is capable of:
	 * <ul>
	 * <li>Enriching the JWT claimset by calling the 'UserInfo' endpoint.</li>
	 * <li>Splitting nested roles, eg. {@code realm_access.roles}.</li>
	 * <li>Splitting a claim String on both `comma's` and `spaces`.</li>
	 * <li>Splitting a single entry Claim list on both `comma's` and `spaces`.</li>
	 * <li>Validating if the found Authorities may access the target resource.</li>
	 * </ul>
	 */
	protected AbstractAuthenticationToken jwtAuthenticationTokenConverter(Jwt jwt) {
		if (StringUtils.isNotBlank(audience) && (jwt.getAudience() == null || !jwt.getAudience().contains(audience))) {
			log.warn("JWT token audience [{}] does not contain the required audience [{}]", jwt.getAudience(), audience);
			throw new InvalidBearerTokenException("JWT token audience does not match expected audience");
		}

		if (StringUtils.isNotBlank(userInfoUri)) {
			log.debug("Fetching user roles from userInfoUri [{}]", userInfoUri);
			jwt = updateJwtWithUserInfoUri(jwt);
		}

		Collection<GrantedAuthority> authorities = new HashSet<>(getGrantedAuthorities(jwt));
		authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.BEARER_AUTHORITY));

		String principalClaimValue = jwt.getClaimAsString(userNameAttributeName);
		AbstractAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities, principalClaimValue);

		// If Authorities are set, the user is authenticated if the user has at least one of the required roles
		if (!getAuthorities().isEmpty()) {
			boolean result = !Collections.disjoint(getAuthorities(), token.getAuthorities());
			token.setAuthenticated(result);
			log.info("User {} required role(s) {}", () -> result ? "contains" : "does not contain", this::getAuthorities);
		}

		return token;
	}

	/**
	 * <p>Determines the converter to use for extracting authorities from the JWT token.</p>
	 * See {@link JwtGrantedAuthoritiesConverter} for the default implementation.
	 * This one splits on both spaces and comma's, as well as a single line roles list.
	 *
	 * @return the converter to use for extracting authorities from the JWT token
	 */
	Collection<GrantedAuthority> getGrantedAuthorities(Jwt jwt) {
		log.debug("Using custom Jwt to GrantedAuthorities converter for authoritiesClaimName [{}]", authoritiesClaimName);
		return AuthorityMapperUtil.getRolesFromClaim(jwt, authoritiesClaimName).stream()
				.map(role -> new SimpleGrantedAuthority(DEFAULT_ROLE_PREFIX + role))
				.collect(Collectors.toList());
	}

	private Jwt updateJwtWithUserInfoUri(Jwt jwt) {
		final Map<String, Object> userInfo;
		try {
			userInfo = RestClient.create()
					.get()
					.uri(userInfoUri)
					.accept(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});
		} catch (HttpClientErrorException e) {
			log.warn("userInfo endpoint exception, status code [{}]", e.getStatusCode().value(), e);
			return jwt;
		}

		log.debug("Fetched user info: {}", userInfo);
		return new Jwt(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getHeaders(), userInfo);
	}

	private JwtDecoder getJwtDecoder() {
		if (StringUtils.isNotBlank(issuerUri)) {
			log.debug("Creating JwtDecoder from issuerUri [{}]", issuerUri);
			return JwtDecoders.fromIssuerLocation(issuerUri);
		} else if (StringUtils.isNotBlank(jwkSetUri)) {
			log.debug("Creating JwtDecoder from jwkSetUri [{}]", jwkSetUri);
			return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
		}

		throw new IllegalArgumentException("Either issuerUri or jwkSetUri must be provided");
	}
}
