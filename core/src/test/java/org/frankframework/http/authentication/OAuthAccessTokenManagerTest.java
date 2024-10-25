package org.frankframework.http.authentication;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.oauth2.sdk.Scope;

import org.frankframework.http.HttpSender;
import org.frankframework.http.authentication.OAuthAccessTokenManager.AuthenticationType;
import org.frankframework.util.CredentialFactory;

public class OAuthAccessTokenManagerTest {

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
		String scope = "email";
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(getEndpoint()+MockTokenServer.PATH, scope, client_cf, true, AuthenticationType.REQUEST_PARAMETER, httpSender, -1);

		String accessToken = accessTokenManager.getAccessToken(null, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenWithResourceOwnerPasswordGrant() throws Exception {
		String scope = "email";
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;
		String username = "fakeUsername";
		String password = "fakePassword";

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);
		Credentials credentials = new UsernamePasswordCredentials(username, password);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(getEndpoint()+MockTokenServer.PATH, scope, client_cf, false, AuthenticationType.REQUEST_PARAMETER, httpSender, -1);

		String accessToken = accessTokenManager.getAccessToken(credentials, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenWithAuthHeader() throws Exception {
		String scope = "email";
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;
		String username = "fakeUsername";
		String password = "fakePassword";

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);
		Credentials credentials = new UsernamePasswordCredentials(username, password);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(getEndpoint()+MockTokenServer.PATH, scope, client_cf, false, AuthenticationType.AUTHENTICATION_HEADER, httpSender, -1);

		String accessToken = accessTokenManager.getAccessToken(credentials, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenNoScope() throws Exception {
		String scope = null;
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(getEndpoint()+MockTokenServer.PATH, scope, client_cf, true, AuthenticationType.REQUEST_PARAMETER, httpSender, -1);

		String accessToken = accessTokenManager.getAccessToken(null, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenWrongTokenEndpoint() throws Exception {
		String scope = "email";
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(getEndpoint() + "/xxxxx", scope, client_cf, true, AuthenticationType.REQUEST_PARAMETER, httpSender, -1);

		HttpAuthenticationException actualException = assertThrows(HttpAuthenticationException.class, () -> accessTokenManager.getAccessToken(null, true));
		assertThat(actualException.getMessage(), containsString("Could not retrieve token"));
	}

	@Test
	public void testRetrieveAccessTokenFirstExpiredForced() throws Exception {
		String scope = "email";
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(getEndpoint() + MockTokenServer.EXPIRED_PATH, scope, client_cf, true, AuthenticationType.REQUEST_PARAMETER, httpSender, -1);

		tokenServer.resetScenarios();
		assertThat(accessTokenManager.getAccessToken(null, true), containsString("Expired"));

		accessTokenManager.retrieveAccessToken(null);
		assertThat(accessTokenManager.getAccessToken(null, true), not(containsString("Expired")));
	}

	@Test
	public void testRetrieveAccessTokenFirstExpiredAutomatic() throws Exception {
		String scope = "email";
		String clientId = MockTokenServer.CLIENT_ID;
		String clientSecret = MockTokenServer.CLIENT_SECRET;

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(getEndpoint() + MockTokenServer.EXPIRED_PATH, scope, client_cf, true, AuthenticationType.REQUEST_PARAMETER, httpSender, -1);

		tokenServer.resetScenarios();
		assertThat(accessTokenManager.getAccessToken(null, true), containsString("Expired"));

		Thread.sleep(100);
		assertThat(accessTokenManager.getAccessToken(null, true), not(containsString("Expired")));
	}

	@Test
	public void scopeTest() {
		Scope scope1 = new Scope("read", "write");
		Scope scope2 = Scope.parse("read write");
		Scope scope3 = Scope.parse("read, write");
		assertEquals("read write", scope1.toString());
		assertEquals("read write", scope2.toString());
		assertEquals("read write", scope3.toString());
	}

}
