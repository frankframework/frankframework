package org.frankframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.stream.Stream;

import lombok.extern.log4j.Log4j2;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.frankframework.http.AbstractHttpSession;
import org.frankframework.http.HttpSender;
import org.frankframework.senders.SenderTestBase;

import org.frankframework.testutil.TestAssertions;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Tests whether requests for access tokens actually work using a Keycloak test container
 *
 * @author erik.van.dongen
 * @see "https://github.com/dasniko/testcontainers-keycloak"
 */
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
@Log4j2
public class OAuthAccessTokenKeycloakTest extends SenderTestBase<HttpSender> {

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer()
			.withRealmImportFile("/Http/Authentication/iaf-test.json");

	private static final String CLIENT_ID = "testiaf-client";

	private static final String CLIENT_SECRET = "testiaf-client-pwd";

	private static final String TOKEN_ENDPOINT_FORMAT = "http://localhost:%s/realms/iaf-test/protocol/openid-connect/token";

	private final Credentials credentials = new UsernamePasswordCredentials("fakeCredentialUserName", "fakeCredentialPassword");

	public static Stream<Arguments> parameters() {
		return Stream.of(
				Arguments.of(AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH, true),
				Arguments.of(AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS, true),

				// Keycloak does not store passwords directly, so resource owner password credentials is impossible.
				Arguments.of(AbstractHttpSession.OauthAuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_BASIC_AUTH, false),
				Arguments.of(AbstractHttpSession.OauthAuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_QUERY_PARAMETERS, false)
		);
	}

	@Override
	public HttpSender createSender() throws Exception {
		HttpSender sender = new HttpSender();

		sender.setName("Http Sender");
		sender.setTokenEndpoint(getTestEndpoint());
		sender.setUrl("http://localhost");
		sender.setClientId(CLIENT_ID);
		sender.setClientSecret(CLIENT_SECRET);
		sender.setScope("email");
		sender.setUsername("fakeCredentialUserName");
		sender.setPassword("fakeCredentialPassword");
		sender.setTimeout(120000);

		sender.configure();
		sender.start();

		return sender;
	}

	@MethodSource("parameters")
	@ParameterizedTest
	void testGetAccessToken(AbstractHttpSession.OauthAuthenticationMethod oauthAuthenticationMethod, boolean shouldResolveSuccessfully) throws Exception {
		assumeFalse(TestAssertions.isTestRunningOnGitHub());

		var authenticator = oauthAuthenticationMethod.newAuthenticator(sender);

		if (shouldResolveSuccessfully) {
			String accessToken = authenticator.getOrRefreshAccessToken(credentials, false);

			assertNotNull(accessToken);
			assertTrue(accessToken.length() > 50, "Length of accessToken is short, which could indicate that it failed");
		} else {
			assertThrows(HttpAuthenticationException.class, () -> authenticator.getOrRefreshAccessToken(credentials, false));
		}
	}

	private String getTestEndpoint() {
		return String.format(TOKEN_ENDPOINT_FORMAT, keycloak.getHttpPort());
	}

}
