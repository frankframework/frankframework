package nl.nn.adapterframework.http;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Test;

import nl.nn.adapterframework.http.authentication.OAuthAccessTokenManager;

public class OAuthAccessTokenManagerTest {

	String TOKENENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/token";
	String USERINFOENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/userinfo";
	String INTROSPECTIONENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/userinfo";
	
	String clientClientId = "testiaf-client";
	String clientClientSecret = "testiaf-client-pwd";

	String serviceClientId = "testiaf-service";
	String serviceClientSecret = "testiaf-service-pwd";

	@Test
	public void testRetrieveAccessToken() throws Exception {
		String tokenEndpoint = TOKENENDPOINT;
		String scope = "email";
		String clientId = clientClientId;
		String clientSecret= clientClientSecret;

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenEndpoint, scope);
				
		String accessToken = accessTokenManager.getAccessToken(credentials);
		
		assertThat(accessToken,startsWith("Bearer"));
	}
	
	@Test
	public void testRetrieveAccessTokenNoScope() throws Exception {
		String tokenEndpoint = TOKENENDPOINT;
		String scope = null;
		String clientId = clientClientId;
		String clientSecret= clientClientSecret;

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenEndpoint, scope);
				
		String accessToken = accessTokenManager.getAccessToken(credentials);
		
		assertThat(accessToken,startsWith("Bearer"));
	}
	
	@Test
	public void testRetrieveAccessTokenWrongCredentials() throws Exception {
		String tokenEndpoint = TOKENENDPOINT;
		String scope = "email";
		String clientId = clientClientId;
		String clientSecret="xxx";

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenEndpoint, scope);

		assertThrows(AuthenticationException.class, ()->accessTokenManager.getAccessToken(credentials));
	}
	
	@Test
	public void testRetrieveAccessTokenWrongTokenEndpoint() throws Exception {
		String tokenEndpoint = TOKENENDPOINT+"x";
		String scope = "email";
		String clientId = clientClientId;
		String clientSecret=clientClientSecret;

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenEndpoint, scope);

		assertThrows(AuthenticationException.class, ()->accessTokenManager.getAccessToken(credentials));
	}

}
