package org.frankframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.http.AbstractHttpSession;
import org.frankframework.http.HttpSender;
import org.frankframework.util.StreamUtil;

/**
 * Tests whether tokenRequests are generated as expected according to the given input
 */
public class OAuthAccessTokenRequestTest {

	private static final String CLIENT_ID = "fakeClientId";

	private static final String CLIENT_SECRET = "fakeClientSecret";

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
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getDomainAwareCredentials(), new ArrayList<>());

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
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getDomainAwareCredentials(), new ArrayList<>());

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
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getDomainAwareCredentials(), new ArrayList<>());

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
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getDomainAwareCredentials(), new ArrayList<>());

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Authorization", BASE_64);
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		assertEquals("grant_type=password&scope=email&username=fakeCredentialUserName&password=fakeCredentialPassword", StreamUtil.streamToString(request.getEntity()
				.getContent(), "\n", "UTF-8"));
		assertEquals("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: 95,Chunked: false]", request.getEntity()
				.toString());
	}

	@Test
	void testRetrieveAccessTokenWithResourceOwnerPasswordGrantWithMissingParamsShouldThrow() {
		httpSender.setScope("email");

		httpSender.setOauthAuthenticationMethod(AbstractHttpSession.OauthAuthenticationMethod.RESOURCE_OWNER_PASSWORD_CREDENTIALS_QUERY_PARAMETERS);

		// Set each required field sequentially and check if an exception is thrown until all required fields are set
		ConfigurationException missingUsername = assertThrows(ConfigurationException.class, () -> httpSender.configure(), "Expected ConfigurationException when username is missing");
		assertTrue(missingUsername.getMessage().toLowerCase().contains("username"), "Expected exception message to mention 'username'");
		httpSender.setUsername("fakeCredentialUserName");

		ConfigurationException missingPassword = assertThrows(ConfigurationException.class, () -> httpSender.configure(), "Expected ConfigurationException when password is missing");
		assertTrue(missingPassword.getMessage().toLowerCase().contains("password"), "Expected exception message to mention 'password'");
		httpSender.setPassword("fakeCredentialPassword");

		ConfigurationException missingClientId = assertThrows(ConfigurationException.class, () -> httpSender.configure(), "Expected ConfigurationException when clientId is missing");
		assertTrue(missingClientId.getMessage().toLowerCase().contains("clientid"), "Expected exception message to mention 'clientId'");
		httpSender.setClientId(CLIENT_ID);

		ConfigurationException missingClientSecret = assertThrows(ConfigurationException.class, () -> httpSender.configure(), "Expected ConfigurationException when clientSecret is missing");
		assertTrue(missingClientSecret.getMessage().toLowerCase().contains("clientsecret"), "Expected exception message to mention 'clientSecret'");
		httpSender.setClientSecret(CLIENT_SECRET);

		assertDoesNotThrow(() -> httpSender.configure());
	}

	@Test
	void testSamlAssertion() throws Exception {
		httpSender.setTokenEndpoint("fakeEndpoint");
		httpSender.setOauthAuthenticationMethod(AbstractHttpSession.OauthAuthenticationMethod.SAML_ASSERTION);

		httpSender.setKeystore("/Signature/saml-keystore.p12");
		httpSender.setKeystorePassword("geheim");
		httpSender.setKeystoreAlias("myalias");
		httpSender.setKeystoreAliasPassword("geheim");

		httpSender.setTruststore("/Signature/saml-keystore.p12");
		httpSender.setTruststorePassword("geheim");
		httpSender.setTruststoreAuthAlias("myalias");

		httpSender.setClientId(CLIENT_ID);
		httpSender.setClientSecret(CLIENT_SECRET);

		httpSender.setSamlIssuer("www.successfactors.com");
		httpSender.setSamlAudience("www.successfactors.com");

		httpSender.configure();
		httpSender.start();

		AbstractOauthAuthenticator oauthAuthenticator = (AbstractOauthAuthenticator) AbstractHttpSession.OauthAuthenticationMethod.SAML_ASSERTION.newAuthenticator(httpSender);
		oauthAuthenticator.configure();
		HttpEntityEnclosingRequestBase request = oauthAuthenticator.createRequest(httpSender.getDomainAwareCredentials(), new ArrayList<>());

		final String body = StreamUtil.streamToString(request.getEntity().getContent(), "\n", "UTF-8");
		final String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);
		int bodyLength = body.length();

		assertEquals("POST", request.getMethod());
		assertHeaderPresent(request, "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

		assertTrue(decodedBody.contains("grant_type=urn:ietf:params:oauth:grant-type:saml2-bearer&client_id=fakeClientId&client_secret=fakeClientSecret&assertion="));

		assertEquals(String.format("[Content-Type: application/x-www-form-urlencoded; charset=UTF-8,Content-Length: %s,Chunked: false]", bodyLength), request.getEntity()
				.toString());

		Pattern pattern = Pattern.compile("(.*?)assertion=(.*?)($|&(.*?))");
		Matcher matcher = pattern.matcher(decodedBody);

		assertTrue(matcher.find());
		String base64EncodedAssertion = matcher.group(2);
		assertNotNull(base64EncodedAssertion);

		String assertion = new String(Base64.getDecoder().decode(base64EncodedAssertion));
		assertNotNull(assertion);
		assertTrue(assertion.length() > 50, "Assertion should be a long string");

		assertTrue(assertion.contains("<saml2:Issuer>www.successfactors.com</saml2:Issuer>"));
		assertTrue(assertion.contains("<saml2:Audience>www.successfactors.com</saml2:Audience>"));
		assertTrue(assertion.contains("<saml2:AttributeValue xsi:type=\"xs:string\">fakeClientId</saml2:AttributeValue>"));
	}

	public void assertHeaderPresent(HttpRequestBase method, String header, String expectedValue) {
		Header[] headers = method.getHeaders(header);
		assertNotNull(headers);
		assertEquals(1, headers.length, "Header must exist");
		assertEquals(expectedValue, headers[0].getValue());
	}
}
