package org.frankframework.http.authentication;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import org.frankframework.http.AbstractHttpSession;
import org.frankframework.http.HttpSender;

public class OAuthAccessTokenAuthenticatorTest {

	@RegisterExtension
	public WireMockExtension tokenServer = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	private HttpSender httpSender;

	@BeforeEach
	public void setup() throws Exception {
		httpSender = new HttpSender();
		httpSender.setUrl("https://dummy");
		httpSender.configure();
		httpSender.start();

		MockTokenServer.createStubs(tokenServer);
	}

	private String getEndpoint() {
		return "http://localhost:"+tokenServer.getPort();
	}

	@Test
	public void testRetrieveAccessTokenWithClientCredentialsGrant() throws Exception {
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;

		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("email");
		httpSender.setClientId(clientId);
		httpSender.setClientSecret(clientSecret);

		httpSender.configure();
		httpSender.start();

		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);

		String accessToken = authenticator.getOrRefreshAccessToken(new UsernamePasswordCredentials(clientId, clientSecret), true);

		assertNotNull(accessToken);
		assertTrue(accessToken.length() > 5, "accessToken should contain a string");
	}

	@Test
	public void testRetrieveAccessTokenWithResourceOwnerPasswordGrant() throws Exception {
		String username = "fakeUsername";
		String password = "fakePassword";

		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		httpSender.configure();
		httpSender.start();

		Credentials credentials = new UsernamePasswordCredentials(username, password);
		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);

		String accessToken = authenticator.getOrRefreshAccessToken(credentials, true);

		assertNotNull(accessToken);
		assertTrue(accessToken.length() > 5, "accessToken should contain a string");
	}

	@Test
	public void testRetrieveAccessTokenWithAuthHeader() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		httpSender.configure();
		httpSender.start();

		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);

		String accessToken = authenticator.getOrRefreshAccessToken(null, true);

		assertNotNull(accessToken);
		assertTrue(accessToken.length() > 5, "accessToken should contain a string");
	}

	@Test
	public void testRetrieveAccessTokenNoScope() throws Exception {
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;

		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope(null);
		httpSender.setClientId(clientId);
		httpSender.setClientSecret(clientSecret);

		httpSender.configure();
		httpSender.start();

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);
		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);

		String accessToken = authenticator.getOrRefreshAccessToken(credentials, true);

		assertNotNull(accessToken);
		assertTrue(accessToken.length() > 5, "accessToken should contain a string");
	}

	@Test
	public void testRetrieveAccessTokenWrongTokenEndpoint() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.PATH + "/xxxxx");
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		httpSender.configure();
		httpSender.start();

		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);

		assertThrows(HttpAuthenticationException.class, () -> authenticator.getOrRefreshAccessToken(null, true));
	}

	@Test
	public void testRetrieveAccessTokenFirstExpiredForced() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.EXPIRED_PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		httpSender.configure();
		httpSender.start();

		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);

		tokenServer.resetScenarios();
		assertThat(authenticator.getOrRefreshAccessToken(null, true), containsString("Expired"));

		assertThat(authenticator.getOrRefreshAccessToken(null, true), not(containsString("Expired")));
	}

	@Test
	public void testRetrieveAccessTokenFirstExpiredAutomatic() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.EXPIRED_PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("read, email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		httpSender.configure();
		httpSender.start();

		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);

		tokenServer.resetScenarios();
		assertThat(authenticator.getOrRefreshAccessToken(null, true), containsString("Expired"));

		Thread.sleep(100);
		assertThat(authenticator.getOrRefreshAccessToken(null, true), not(containsString("Expired")));
	}

	@Test
	public void testRequestTimedOut() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.DELAYED_PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setTimeout(1000);
		httpSender.setScope("read, email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		httpSender.configure();

		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);

		HttpAuthenticationException exception = assertThrows(HttpAuthenticationException.class, () -> authenticator.getOrRefreshAccessToken(null, true));
		assertEquals("timeout of [1000] ms exceeded: (SocketException) Socket closed", exception.getMessage());
	}

	@Test
	public void testRequestDoesNotTimeout() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.DELAYED_PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setTimeout(10000);
		httpSender.setScope("read, email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		httpSender.configure();
		httpSender.start();

		var authenticator = AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);

		String token = assertDoesNotThrow(() -> authenticator.getOrRefreshAccessToken(mock(Credentials.class), true));
		assertNotNull(token);
		assertEquals(MockTokenServer.VALID_TOKEN, token);
	}

}
