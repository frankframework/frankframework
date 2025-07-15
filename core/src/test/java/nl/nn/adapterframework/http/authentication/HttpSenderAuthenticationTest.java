package nl.nn.adapterframework.http.authentication;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.http.HttpSender.PostType;
import nl.nn.adapterframework.http.HttpSenderBase.HttpMethod;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;

public class HttpSenderAuthenticationTest extends SenderTestBase<HttpSender>{

	String RESULT_STATUS_CODE_SESSIONKEY= "ResultStatusCodeSessionKey";

	@Rule
	public MockTokenServer tokenServer = new MockTokenServer();
	@Rule
	public MockAuthenticatedService authtenticatedService = new MockAuthenticatedService();

	@Override
	public HttpSender createSender() throws Exception {
		return new HttpSender();
	}

	//Send message
	private Message sendMessage() throws SenderException, TimeoutException {
		return super.sendMessage("");
	}

	//Send non-repeatable message
	private Message sendNonRepeatableMessage() throws SenderException, TimeoutException, IOException {
		if(sender.getHttpMethod() == HttpMethod.GET) fail("method not allowed when using no HttpEntity");
		InputStream is = new Message("dummy-string").asInputStream();
		return sendMessage(Message.asMessage(new FilterInputStream(is) {}));
	}

	//Send repeatable message
	private Message sendRepeatableMessage() throws SenderException, TimeoutException, IOException {
		if(sender.getHttpMethod() == HttpMethod.GET) fail("method not allowed when using no HttpEntity");
		return sendMessage(Message.asMessage(new Message("dummy-string").asByteArray()));
	}

	@Test
	public void testBasicAuthentication() throws Exception {
		sender.setUrl(authtenticatedService.getBasicEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	public void testBasicAuthenticationUnchallenged() throws Exception {
		sender.setUrl(authtenticatedService.getBasicEndpointUnchallenged());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	public void testBasicAuthenticationNoCredentials() throws Exception {
		sender.setUrl(authtenticatedService.getBasicEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("401", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}



	@Test
	public void testOAuthAuthentication() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	public void testOAuthAuthenticationWrongEndpoint() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthResourceNotFoundEndpoint());
		sender.setResultStatusCodeSessionKey(null);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		SenderResult result = sender.sendMessage(new Message(""), session);

		assertFalse(session.containsKey(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result);
		assertFalse(result.isSuccess());
		assertNotNull(result.getResult());
		assertNotNull(result.getResult().asString());
		assertEquals("404", result.getForwardName());
	}

	@Test
	public void testOAuthAuthenticationWrongEndpointStatusInSessionKey() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthResourceNotFoundEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		SenderResult result = sender.sendMessage(new Message(""), session);

		assertEquals("404", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result);
		assertTrue(result.isSuccess());
		assertNotNull(result.getResult());
		assertNotNull(result.getResult().asString());
		assertEquals("404", result.getForwardName());
	}

	@Test
	public void testOAuthAuthenticationWrongTokenEndpoint() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint() + "/xxxxx");
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());
		sender.setTimeout(10);

		sender.configure();
		sender.open();

		assertThrows(TimeoutException.class, () -> sender.sendMessage(new Message(""), session));
		assertNull(session.getString(RESULT_STATUS_CODE_SESSIONKEY));
	}

	@Test
	public void testOAuthAuthenticationUnchallenged() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpointUnchallenged());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	public void testOAuthAuthenticationNoCredentials() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());

		ConfigurationException exception = assertThrows(ConfigurationException.class, ()->sender.configure());
		assertThat(exception.getMessage(), containsString("clientAuthAlias or ClientId and ClientSecret must be specifie"));
	}

	@Test
	public void testOAuthAuthenticationTokenExpired() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpointFirstExpired());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Ignore("retrying unchallenged request/responses might cause endless authentication loops")
	@Test
	public void testOAuthAuthenticationUnchallengedExpired() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpointUnchallenged());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpointFirstExpired());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	public void testOAuthAuthenticationFailing() throws Exception {
		sender.setUrl(authtenticatedService.gethEndpointFailing());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("401", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}



	@Test
	public void testFlexibleAuthenticationBasic() throws Exception {
		sender.setUrl(authtenticatedService.getMultiAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	public void testFlexibleAuthenticationBasicNoCredentials() throws Exception {
		sender.setUrl(authtenticatedService.getMultiAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("401", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}

	@Test
	public void testFlexibleAuthenticationOAuth() throws Exception {
		sender.setUrl(authtenticatedService.getMultiAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test
	public void testRetryPayloadOnResetBasicAuth() throws Exception {
		sender.setUrl(authtenticatedService.getBasicEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		authtenticatedService.setScenarioState(authtenticatedService.SCENARIO_CONNECTION_RESET, authtenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test
	public void testRetryPayloadOnResetOAuth() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		authtenticatedService.setScenarioState(authtenticatedService.SCENARIO_CONNECTION_RESET, authtenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test //Mocking a Repeatable Message
	public void testRetryRepeatablePayloadOnResetOAuth() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.setPostType(PostType.BINARY);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.open();

		authtenticatedService.setScenarioState(authtenticatedService.SCENARIO_CONNECTION_RESET, authtenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		Message result = sendRepeatableMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test //Mocking a Non-Repeatable Message (avoids a NonRepeatableRequestException)
	public void testRetryNonRepeatablePayloadOnResetOAuth() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.setPostType(PostType.BINARY);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.open();

		authtenticatedService.setScenarioState(authtenticatedService.SCENARIO_CONNECTION_RESET, authtenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		SenderException exception = assertThrows(SenderException.class, () -> {
			sendNonRepeatableMessage();
		});
		assertTrue(exception.getCause() instanceof SocketException);
		assertEquals("(SocketException) Connection reset", exception.getMessage());
	}

	@Test //Mocking a Repeatable Multipart Message
	public void testRetryRepeatableMultipartPayloadOnResetOAuth() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.setFirstBodyPartName("request");

		String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
				+ "sessionKey=\"part_file\" size=\"72833\" "
				+ "mimeType=\"application/pdf\"/></parts>";
		session.put("multipartXml", xmlMultipart);
		session.put("part_file", "<dummy xml file/>"); // as it stands, only text is repeatable

		sender.setMultipartXmlSessionKey("multipartXml");
		sender.addParameter(ParameterBuilder.create("xml-part", "<ik><ben/><xml/></ik>"));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("part_file"));

		sender.setPostType(PostType.MTOM);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.open();

		authtenticatedService.setScenarioState(authtenticatedService.SCENARIO_CONNECTION_RESET, authtenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		Message result = sendRepeatableMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}


	@Test //Mocking a Non-Repeatable Multipart Message (avoids a NonRepeatableRequestException)
	public void testRetryNonRepeatableMultipartPayloadOnResetOAuth() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		Message nonRepeatableMessage = Message.asMessage(new FilterInputStream(new Message("dummy-string").asInputStream()) {});
		session.put("binaryPart", nonRepeatableMessage);
		sender.addParameter(ParameterBuilder.create("xml-part", "<ik><ben/><xml/></ik>"));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.setPostType(PostType.MTOM);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.open();

		authtenticatedService.setScenarioState(authtenticatedService.SCENARIO_CONNECTION_RESET, authtenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		SenderException exception = assertThrows(SenderException.class, () -> {
			sendNonRepeatableMessage();
		});
		assertTrue(exception.getCause() instanceof SocketException);
		assertEquals("(SocketException) Connection reset", exception.getMessage());
	}


	@Test
	public void testRetryRepeatableMultipartPayloadOnOAuthAuthenticationTokenExpired() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpointFirstExpired());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message repeatableMessage = Message.asMessage("dummy-string".getBytes());
		session.put("binaryPart", repeatableMessage);
		sender.addParameter(ParameterBuilder.create("xml-part", "<ik><ben/><xml/></ik>"));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.setPostType(PostType.MTOM);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.open();

		authtenticatedService.setScenarioState(authtenticatedService.SCENARIO_CONNECTION_RESET, authtenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		Message result = sendNonRepeatableMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertEquals("{}", result.asString());
	}

	@Test
	public void testRetryNonRepeatableMultipartPayloadOnOAuthAuthenticationTokenExpired() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpointFirstExpired());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());

		sender.configure();
		sender.open();

		Message nonRepeatableMessage = Message.asMessage(new FilterInputStream(new Message("dummy-string").asInputStream()) {});
		session.put("binaryPart", nonRepeatableMessage);
		sender.addParameter(ParameterBuilder.create("xml-part", "<ik><ben/><xml/></ik>"));
		sender.addParameter(ParameterBuilder.create().withName("binary-part").withSessionKey("binaryPart"));

		sender.setPostType(PostType.MTOM);
		sender.setMethodType(HttpMethod.POST);

		sender.configure();
		sender.open();

		authtenticatedService.setScenarioState(authtenticatedService.SCENARIO_CONNECTION_RESET, authtenticatedService.SCENARIO_STATE_RESET_CONNECTION);

		SenderException exception = assertThrows(SenderException.class, () -> {
			sendNonRepeatableMessage();
		});
		exception.printStackTrace();
		assertTrue(exception.getCause() instanceof SocketException);
		assertEquals("(SocketException) Connection reset", exception.getMessage());
	}

	@Test
	public void testRetryOnResetAuthenticated() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setClientId(tokenServer.getClientId());
		sender.setClientSecret(tokenServer.getClientSecret());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTimeout(100000);

		sender.configure();
		sender.open();

		tokenServer.setScenarioState(tokenServer.SCENARIO_CONNECTION_RESET, tokenServer.SCENARIO_STATE_RESET_CONNECTION);

		Message result = sendMessage();
		assertEquals("200", session.getString(RESULT_STATUS_CODE_SESSIONKEY));
		assertNotNull(result.asString());
	}
}
