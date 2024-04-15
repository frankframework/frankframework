package org.frankframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Base64;

import org.apache.http.Header;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.frankframework.http.authentication.OAuthAccessTokenManager.AuthenticationType;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StreamUtil;
import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;

public class OAuthAccessTokenManagerRequestTest {

	private final String scope = "email";
	private final String clientId = "fakeClientId";
	private final String clientSecret = "fakeClientSecret";
	private final String tokenEndpoint = "http://fakeTokenEndpoint";
	private final String base64 = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

	private final Credentials credentials = new UsernamePasswordCredentials("fakeCredentialUserName", "fakeCredentialPassword");
	private final CredentialFactory client_cf = new CredentialFactory(null, clientId, clientSecret);

	private TestableOAuthAccessTokenManager tokenManager;


	@Test
	void testRetrieveAccessTokenWithClientCredentialsGrantInRequestParameters() throws Exception {
		tokenManager = new TestableOAuthAccessTokenManager(true, AuthenticationType.REQUEST_PARAMETER);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("grant_type=client_credentials&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=["+base64+"], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap().toString());

		HttpPost apacheRequest = (HttpPost)tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization",base64);
		assertHeaderPresent(apacheRequest, "Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=client_credentials&scope=email&client_id=fakeClientId&client_secret=fakeClientSecret", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 95,Chunked: false]", apacheRequest.getEntity().toString());
	}

	@Test
	void testRetrieveAccessTokenWithClientCredentialsGrantInBasicAuthentication() throws Exception {
		tokenManager = new TestableOAuthAccessTokenManager(true, AuthenticationType.AUTHENTICATION_HEADER);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("grant_type=client_credentials&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=["+base64+"], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap().toString());

		HttpPost apacheRequest = (HttpPost)tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization",base64);
		assertHeaderPresent(apacheRequest, "Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=client_credentials&scope=email", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 41,Chunked: false]", apacheRequest.getEntity().toString());
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantUsingRequestParameters() throws Exception {
		tokenManager = new TestableOAuthAccessTokenManager(false, AuthenticationType.REQUEST_PARAMETER);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=["+base64+"], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap().toString());

		HttpPost apacheRequest = (HttpPost)tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization",base64);
		assertHeaderPresent(apacheRequest, "Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email&client_id=fakeClientId&client_secret=fakeClientSecret", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 149,Chunked: false]", apacheRequest.getEntity().toString());
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantUsingBasicAuthentication() throws Exception {
		tokenManager = new TestableOAuthAccessTokenManager(false, AuthenticationType.AUTHENTICATION_HEADER);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=["+base64+"], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap().toString());

		HttpPost apacheRequest = (HttpPost)tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization",base64);
		assertHeaderPresent(apacheRequest, "Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 95,Chunked: false]", apacheRequest.getEntity().toString());
	}

	public void assertHeaderPresent(HttpRequestBase method, String header, String expectedValue) {
		Header[] headers = method.getHeaders(header);
		assertNotNull(headers);
		assertEquals(1, headers.length);
		assertEquals(expectedValue, headers[0].getValue());
	}

	private class TestableOAuthAccessTokenManager extends OAuthAccessTokenManager {

		public TestableOAuthAccessTokenManager(boolean useClientCredentials, AuthenticationType type) throws HttpAuthenticationException {
			super(tokenEndpoint, scope, client_cf, useClientCredentials, type, null, -1);
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
