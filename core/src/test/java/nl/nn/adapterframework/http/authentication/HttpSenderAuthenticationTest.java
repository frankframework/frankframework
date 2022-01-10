package nl.nn.adapterframework.http.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;

import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;

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


	@Test
	public void testBasicAuthentication() throws Exception {
		sender.setUrl(authtenticatedService.getBasicEndpoint());
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertNotNull(result.asString());
	}

	@Test
	public void testBasicAuthenticationUnchallenged() throws Exception {
		sender.setUrl(authtenticatedService.getBasicEndpointUnchallenged());
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertNotNull(result.asString());
	}

	@Test
	public void testBasicAuthenticationNoCredentials() throws Exception {
		sender.setUrl(authtenticatedService.getBasicEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertEquals("401", session.getMessage(RESULT_STATUS_CODE_SESSIONKEY).asString());
		assertNotNull(result.asString());
	}

	@Test
	public void testOAuthAuthenticationUnchallenged() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpointUnchallenged());
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());
		sender.setTimeout(100000);

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertNotNull(result.asString());
	}

	@Test
	public void testOAuthAuthentication() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertNotNull(result.asString());
	}

	@Test
	public void testOAuthAuthenticationNoCredentials() throws Exception {
		sender.setUrl(authtenticatedService.getOAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertEquals("401", session.getMessage(RESULT_STATUS_CODE_SESSIONKEY).asString());
		assertNotNull(result.asString());
	}

	@Test
	public void testFlexibleAuthenticationBasic() throws Exception {
		sender.setUrl(authtenticatedService.getMultiAuthEndpoint());
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertNotNull(result.asString());
	}

	@Test
	public void testFlexibleAuthenticationBasicNoCredentials() throws Exception {
		sender.setUrl(authtenticatedService.getMultiAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertEquals("401", session.getMessage(RESULT_STATUS_CODE_SESSIONKEY).asString());
		assertNotNull(result.asString());
	}

	@Test
	public void testFlexibleAuthenticationOAuth() throws Exception {
		sender.setUrl(authtenticatedService.getMultiAuthEndpoint());
		sender.setTokenEndpoint(tokenServer.getEndpoint());
		sender.setUsername(tokenServer.getClientId());
		sender.setPassword(tokenServer.getClientSecret());

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertNotNull(result.asString());
	}
	@Test
	public void testFlexibleAuthenticationOAuthNoCredentials() throws Exception {
		sender.setUrl(authtenticatedService.getMultiAuthEndpoint());
		sender.setResultStatusCodeSessionKey(RESULT_STATUS_CODE_SESSIONKEY);
		sender.setTokenEndpoint(tokenServer.getEndpoint());

		sender.configure();
		sender.open();
		
		Message result = sendMessage("");
		assertEquals("401", session.getMessage(RESULT_STATUS_CODE_SESSIONKEY).asString());
		assertNotNull(result.asString());
	}
	
}
