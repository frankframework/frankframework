package org.frankframework.http.authentication;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import org.frankframework.http.AbstractHttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

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
		var authenticator = AbstractHttpSession.AuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);

		String accessToken = authenticator.getOrRefreshAccessToken(new UsernamePasswordCredentials(clientId, clientSecret), true);

		assertThat(accessToken, startsWith("Bearer"));
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

		Credentials credentials = new UsernamePasswordCredentials(username, password);
		var authenticator = AbstractHttpSession.AuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);

		String accessToken = authenticator.getOrRefreshAccessToken(credentials, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenWithAuthHeader() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		var authenticator = AbstractHttpSession.AuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);

		String accessToken = authenticator.getOrRefreshAccessToken(null, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenNoScope() throws Exception {
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;

		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope(null);

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);
		var authenticator = AbstractHttpSession.AuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);

		String accessToken = authenticator.getOrRefreshAccessToken(credentials, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenWrongTokenEndpoint() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.PATH + "/xxxxx");
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		var authenticator = AbstractHttpSession.AuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);

		assertThrows(HttpAuthenticationException.class, () -> authenticator.getOrRefreshAccessToken(null, true));
	}

	@Test
	public void testRetrieveAccessTokenFirstExpiredForced() throws Exception {
		httpSender.setTokenEndpoint(getEndpoint() + MockTokenServer.EXPIRED_PATH);
		httpSender.setTokenExpiry(-1);
		httpSender.setScope("email");
		httpSender.setClientId(MockTokenServer.CLIENT_ID);
		httpSender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		var authenticator = AbstractHttpSession.AuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);

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

		var authenticator = AbstractHttpSession.AuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);

		tokenServer.resetScenarios();
		assertThat(authenticator.getOrRefreshAccessToken(null, true), containsString("Expired"));

		Thread.sleep(100);
		assertThat(authenticator.getOrRefreshAccessToken(null, true), not(containsString("Expired")));
	}

}
