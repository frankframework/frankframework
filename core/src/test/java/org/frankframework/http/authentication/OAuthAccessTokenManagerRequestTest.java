package org.frankframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Base64;

import org.apache.http.Header;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;

import org.frankframework.http.authentication.OAuthAccessTokenManager.AuthenticationType;
import org.frankframework.util.StreamUtil;

/**
 * Tests whether tokenRequests are generated as expected according to the given input
 */
public class OAuthAccessTokenManagerRequestTest {

	private static final String CLIENT_ID = "fakeClientId";

	private static final String CLIENT_SECRET = "fakeClientSecret";

	private static final String TOKEN_ENDPOINT = "http://fakeTokenEndpoint";

	private static final String BASE_64 = "Basic " + Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes());

	private final Credentials credentials = new UsernamePasswordCredentials("fakeCredentialUserName", "fakeCredentialPassword");

	private TestableOAuthAccessTokenManager tokenManager;

	@Test
	void testRetrieveAccessTokenWithClientCredentialsGrantInRequestParameters() throws Exception {
		tokenManager = getTokenManager(true, AuthenticationType.REQUEST_PARAMETER);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("grant_type=client_credentials&scope=email&client_secret=fakeClientSecret&client_id=fakeClientId", httpRequest.getQuery());
		assertEquals("{Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap()
				.toString());

		HttpPost apacheRequest = (HttpPost) tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=client_credentials&scope=email&client_secret=fakeClientSecret&client_id=fakeClientId", StreamUtil.streamToString(apacheRequest.getEntity()
				.getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 95,Chunked: false]", apacheRequest.getEntity()
				.toString());
	}

	@Test
	void testRetrieveAccessTokenWithClientCredentialsGrantInBasicAuthentication() throws Exception {
		tokenManager = getTokenManager(true, AuthenticationType.AUTHENTICATION_HEADER);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("grant_type=client_credentials&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=[" + BASE_64 + "], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap()
				.toString());

		HttpPost apacheRequest = (HttpPost) tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization", BASE_64);
		assertHeaderPresent(apacheRequest, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=client_credentials&scope=email", StreamUtil.streamToString(apacheRequest.getEntity().getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 41,Chunked: false]", apacheRequest.getEntity()
				.toString());
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantUsingRequestParameters() throws Exception {
		tokenManager = getTokenManager(false, AuthenticationType.REQUEST_PARAMETER);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("password=fakeCredentialPassword&grant_type=password&scope=email&client_secret=fakeClientSecret&client_id=fakeClientId&username=fakeCredentialUserName", httpRequest.getQuery());
		assertEquals("{Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap()
				.toString());

		HttpPost apacheRequest = (HttpPost) tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("password=fakeCredentialPassword&grant_type=password&scope=email&client_secret=fakeClientSecret&client_id=fakeClientId&username=fakeCredentialUserName", StreamUtil.streamToString(apacheRequest.getEntity()
				.getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 149,Chunked: false]", apacheRequest.getEntity()
				.toString());
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantUsingBasicAuthentication() throws Exception {
		tokenManager = getTokenManager(false, AuthenticationType.AUTHENTICATION_HEADER);

		TokenRequest tokenRequest = tokenManager.createRequest(credentials);

		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
		assertEquals("POST", httpRequest.getMethod().name());
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email", httpRequest.getQuery());
		assertEquals("{Authorization=[" + BASE_64 + "], Content-Type=[application/x-www-form-urlencoded; charset=UTF-8]}", httpRequest.getHeaderMap()
				.toString());

		HttpPost apacheRequest = (HttpPost) tokenManager.convertToApacheHttpRequest(httpRequest);
		assertEquals("POST", apacheRequest.getMethod());
		assertHeaderPresent(apacheRequest, "Authorization", BASE_64);
		assertHeaderPresent(apacheRequest, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("password=fakeCredentialPassword&grant_type=password&username=fakeCredentialUserName&scope=email", StreamUtil.streamToString(apacheRequest.getEntity()
				.getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 95,Chunked: false]", apacheRequest.getEntity()
				.toString());
	}

	public void assertHeaderPresent(HttpRequestBase method, String header, String expectedValue) {
		Header[] headers = method.getHeaders(header);
		assertNotNull(headers);
		assertEquals(1, headers.length);
		assertEquals(expectedValue, headers[0].getValue());
	}

	private TestableOAuthAccessTokenManager getTokenManager(boolean useClientCredentials, AuthenticationType authenticationType) throws Exception {
		return new TestableOAuthAccessTokenManager(useClientCredentials, authenticationType, CLIENT_ID, CLIENT_SECRET, TOKEN_ENDPOINT);
	}
}
