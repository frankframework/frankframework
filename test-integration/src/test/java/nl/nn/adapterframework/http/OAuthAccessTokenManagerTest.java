package nl.nn.adapterframework.http;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.http.authentication.HttpAuthenticationException;
import nl.nn.adapterframework.http.authentication.OAuthAccessTokenManager;
import nl.nn.adapterframework.util.CredentialFactory;

public class OAuthAccessTokenManagerTest {

	String TOKENENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/token";
	String USERINFOENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/userinfo";
	String INTROSPECTIONENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/userinfo";

	String clientClientId = "testiaf-client";
	String clientClientSecret = "testiaf-client-pwd";

	String serviceClientId = "testiaf-service";
	String serviceClientSecret = "testiaf-service-pwd";

	private HttpSender httpSender = new HttpSender();
	private int expiry = 60;

	@Before
	public void setup() {
		httpSender = new HttpSender();
		httpSender.setUrl("https://dummy");
		httpSender.configure();
		httpSender.open();
	}

	@Test
	public void testRetrieveAccessToken() {
		String tokenEndpoint = TOKENENDPOINT;
		String scope = "email";
		String clientId = clientClientId;
		String clientSecret= clientClientSecret;

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenEndpoint, scope, client_cf, true, false, httpSender, expiry);

		String accessToken = accessTokenManager.getAccessToken(null, true);

		assertThat(accessToken,startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenNoScope() {
		String tokenEndpoint = TOKENENDPOINT;
		String scope = null;
		String clientId = clientClientId;
		String clientSecret= clientClientSecret;

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenEndpoint, scope, client_cf, true, false, httpSender, expiry);

		String accessToken = accessTokenManager.getAccessToken(null, true);

		assertThat(accessToken,startsWith("Bearer"));
	}

	@Test
	public void testRetrieveAccessTokenWrongCredentials() {
		String tokenEndpoint = TOKENENDPOINT;
		String scope = "email";
		String clientId = clientClientId;
		String clientSecret="xxx";

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenEndpoint, scope, client_cf, true, false, httpSender, expiry);

		HttpAuthenticationException exception = assertThrows(HttpAuthenticationException.class, ()->accessTokenManager.getAccessToken(null, true));
		assertThat(exception.getMessage(), containsString("unauthorized_client"));
	}

	@Test
	public void testRetrieveAccessTokenWrongTokenEndpoint() {
		String tokenEndpoint = TOKENENDPOINT+"x";
		String scope = "email";
		String clientId = clientClientId;
		String clientSecret=clientClientSecret;

		CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenEndpoint, scope, client_cf, true, false, httpSender, expiry);

		HttpAuthenticationException exception = assertThrows(HttpAuthenticationException.class, ()->accessTokenManager.getAccessToken(null, true));
		assertThat(exception.getMessage(), containsString("404"));
	}

}
