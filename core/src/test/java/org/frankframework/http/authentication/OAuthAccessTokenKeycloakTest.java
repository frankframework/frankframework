package org.frankframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Tests whether requests for access tokens actually work using a Keycloak test container
 *
 * @author erik.van.dongen
 * @see "https://github.com/dasniko/testcontainers-keycloak"
 */
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
public class OAuthAccessTokenKeycloakTest {

	@Container
	static final KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("/Http/Authentication/iaf-test.json");

	private static final String CLIENT_ID = "testiaf-client";

	private static final String CLIENT_SECRET = "testiaf-client-pwd";

	private static final String TOKEN_ENDPOINT_FORMAT = "http://localhost:%s/realms/iaf-test/protocol/openid-connect/token";

	private final Credentials credentials = new UsernamePasswordCredentials("fakeCredentialUserName", "fakeCredentialPassword");

	public static Stream<Arguments> parameters() {
		return Stream.of(
				Arguments.of(true, OAuthAccessTokenManager.AuthenticationType.REQUEST_PARAMETER),
				Arguments.of(false, OAuthAccessTokenManager.AuthenticationType.REQUEST_PARAMETER),
				Arguments.of(true, OAuthAccessTokenManager.AuthenticationType.AUTHENTICATION_HEADER),
				Arguments.of(false, OAuthAccessTokenManager.AuthenticationType.AUTHENTICATION_HEADER)
		);
	}

	@MethodSource("parameters")
	@ParameterizedTest
	void testGetAccessToken(boolean useCredentials, OAuthAccessTokenManager.AuthenticationType authenticationType) throws Exception {
		TestableOAuthAccessTokenManager tokenManager = getTokenManager(useCredentials, authenticationType);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);
		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost apacheRequest = (HttpPost) tokenManager.convertToApacheHttpRequest(httpRequest);

			try (CloseableHttpResponse apacheResponse = httpClient.execute(apacheRequest)) {
				if (!useCredentials) {
					// expect this to go wrong because 'direct access grants' is disabled for 'iaf-test' realm
					assertEquals(400, apacheResponse.getStatusLine().getStatusCode());
				} else {
					HTTPResponse response = tokenManager.convertFromApacheHttpResponse(apacheResponse);
					TokenResponse tokenResponse = TokenResponse.parse(response);
					AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();

					AccessToken accessToken = successResponse.getTokens().getAccessToken();
					assertNotNull(accessToken);
				}
			}
		}
	}

	private TestableOAuthAccessTokenManager getTokenManager(boolean useClientCredentials, OAuthAccessTokenManager.AuthenticationType authenticationType) throws Exception {
		return new TestableOAuthAccessTokenManager(useClientCredentials, authenticationType, CLIENT_ID, CLIENT_SECRET, getTestEndpoint());
	}

	private String getTestEndpoint() {
		return String.format(TOKEN_ENDPOINT_FORMAT, keycloak.getHttpPort());
	}
}
