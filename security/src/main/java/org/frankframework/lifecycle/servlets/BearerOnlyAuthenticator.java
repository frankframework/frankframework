/*
   Copyright 2025 WeAreFrank!

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

import lombok.Setter;

/**
 * Authenticator for bearer-only SSO authentication. This means that the application will not handle user login or logout, but expects the user to be
 * authenticated by an external identity provider (IdP) using a JWT token. This has to be provided in the HTTP Authorization header as a Bearer token.
 * <p>
 * This authenticator should be configured by setting type to 'BEARER_ONLY' and have a issuerUri or jwkSetUri defined, for example:
 * <pre>{@code
 * application.security.console.authentication.type=BEARER_ONLY
 * application.security.console.authentication.issuerUri=https://example.com/realms/myrealm
 * }</pre>
 * </p>
 * <p>
 * Possibly, other optional settings might need to be applied as well. For example, when using Keycloak as IdP, the following settings are common:
 * <pre>{@code
 * application.security.console.authentication.userNameAttributeName=preferred_username
 * application.security.console.authentication.authoritiesClaimName=realm_access.roles
 * }</pre>
 * </p>
 *
 * @author evandongen
 */
public class BearerOnlyAuthenticator extends AbstractServletAuthenticator {

	@Setter
	private String issuerUri;

	@Setter
	private String jwkSetUri;

	/**
	 * If set, use this URI to obtain user info from the IdP with the access token.
	 * This is optional, as all required user info might already be present in the JWT token
	 */
	@Setter
	private String userInfoUri;

	/**
	 * The claim name in the JWT token that contains the preferred username of the user.
	 * Defaults to "sub", which is the standard claim for subject identifier. But, when using Keycloak, it is common to use "preferred_username" instead.
	 * @see "JwtAuthenticationConverter#principalClaimName"
	 */
	@Setter
	private String userNameAttributeName;

	/**
	 * <p>The claim name in the JWT token that contains the authorities of the user.
	 * Defaults to any of {@code JwtGrantedAuthoritiesConverter#WELL_KNOWN_AUTHORITIES_CLAIM_NAMES} when this value is not set.</p>
	 * <p>For keycloak, "realm_access.roles" is the standard claim, this is a 'nested' value. When we encounter a dot (.) in the claim name,
	 * we assume it is a nested claim and use the custom mapper.</p>
	 *
	 * @ff.tip can only contain one dot (.) to indicate a nested claim, e.g. "realm_access.roles".
	 */
	@Setter
	private String authoritiesClaimName;

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		if (StringUtils.isAllBlank(issuerUri, jwkSetUri)) {
			throw new IllegalArgumentException("Configuring issuerUri and/or jwkSetUri is mandatory to use BearerOnlyAuthenticator");
		}

		if (StringUtils.countMatches(authoritiesClaimName, ".") > 1) {
			throw new IllegalArgumentException("The authoritiesClaimName must not contain more than one dot (.) to indicate a nested claim. Found: " + authoritiesClaimName);
		}

		http.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.decoder(getJwtDecoder())
						.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter = getJwtCollectionConverter();

		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

		if (StringUtils.isNotBlank(userNameAttributeName)) {
			jwtAuthenticationConverter.setPrincipalClaimName(userNameAttributeName);
		}

		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
		return jwtAuthenticationConverter;
	}

	/**
	 * <p>Determines the converter to use for extracting authorities from the JWT token.</p>
	 *
	 * @return the converter to use for extracting authorities from the JWT token
	 */
	Converter<Jwt, Collection<GrantedAuthority>> getJwtCollectionConverter() {
		// use default converter when no nested claim is used and no userInfoUri is set
		if (!Strings.CS.contains(authoritiesClaimName, ".") && StringUtils.isBlank(userInfoUri)) {
			JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

			if (StringUtils.isNotBlank(authoritiesClaimName)) {
				grantedAuthoritiesConverter.setAuthoritiesClaimName(authoritiesClaimName);
			}

			grantedAuthoritiesConverter.setAuthorityPrefix(DEFAULT_ROLE_PREFIX);

			return grantedAuthoritiesConverter;
		}

		return jwt -> getListOfRoles(jwt).stream()
					.map(role -> new SimpleGrantedAuthority(DEFAULT_ROLE_PREFIX + role))
					.collect(Collectors.toList());
	}

	/**
	 * <ul>
	 *   <li>If the userInfoUri is set, we obtain the user info from the IdP using the access token and extract the roles from there</li>
	 *   <li>Otherwise, we get the roles from the given jwt</li>
	 * </ul>
	 */
	private List<String> getListOfRoles(Jwt jwt) {
		if (StringUtils.isNotBlank(userInfoUri)) {
			return getRolesFromUserInfoUri(jwt.getTokenValue());
		}

		// get roles from given jwt
		return AuthorityMapperUtil.getRolesFromUserInfo(jwt, authoritiesClaimName).stream().toList();
	}

	List<String> getRolesFromUserInfoUri(String accessToken) {
		Map<String, Object> userInfo = RestClient.create()
				.get()
				.uri(userInfoUri)
				.accept(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});

		return AuthorityMapperUtil.getRolesFromAttributesMap(userInfo, authoritiesClaimName);
	}

	private JwtDecoder getJwtDecoder() {
		if (StringUtils.isNotBlank(issuerUri)) {
			return JwtDecoders.fromIssuerLocation(issuerUri);
		} else if (StringUtils.isNotBlank(jwkSetUri)) {
			return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
		}

		throw new IllegalArgumentException("Either issuerUri or jwkSetUri must be provided");
	}
}
