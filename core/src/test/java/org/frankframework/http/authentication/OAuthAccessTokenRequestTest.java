package org.frankframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Base64;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;

import org.frankframework.http.AbstractHttpSession;

import org.frankframework.http.HttpSender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.util.StreamUtil;

/**
 * Tests whether tokenRequests are generated as expected according to the given input
 */
public class OAuthAccessTokenRequestTest {

	private static final String CLIENT_ID = "fakeClientId";

	private static final String CLIENT_SECRET = "fakeClientSecret";

	private static final String TOKEN_ENDPOINT = "http://fakeTokenEndpoint";

	private static final String BASE_64 = "Basic " + Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes());

	private HttpSender httpSender;

	@BeforeEach
	public void setup() throws Exception {
		httpSender = new HttpSender();
		httpSender.setUrl("https://dummy");
		httpSender.setTokenEndpoint("https://token-dummy");
	}

	@Test
	void testRetrieveAccessTokenWithClientCredentialsGrantInRequestParameters() throws Exception {
		httpSender.setScope("email");
		httpSender.setClientId(CLIENT_ID);
		httpSender.setClientSecret(CLIENT_SECRET);

		httpSender.configure();
		httpSender.start();

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials(), new ArrayList<>());

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("client_secret=fakeClientSecret&client_id=fakeClientId&grant_type=client_credentials&scope=email", StreamUtil.streamToString(request.getEntity()
				.getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 95,Chunked: false]", request.getEntity()
				.toString());
	}

	@Test
	void testRetrieveAccessTokenWithClientCredentialsGrantInBasicAuthentication() throws Exception {
		httpSender.setScope("username,email");
		httpSender.setClientId(CLIENT_ID);
		httpSender.setClientSecret(CLIENT_SECRET);

		httpSender.configure();
		httpSender.start();

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials(), new ArrayList<>());

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Authorization", BASE_64);
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=client_credentials&scope=username+email", StreamUtil.streamToString(request.getEntity().getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 50,Chunked: false]", request.getEntity()
				.toString());
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantUsingRequestParameters() throws Exception {
		httpSender.setScope("email");
		httpSender.setClientId(CLIENT_ID);
		httpSender.setClientSecret(CLIENT_SECRET);
		httpSender.setUsername("fakeCredentialUserName");
		httpSender.setPassword("fakeCredentialPassword");

		httpSender.configure();
		httpSender.start();

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.OauthAuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials(), new ArrayList<>());

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("client_id=fakeClientId&client_secret=fakeClientSecret&grant_type=password&scope=email&username=fakeCredentialUserName&password=fakeCredentialPassword", StreamUtil.streamToString(request.getEntity()
				.getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 149,Chunked: false]", request.getEntity()
				.toString());
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantUsingBasicAuthentication() throws Exception {
		httpSender.setScope("email");
		httpSender.setClientId(CLIENT_ID);
		httpSender.setClientSecret(CLIENT_SECRET);
		httpSender.setUsername("fakeCredentialUserName");
		httpSender.setPassword("fakeCredentialPassword");

		httpSender.configure();
		httpSender.start();

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.OauthAuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials(), new ArrayList<>());

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Authorization", BASE_64);
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=password&scope=email&username=fakeCredentialUserName&password=fakeCredentialPassword", StreamUtil.streamToString(request.getEntity()
				.getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 95,Chunked: false]", request.getEntity()
				.toString());
	}

	public void assertHeaderPresent(HttpRequestBase method, String header, String expectedValue) {
		Header[] headers = method.getHeaders(header);
		assertNotNull(headers);
		assertEquals(1, headers.length, "Header must exist");
		assertEquals(expectedValue, headers[0].getValue());
	}
}
