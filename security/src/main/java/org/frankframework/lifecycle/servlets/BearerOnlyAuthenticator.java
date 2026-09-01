/*
   Copyright 2025-2026 WeAreFrank!

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

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
public class BearerOnlyAuthenticator extends AbstractOAuth2Authenticator {

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		configureBearerTokenResourceServer(http);
		return http.build();
	}
}
