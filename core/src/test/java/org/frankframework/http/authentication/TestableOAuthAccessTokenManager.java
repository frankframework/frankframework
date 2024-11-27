package org.frankframework.http.authentication;

import org.frankframework.util.CredentialFactory;

/**
 * Used in both {@link OAuthAccessTokenManagerRequestTest} and {@link OAuthAccessTokenKeycloakTest}
 */
public class TestableOAuthAccessTokenManager extends OAuthAccessTokenManager {

	private static final String SCOPE = "email";

	public TestableOAuthAccessTokenManager(boolean useClientCredentials, AuthenticationType type, String clientId, String clientSecret, String tokenEndpoint) throws HttpAuthenticationException {
		super(tokenEndpoint, SCOPE, new CredentialFactory(null, clientId, clientSecret), useClientCredentials, type, null, -1);
	}
}
