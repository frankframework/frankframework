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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Strings;
import org.springframework.security.oauth2.core.ClaimAccessor;

/**
 * Utility class for mapping authorities from user info or attributes map.
 *
 * @see BearerOnlyAuthenticator
 * @see AuthorityMapper
 */
public class AuthorityMapperUtil {
	private AuthorityMapperUtil() {
		// utility class
	}

	/**
	 * Get roles from OidcUserInfo based on the authoritiesClaimName. Please note that {@code OAuth2Authenticator.configure()} makes sure that a maximum
	 * of only one (1) '.' is allowed in the authoritiesClaimName.
	 */
	static Collection<String> getRolesFromUserInfo(ClaimAccessor userInfo, String authoritiesClaimName) {
		// use a normal get if the key does not contain a '.'
		if (!Strings.CS.contains(authoritiesClaimName, ".")) {
			return userInfo.getClaim(authoritiesClaimName);
		} else {
			String[] keyParts = authoritiesClaimName.split("\\.");

			// get first part of the key
			Map<String, Collection<String>> realmAccess = userInfo.getClaim(keyParts[0]);

			// get second part of the key
			return realmAccess.get(keyParts[1]);
		}
	}

	/**
	 * Get roles from OAuth2UserAuthority userAttributes based on the authoritiesClaimName. Please note that {@code OAuth2Authenticator.configure()} makes
	 * sure that a maximum of only one (1) '.' is allowed in the authoritiesClaimName.
	 */
	static List<String> getRolesFromAttributesMap(Map<String, Object> userAttributes, String authoritiesClaimName) {
		if (userAttributes == null || userAttributes.isEmpty()) {
			return List.of();
		}

		// use a normal get if the key does not contain a '.'
		if (!Strings.CS.contains(authoritiesClaimName, ".")) {
			return (List<String>) userAttributes.get(authoritiesClaimName);
		} else {
			String[] keyParts = authoritiesClaimName.split("\\.");

			// get the first part of the key
			Map<String, Collection<String>> realmAccess = (Map<String, Collection<String>>) userAttributes.get(keyParts[0]);

			// get second part of the key
			return (List<String>) realmAccess.get(keyParts[1]);
		}
	}
}
