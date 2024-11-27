package org.frankframework.http;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.http.authentication.HttpAuthenticationException;
import org.frankframework.http.authentication.OAuthAccessTokenManager;
import org.frankframework.util.CredentialFactory;

public class OAuthAccessTokenManagerTest {

	String TOKENENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/token";
	String USERINFOENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/userinfo";
	String INTROSPECTIONENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/userinfo";

	String clientClientId = "testiaf-client";
	String clientClientSecret = "testiaf-client-pwd";

	String serviceClientId = "testiaf-service";
	String serviceClientSecret = "testiaf-service-pwd";

	private HttpSender httpSender = new HttpSender();
	private final int expiry = 60;

	@BeforeEach
	public void setup() throws ConfigurationException, SenderException {
		httpSender = new HttpSender();
		httpSender.setUrl("https://dummy");
		httpSender.configure();
		httpSender.open();
	}

	@Test
	public void testRetrieveAccessToken() throws HttpAuthenticationException {
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
	public void testRetrieveAccessTokenNoScope() throws HttpAuthenticationException {
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
	public void testRetrieveAccessTokenWrongCredentials() throws HttpAuthenticationException {
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
	public void testRetrieveAccessTokenWrongTokenEndpoint() throws HttpAuthenticationException {
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
