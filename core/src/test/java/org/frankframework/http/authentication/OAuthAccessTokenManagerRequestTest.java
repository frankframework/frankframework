package org.frankframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;

import org.frankframework.http.AbstractHttpSession;

import org.frankframework.http.HttpSender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.util.StreamUtil;

/**
 * Tests whether tokenRequests are generated as expected according to the given input
 */
public class OAuthAccessTokenManagerRequestTest {

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

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.AuthenticationMethod.CLIENT_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials());

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=client_credentials&scope=email&client_secret=fakeClientSecret&client_id=fakeClientId", StreamUtil.streamToString(request.getEntity()
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

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.AuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials());

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

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.AuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_QUERY_PARAMETERS.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials());

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=password&scope=email&username=fakeCredentialUserName&password=fakeCredentialPassword&client_id=fakeClientId&client_secret=fakeClientSecret", StreamUtil.streamToString(request.getEntity()
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

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.AuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_BASIC_AUTH.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials());

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Authorization", BASE_64);
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=password&scope=email&username=fakeCredentialUserName&password=fakeCredentialPassword", StreamUtil.streamToString(request.getEntity()
				.getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 95,Chunked: false]", request.getEntity()
				.toString());
	}

	@Test
	@Tag("testtesttest")
	void testSamlAssertion() throws Exception {
		httpSender.setTokenEndpoint("fakeEndpoint");
		httpSender.setAuthenticationMethod(AbstractHttpSession.AuthenticationMethod.SAML_ASSERTION);

		httpSender.setPrivateKey("-----BEGIN PRIVATE KEY-----\n" +
				"MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGAa5meBF/8w4RETkBj\n" +
				"JgQeexBRPsR4JrrKYUjptS020Kyld9MSz1UOCuyPiFsjArpJPWs93mbSdCR89TwP\n" +
				"9knZmJFQZk3wM5mkcEA+WJ6xjDH8/7zIJIQqzCGGT8UfMBeFqIhufQC4jYwTBRDW\n" +
				"b2GHaLqrq+3nnHEmg1i6L8WRLXUCAwEAAQKBgCqcNfBjlrRSj74xT1JBtVRkvNfP\n" +
				"dAlaVUS7XBmsYxW2GPzfsIY8l4gJ8Dk+ZhnxbYmOC30kWNk3jeiLtYKB8lIrvoBV\n" +
				"fNXe4IJB39t7U8JbdUsQSa2nzVoUFjjeaI3LiJ6z1l/hzyhl70KSrXi5ycGVJrh5\n" +
				"s9v3EROO99R/y1lNAkEAypss5gtI4dr6k+ElweeArftqL0MIgqYngamW3HGheglh\n" +
				"qZlqU1uL3Fga5xwBlecq5FxiowNABOTES5YvzD5AxwJBAIf02yVM/4OXxgB2mE1G\n" +
				"lzyiQ2tnKbPHrgzToM4SFDZN2EBOtWG+4AABsZLtXM6cJ3n1yMAze7tdlg5hMQLx\n" +
				"W+MCQQCBkrYnNUZaM0qX8qDMHrscCbNCIJO7wnl3ojb6Kq3Dt2Y/Kf9m6iBLPgmO\n" +
				"jkmxTdMPksn+SODTgF7NnHJbI+EXAkAYcZ6hEznxZ+1SkgAKDMIORcJHYjHuP918\n" +
				"MuR7iGaX6OETltMnstDFT4ikuQZxo0O5usYQQHFjm4zqIvFT7R8vAkA2ntBhvJbQ\n" +
				"AlNvCCvBjlTbsdp7A2LnIwddBZvi0ekQKMW9709HTkvcysbBTYHGV6qG6IOj3/AI\n" +
				"36qWDXImqowl\n" +
				"-----END PRIVATE KEY-----");

		httpSender.setCertificate2("-----BEGIN CERTIFICATE-----\n" +
				"MIIDJTCCAg2gAwIBAgIUFVmKTIu/07BPj3LRAsqiZut8lfswDQYJKoZIhvcNAQEL\n" +
				"BQAwIjELMAkGA1UEBhMCTkwxEzARBgNVBAoMCldlQXJlRnJhbmswHhcNMjQxMTI3\n" +
				"MTIzMDA2WhcNMjUxMTI3MTIzMDA2WjAiMQswCQYDVQQGEwJOTDETMBEGA1UECgwK\n" +
				"V2VBcmVGcmFuazCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMlPvUF3\n" +
				"2PyY5zJA50W2o9scE244L/pQCBInrADajhfWdIsCSMqhEyzGWl3nVhjkqZA0bVha\n" +
				"JQulfJH25XUQYHonoeffdqqh1gQEu03IKClKqRDLZ9lbT7jFbYEX/1hFlzu4FqRp\n" +
				"pWLxdeGvMNvwX95VQSsRiHQ9GvuiuyUc/7+0V6OZBLfp+zWDuOrZ/yGmLlFeWwQb\n" +
				"wyEIKKknBediwImW3o+iFuYs2FPsCqsJ3qfZol6ig+MeTVkFjNIIKONG+/FB56cz\n" +
				"RWGYC+7g8OLjBWKaChPiKchs8g+v7NkBGCOJgyeBTA4f4pl+ttAXgGN0HwMhcuT2\n" +
				"0eB//QxYx+AU5t0CAwEAAaNTMFEwHQYDVR0OBBYEFMP1Lz7dXT8lyus6hEUAcVFG\n" +
				"sA0SMB8GA1UdIwQYMBaAFMP1Lz7dXT8lyus6hEUAcVFGsA0SMA8GA1UdEwEB/wQF\n" +
				"MAMBAf8wDQYJKoZIhvcNAQELBQADggEBAGnVIsNlC2gBhJgrbRwmZnUgv7T7rxrc\n" +
				"1y4n3sfABem5AWuGsx57/vKOtGlc8NYE10lPB0U7JTX5cwumSCgoNhOX0jAzSrXl\n" +
				"Qb4i62POqkT6jxnjfaLJHlYUWdFDjffmzO/tH0gW6ETpi8c1VHKuulsoLLpXBkae\n" +
				"HqOmr2g24wtU2NH+SNWvyKUk/Xyjr2Gjr2uAL45m80BjE8zYdQBoi9bI9e7BaO6F\n" +
				"hOTVSThh5J3R23GaM4n6Wi2V2PQ6ynZ0qwNIpd4ak+cdJOr6tseVYu3mCpDaKSjz\n" +
				"2UqUGdhJIbcKhIc0TaLaRpXlUUUGq8lq9/7GBNEl7t1E25Jm0I2hSTI=\n" +
				"-----END CERTIFICATE-----\n");

		httpSender.setClientId(CLIENT_ID);
		httpSender.setClientSecret(CLIENT_SECRET);

		httpSender.setIssuer("www.successfactors.com");
		httpSender.setAudience("www.successfactors.com");

		httpSender.configure();
		httpSender.start();

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.AuthenticationMethod.SAML_ASSERTION.newAuthenticator(httpSender);
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getCredentials());

		String body = StreamUtil.streamToString(request.getEntity().getContent(), "\n", "UTF-8");
		int bodyLength = body.length();

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

		assertTrue(body.contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Asaml2-bearer&client_id=fakeClientId&client_secret=fakeClientSecret&assertion="));

		assertEquals(String.format("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: %s,Chunked: false]", bodyLength), request.getEntity()
				.toString());

		Pattern pattern = Pattern.compile("(.*?)assertion=(.*?)($|&(.*?))");
		Matcher matcher = pattern.matcher(body);

		assertTrue(matcher.find());
		String urlEncodedAssertion = matcher.group(2);
		assertNotNull(urlEncodedAssertion);
		String base64EncodedAssertion = URLDecoder.decode(urlEncodedAssertion, StandardCharsets.UTF_8);
		assertNotNull(base64EncodedAssertion);
	}

	public void assertHeaderPresent(HttpRequestBase method, String header, String expectedValue) {
		Header[] headers = method.getHeaders(header);
		assertNotNull(headers);
		assertEquals(1, headers.length, "Header must exist");
		assertEquals(expectedValue, headers[0].getValue());
	}
}
