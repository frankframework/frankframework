package nl.nn.adapterframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.http.Header;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.Test;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;

import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.StreamUtil;

public class OAuthAccessTokenManagerRequestTest {

	String scope = "email";
	String clientId = "fakeClientId";
	String clientSecret = "fakeClientSecret";
	String tokenEndpoint = "http://fakeTokenEndpoint";

	CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

	private TestableOAuthAccessTokenManager tokenManager;


	@Test
	void testRetrieveAccessTokenWithClientCredentialsGrantInRequestParameters() throws Exception {
		Credentials credentials = new UsernamePasswordCredentials("fakeCredentialUserName", "fakeCredentialPassword");

		tokenManager = new TestableOAuthAccessTokenManager(true, false);

		TokenRequest tokenRequest =  tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("grant_type=client_credentials&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=[Basic ZmFrZUNsaWVudElkOmZha2VDbGllbnRTZWNyZXQ=], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap().toString());

		HttpPost apacheRequest = (HttpPost)tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization","Basic ZmFrZUNsaWVudElkOmZha2VDbGllbnRTZWNyZXQ=");
		assertEquals("[Content-Type: text/plain; charset=ISO-8859-1,Content-Length: 95,Chunked: false]", apacheRequest.getEntity().toString());
		assertEquals("grant_type=client_credentials&scope=email&client_id=fakeClientId&client_secret=fakeClientSecret", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
	}

	@Test
	void testRetrieveAccessTokenWithClientCredentialsGrantInBasicAuthentication() throws Exception {
		Credentials credentials = new UsernamePasswordCredentials("fakeCredentialUserName", "fakeCredentialPassword");

		tokenManager = new TestableOAuthAccessTokenManager(true, true);

		TokenRequest tokenRequest =  tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("grant_type=client_credentials&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=[Basic ZmFrZUNsaWVudElkOmZha2VDbGllbnRTZWNyZXQ=], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap().toString());

		HttpPost apacheRequest = (HttpPost)tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization","Basic ZmFrZUNsaWVudElkOmZha2VDbGllbnRTZWNyZXQ=");
		assertEquals("[Content-Type: text/plain; charset=ISO-8859-1,Content-Length: 41,Chunked: false]", apacheRequest.getEntity().toString());
		assertEquals("grant_type=client_credentials&scope=email", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantUsingRequestParameters() throws Exception {
		Credentials credentials = new UsernamePasswordCredentials("fakeCredentialUserName", "fakeCredentialPassword");

		tokenManager = new TestableOAuthAccessTokenManager(false, false);

		TokenRequest tokenRequest =  tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=[Basic ZmFrZUNsaWVudElkOmZha2VDbGllbnRTZWNyZXQ=], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap().toString());

		HttpPost apacheRequest = (HttpPost)tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization","Basic ZmFrZUNsaWVudElkOmZha2VDbGllbnRTZWNyZXQ=");
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email&client_id=fakeClientId&client_secret=fakeClientSecret", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: text/plain; charset=ISO-8859-1,Content-Length: 149,Chunked: false]", apacheRequest.getEntity().toString());
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantUsingBasicAuthentication() throws Exception {
		Credentials credentials = new UsernamePasswordCredentials("fakeCredentialUserName", "fakeCredentialPassword");

		tokenManager = new TestableOAuthAccessTokenManager(false, true);

		TokenRequest tokenRequest =  tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=[Basic ZmFrZUNsaWVudElkOmZha2VDbGllbnRTZWNyZXQ=], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap().toString());

		HttpPost apacheRequest = (HttpPost)tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization","Basic ZmFrZUNsaWVudElkOmZha2VDbGllbnRTZWNyZXQ=");
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: text/plain; charset=ISO-8859-1,Content-Length: 95,Chunked: false]", apacheRequest.getEntity().toString());
	}

	public void assertHeaderPresent(HttpRequestBase method, String header, String expectedValue) {
		Header[] headers = method.getHeaders(header);
		assertNotNull(headers);
		assertEquals(1, headers.length);
		assertEquals(expectedValue, headers[0].getValue());
	}

	private class TestableOAuthAccessTokenManager extends OAuthAccessTokenManager {

		public TestableOAuthAccessTokenManager(boolean useClientCredentialsGrant, boolean authenticatedTokenRequest) throws HttpAuthenticationException {
			super(tokenEndpoint, scope, client_cf, useClientCredentialsGrant, authenticatedTokenRequest, null, -1);
		}

		@Override
		public TokenRequest createRequest(Credentials credentials) {
			return super.createRequest(credentials);
		}

		@Override
		public HttpRequestBase convertToApacheHttpRequest(HTTPRequest httpRequest) throws HttpAuthenticationException {
			return super.convertToApacheHttpRequest(httpRequest);
		}

	}

}
