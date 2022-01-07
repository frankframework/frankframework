package nl.nn.adapterframework.http.authentication;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Rule;
import org.junit.Test;

public class OAuthAccessTokenManagerTest {

	@Rule
	public MockTokenServer tokenServer = new MockTokenServer();
		
	@Test
	public void testRetrieveAccessToken() throws Exception {
		String scope = "email";
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getPath(), scope);
				
		String accessToken = accessTokenManager.getAccessToken(credentials);
		
		assertThat(accessToken,startsWith("Bearer"));
	}
	
	@Test
	public void testRetrieveAccessTokenNoScope() throws Exception {
		String scope = null;
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getPath(), scope);
				
		String accessToken = accessTokenManager.getAccessToken(credentials);
		
		assertThat(accessToken,startsWith("Bearer"));
	}
	
	@Test
	public void testRetrieveAccessTokenWrongTokenEndpoint() throws Exception {
		String scope = "email";
		String clientId = tokenServer.getClientId();
		String clientSecret = tokenServer.getClientSecret();

		Credentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);

		OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(tokenServer.getServer()+"/xxxx", scope);

		assertThrows(AuthenticationException.class, ()->accessTokenManager.getAccessToken(credentials));
	}

}
