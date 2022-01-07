package nl.nn.adapterframework.http.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;

import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;

public class HttpSenderAuthenticationTest extends SenderTestBase<HttpSender>{
	
	String USERINFOENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/userinfo";
	String INTROSPECTIONENDPOINT = "http://localhost:8888/auth/realms/iaf-test/protocol/openid-connect/token/introspect";
	
	String CLIENT_INFO_ENDPOINT = "http://localhost:8888/auth/admin/realms/iaf-test/clients/27ac1af3-5df6-4b05-96c6-368ebc171bd1";
	
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

}
