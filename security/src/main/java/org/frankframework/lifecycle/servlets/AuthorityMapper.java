/*
   Copyright 2023-2026 WeAreFrank!

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

import static org.frankframework.lifecycle.servlets.AbstractServletAuthenticator.DEFAULT_ROLE_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.StringResolver;

@Log4j2
public class AuthorityMapper implements GrantedAuthoritiesMapper {
	private final Map<GrantedAuthority, String> authorityToRoleMapping = new HashMap<>();

	private String authoritiesClaimName;

	public AuthorityMapper(URL roleMappingURL, Set<String> roles, Properties properties) throws IOException {
		Properties roleMappingProperties = new Properties();

		try (InputStream stream = roleMappingURL.openStream()) {
			roleMappingProperties.load(stream);
		} catch (IOException e) {
			throw new IOException("unable to open role-mapping file [" + roleMappingURL + "]", e);
		}

		for (String role : roles) {
			String value = roleMappingProperties.getProperty(role);
			if (StringUtils.isEmpty(value)) {
				log.warn("role [{}] has not been mapped to anything, ignoring this role", role);
				continue;
			}

			String resolvedValue = StringResolver.substVars(value, properties);
			if (StringUtils.isNotEmpty(resolvedValue)) {
				GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_" + role);
				authorityToRoleMapping.put(grantedAuthority, resolvedValue);
				log.info("mapped role [{}] to [{}]", resolvedValue, grantedAuthority);
			}
		}
	}

	/**
	 * Overloaded constructor to support a custom authorities claim name.
	 */
	public AuthorityMapper(URL roleMappingURL, Set<String> securityRoles, Properties environmentProperties, String authoritiesClaimName) throws IOException {
		this(roleMappingURL, securityRoles, environmentProperties);

		this.authoritiesClaimName = authoritiesClaimName;
	}

	@NonNull
	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
		List<String> canonicalRoleNames = authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
		List<String> userRoles = getUserRolesFrom(authorities);

		log.debug("Canonical role names from granted authorities: {}", () -> canonicalRoleNames);
		log.debug("User roles from token claims: {}", () -> userRoles);

		authorityToRoleMapping.forEach((authority, role) -> {
			if (canonicalRoleNames.contains(role) || userRoles.contains(role)) {
				log.debug("Found granted authority [{}] for role [{}]", authority::getAuthority, () -> role);

				mappedAuthorities.add(authority);
			}
		});

		return mappedAuthorities;
	}

	private List<String> getUserRolesFrom(Collection<? extends GrantedAuthority> authorities) {
		if (StringUtils.isBlank(authoritiesClaimName)) {
			log.debug("No authoritiesClaimName configured, skipping user roles from token");
			return List.of();
		}

		// get all user roles from the token and add them to the canonicalRoleNames
		return authorities.stream()
				.filter(OAuth2UserAuthority.class::isInstance) // OidcUserAuthority extends OAuth2UserAuthority
				.map(this::getValuesFromToken)
				.flatMap(Collection::stream)
				.map(role -> DEFAULT_ROLE_PREFIX + role)
				.toList();
	}

	/**
	 *	The given authority should be OAuth2UserAuthority or OidcUserAuthority. Check their attributes for the key.
	 * 	Spring always maps one or the other. See <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-map-authorities">spring security documentation</a>
	 * 	for more information.
	 */
	private List<String> getValuesFromToken(GrantedAuthority authority) {
		if (authority instanceof OidcUserAuthority oidcUserAuthority) {
			return AuthorityMapperUtil.getRolesFromUserInfo(oidcUserAuthority.getUserInfo(), authoritiesClaimName).stream().toList();
		} else if (authority instanceof OAuth2UserAuthority oAuth2UserAuthority) {
			return AuthorityMapperUtil.getRolesFromAttributesMap(oAuth2UserAuthority.getAttributes(), authoritiesClaimName);
		}

		return List.of();
	}
}
