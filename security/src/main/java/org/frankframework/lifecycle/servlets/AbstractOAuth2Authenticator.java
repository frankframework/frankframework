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

}
