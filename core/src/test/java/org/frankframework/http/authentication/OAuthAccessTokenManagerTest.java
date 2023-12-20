package org.frankframework.http.authentication;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.frankframework.http.HttpSender;
import org.frankframework.util.CredentialFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.nimbusds.oauth2.sdk.Scope;

public class OAuthAccessTokenManagerTest {

	@Rule
	public MockTokenServer tokenServer = new MockTokenServer();

	private HttpSender httpSender = new HttpSender();

	@Before
	public void setup() throws Exception {
		httpSender = new HttpSender();
		httpSender.setUrl("https://dummy");
		httpSender.configure();
		httpSender.open();
	}

	@Test
	public void testAttributes() {
		assertEquals(tokenServer.getServer() + tokenServer.getPath(), tokenServer.getEndpoint());
	}

	@Test
	public void testRetrieveAccessTokenWithClientCredentialsGrant() throws Exception {
		String scope = "email";
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getEndpoint(), scope, client_cf, true, false, httpSender, -1);

		String accessToken = accessTokenManager.getAccessToken(null, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenWithResourceOwnerPasswordGrant() throws Exception {
		String scope = "email";
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();
		String username = "fakeUsername";
		String password = "fakePassword";

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);
		Credentials credentials = new UsernamePasswordCredentials(username, password);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getEndpoint(), scope, client_cf, false, false, httpSender, -1);

		String accessToken = accessTokenManager.getAccessToken(credentials, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenNoScope() throws Exception {
		String scope = null;
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getEndpoint(), scope, client_cf, true, false, httpSender, -1);

		String accessToken = accessTokenManager.getAccessToken(null, true);

		assertThat(accessToken, startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenWrongTokenEndpoint() throws Exception {
		String scope = "email";
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getEndpoint() + "/xxxxx", scope, client_cf, true, false, httpSender, -1);

		HttpAuthenticationException actualException = assertThrows(HttpAuthenticationException.class, () -> accessTokenManager.getAccessToken(null, true));
		assertThat(actualException.getMessage(), containsString("Could not retrieve token"));
	}

	@Test
	public void testRetrieveAccessTokenFirstExpiredForced() throws Exception {
		String scope = "email";
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getEndpointFirstExpired(), scope, client_cf, true, false, httpSender, -1);

		tokenServer.resetScenarios();
		assertThat(accessTokenManager.getAccessToken(null, true), containsString("Expired"));

		accessTokenManager.retrieveAccessToken(null);
		assertThat(accessTokenManager.getAccessToken(null, true), not(containsString("Expired")));
	}

	@Test
	public void testRetrieveAccessTokenFirstExpiredAutomatic() throws Exception {
		String scope = "email";
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getEndpointFirstExpired(), scope, client_cf, true, false, httpSender, -1);

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
