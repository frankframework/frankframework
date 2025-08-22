package org.frankframework.http.authentication;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.http.AbstractHttpSender.HttpMethod;
import org.frankframework.http.AbstractHttpSession;
import org.frankframework.http.HttpEntityType;
import org.frankframework.http.HttpSender;
import org.frankframework.senders.SenderTestBase;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;

public class HttpSenderAuthenticationTest extends SenderTestBase<HttpSender> {
	private final boolean useMockServer = true;

	private final String KEYCLOAK_SERVER = "http://localhost:8888";
	private final String KEYCLOAK_PATH = "/auth/realms/iaf-test/protocol/openid-connect/token";

	String LOCAL_PATH = "/token";

	private final @Getter String path = useMockServer ? LOCAL_PATH : KEYCLOAK_PATH;

	private Message result;

	String RESULT_STATUS_CODE_SESSIONKEY= "ResultStatusCodeSessionKey";

	@RegisterExtension
	public static WireMockExtension tokenServer = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	@RegisterExtension
	public static WireMockExtension authenticatedService = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		MockTokenServer.createStubs(tokenServer);
		MockAuthenticatedService.createStubs(authenticatedService);
		super.setUp();
	}

	@AfterEach
	public void after() throws Exception {
		super.tearDown();
		if (result != null) {
			result.close();
		}
	}

	public String getTokenEndpoint() {
		return useMockServer ? "http://localhost:"+tokenServer.getPort() : KEYCLOAK_SERVER;
	}

	public String getServiceEndpoint() {
		return useMockServer ? "http://localhost:"+authenticatedService.getPort() : KEYCLOAK_SERVER;
	}

	@Override
	public HttpSender createSender() throws Exception {
		HttpSender httpSender = new HttpSender();
		httpSender.setName("senderAuthenticationTest");
		return httpSender;
	}

	// Send message
	private Message sendMessage() throws SenderException, TimeoutException {
		return super.sendMessage("");
	}

	// Send non-repeatable message
	private Message sendNonRepeatableMessage() throws SenderException, TimeoutException, IOException {
		if(sender.getHttpMethod() == HttpMethod.GET) fail("method not allowed when using no HttpEntity");
		InputStream is = new Message("dummy-string").asInputStream();
		return sendMessage(new Message(new FilterInputStream(is) {}));
	}

	// Send repeatable message
	private Message sendRepeatableMessage() throws SenderException, TimeoutException, IOException {
		if(sender.getHttpMethod() == HttpMethod.GET) fail("method not allowed when using no HttpEntity");
		return sendMessage(new Message(new Message("dummy-string").asByteArray()));
	}

	@Test
	void testBasicAuthentication() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.BASIC_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setUsername(MockTokenServer.CLIENT_ID);
		sender.setPassword(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testBasicClientAuthentication() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.BASIC_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testBasicAuthenticationUsingClientAlias() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.BASIC_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setClientAlias("oauth.credentials");

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testBasicAuthenticationUsingAuthAlias() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.BASIC_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setAuthAlias("oauth.credentials");

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testBasicAuthenticationUnchallenged() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.BASIC_UNCHALLENGED_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setUsername(MockTokenServer.CLIENT_ID);
		sender.setPassword(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testBasicAuthenticationNoCredentials() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.BASIC_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("401", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testOAuthAuthenticatedTokenRequest() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);
		sender.setAuthenticatedTokenRequest(true);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	public void testOAuthAuthenticationWrongEndpoint() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_RESOURCE_NOT_FOUND_PATH);
		sender.setResultStatusCodeSessionKey(null);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		SenderResult senderResult = sender.sendMessage(new Message(""), session);

		assertFalse(session.containsKey(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(senderResult);
		assertFalse(senderResult.isSuccess());
		assertNotNull(senderResult.getResult());
		assertNotNull(senderResult.getResult().asString());
		assertEquals("404", senderResult.getForwardName());
	}

	@Test
	public void testOAuthAuthenticationWrongEndpointStatusInSessionKey() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_RESOURCE_NOT_FOUND_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		SenderResult senderResult = sender.sendMessage(new Message(""), session);

		assertEquals("404", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(senderResult);
		assertTrue(senderResult.isSuccess());
		assertNotNull(senderResult.getResult());
		assertNotNull(senderResult.getResult().asString());
		assertEquals("404", senderResult.getForwardName());
	}

	@Test
	public void testOAuthAuthenticationWrongTokenEndpoint() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH + "/xxxxx");
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);
		sender.setTimeout(10);

		sender.configure();
		sender.start();

		assertThrows(TimeoutException.class, () -> sender.sendMessage(new Message(""), session));
		assertNull(session.getString(RESULT_STATUS_CODE_SESSIONKEY));
	}

	@Test
	void testOAuthAuthenticatedTokenRequest2() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.setOauthAuthenticationMethod(AbstractHttpSession.OauthAuthenticationMethod.CLIENT_CREDENTIALS_BASIC_AUTH);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testOAuthAuthenticatedTokenRequestUsingClientAlias() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientAlias("oauth.credentials");

		sender.setAuthenticatedTokenRequest(true);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testOAuthAuthenticationWithAuthenticatedTokenRequest() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);
		sender.setAuthenticatedTokenRequest(true);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testOAuthAuthenticationWithoutAuthenticatedTokenRequest() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);
		sender.setAuthenticatedTokenRequest(false);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test
	void testOAuthAuthenticationUnchallenged() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_UNCHALLENGED_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test
	void testOAuthAuthenticationNoCredentials() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);

		ConfigurationException exception = assertThrows(ConfigurationException.class, sender::configure);
		assertThat(exception.getMessage().toLowerCase(), containsString("clientsecret"));
		assertThat(exception.getMessage().toLowerCase(), containsString("clientid"));
	}

	@Test
	void testOAuthAuthenticationTokenExpired() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.EXPIRED_PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Disabled("retrying unchallenged request/responses might cause endless authentication loops")
	@Test
	void testOAuthAuthenticationUnchallengedExpired() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_UNCHALLENGED_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.EXPIRED_PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testOAuthAuthenticationFailing() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.FAILING_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("401", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testFlexibleAuthenticationBasic() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.ANY_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setUsername(MockTokenServer.CLIENT_ID);
		sender.setPassword(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testFlexibleAuthenticationBasicNoCredentials() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.ANY_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("401", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testFlexibleAuthenticationOAuth() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.ANY_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test
	void testRetryPayloadOnResetBasicAuth() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.BASIC_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setUsername(MockTokenServer.CLIENT_ID);
		sender.setPassword(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		authenticatedService.setScenarioState(MockAuthenticatedService.SCENARIO_CONNECTION_RESET, MockAuthenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test
	void testRetryPayloadOnResetOAuth() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		authenticatedService.setScenarioState(MockAuthenticatedService.SCENARIO_CONNECTION_RESET, MockAuthenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test //Mocking a Repeatable Message
	void testRetryRepeatablePayloadOnResetOAuth() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.setPostType(HttpEntityType.BINARY);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.start();

		authenticatedService.setScenarioState(MockAuthenticatedService.SCENARIO_CONNECTION_RESET, MockAuthenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		result = sendRepeatableMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test //Mocking a Non-Repeatable Message (avoids a NonRepeatableRequestException)
	@Disabled("Messages are now always repeatable")
	void testRetryNonRepeatablePayloadOnResetOAuth() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.setPostType(HttpEntityType.BINARY);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.start();

		authenticatedService.setScenarioState(MockAuthenticatedService.SCENARIO_CONNECTION_RESET, MockAuthenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		SenderException exception = assertThrows(SenderException.class, this::sendNonRepeatableMessage);
    	assertInstanceOf(SocketException.class, exception.getCause());
		assertEquals("(SocketException) Connection reset", exception.getMessage());
	}

	@Test //Mocking a Repeatable Multipart Message
	void testRetryRepeatableMultipartPayloadOnResetOAuth() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.setFirstBodyPartName("request");

		String xmlMultipart = """
				<parts><part type="file" name="document.pdf" \
				sessionKey="part_file" size="72833" \
				mimeType="application/pdf"/></parts>\
				""";
		session.put("multipartXml", xmlMultipart);
		session.put("part_file", "<dummy xml file/>"); // as it stands, only text is repeatable

		sender.setMultipartXmlSessionKey("multipartXml");
		sender.addParameter(ParameterBuilder.create("xml-part", "<ik><ben/><xml/></ik>"));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("part_file"));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.start();

		authenticatedService.setScenarioState(MockAuthenticatedService.SCENARIO_CONNECTION_RESET, MockAuthenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		result = sendRepeatableMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test //Mocking a Non-Repeatable Multipart Message (avoids a NonRepeatableRequestException)
	@Disabled("Messages are now always repeatable")
	void testRetryNonRepeatableMultipartPayloadOnResetOAuth() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		Message nonRepeatableMessage = new Message(new FilterInputStream(new Message("dummy-string").asInputStream()) {});
		session.put("binaryPart", nonRepeatableMessage);
		sender.addParameter(ParameterBuilder.create("xml-part", "<ik><ben/><xml/></ik>"));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.start();

		authenticatedService.setScenarioState(MockAuthenticatedService.SCENARIO_CONNECTION_RESET, MockAuthenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		SenderException exception = assertThrows(SenderException.class, this::sendNonRepeatableMessage);
		assertInstanceOf(SocketException.class, exception.getCause());
		assertEquals("(SocketException) Connection reset", exception.getMessage());
	}


	@Test
	void testRetryRepeatableMultipartPayloadOnOAuthAuthenticationTokenExpired() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.EXPIRED_PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		Message repeatableMessage = new Message("dummy-string".getBytes());
		session.put("binaryPart", repeatableMessage);
		sender.addParameter(ParameterBuilder.create("xml-part", "<ik><ben/><xml/></ik>"));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.start();

		authenticatedService.setScenarioState(MockAuthenticatedService.SCENARIO_CONNECTION_RESET, MockAuthenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		result = sendNonRepeatableMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertEquals("{}", result.asString());
	}

	@Test
	@Disabled("Messages are now always repeatable")
	void testRetryNonRepeatableMultipartPayloadOnOAuthAuthenticationTokenExpired() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.EXPIRED_PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.configure();
		sender.start();

		Message nonRepeatableMessage = new Message(new FilterInputStream(new Message("dummy-string").asInputStream()) {});
		session.put("binaryPart", nonRepeatableMessage);
		sender.addParameter(ParameterBuilder.create("xml-part", "<ik><ben/><xml/></ik>"));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.setPostType(HttpEntityType.MTOM);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.start();

		authenticatedService.setScenarioState(MockAuthenticatedService.SCENARIO_CONNECTION_RESET, MockAuthenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		SenderException exception = assertThrows(SenderException.class, this::sendNonRepeatableMessage);
		assertInstanceOf(SocketException.class, exception.getCause());
		assertEquals("(SocketException) Connection reset", exception.getMessage());
	}

	@Test
	void testRetryOnResetAuthenticated() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTimeout(100000);

		sender.configure();
		sender.start();

		tokenServer.setScenarioState(MockTokenServer.SCENARIO_CONNECTION_RESET, MockTokenServer.SCENARIO_STATE_RESET_CONNECTION);

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	void testSamlAssertion() throws Exception {
		sender.setUrl(getServiceEndpoint() + MockAuthenticatedService.OAUTH_PATH);
		sender.setTokenEndpoint(getTokenEndpoint() + MockTokenServer.PATH);
		sender.setOauthAuthenticationMethod(AbstractHttpSession.OauthAuthenticationMethod.SAML_ASSERTION);

		sender.setKeystore("/Signature/saml-keystore.p12");
		sender.setKeystorePassword("geheim");
		sender.setKeystoreAlias("myalias");
		sender.setKeystoreAliasPassword("geheim");

		sender.setTruststore("/Signature/saml-keystore.p12");
		sender.setTruststorePassword("geheim");
		sender.setTruststoreAuthAlias("myalias");

		sender.setClientId(MockTokenServer.CLIENT_ID);
		sender.setClientSecret(MockTokenServer.CLIENT_SECRET);

		sender.setSamlIssuer("www.successfactors.com");
		sender.setSamlAudience("www.successfactors.com");

		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTimeout(100000);

		sender.configure();
		sender.start();

		result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

}
