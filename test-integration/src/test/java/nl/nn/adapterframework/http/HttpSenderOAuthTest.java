package nl.nn.adapterframework.http;

import static org.junit.Assert.assertEquals;

import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.http.HttpSenderBase.HttpMethod;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.PropertyUtil;

public class HttpSenderOAuthTest {

	protected String PROPERTY_FILE = "HttpSenderOAuth.properties";

	
	protected String tokenBaseUrl  = PropertyUtil.getProperty(PROPERTY_FILE, "tokenBaseUrl");
	protected String dataBaseUrl   = PropertyUtil.getProperty(PROPERTY_FILE, "dataBaseUrl");
	protected String apiContext    = PropertyUtil.getProperty(PROPERTY_FILE, "apiContext");
	protected String oauthService  = PropertyUtil.getProperty(PROPERTY_FILE, "oauthService");
	protected String url           = dataBaseUrl + apiContext;
	protected String tokenEndpoint = tokenBaseUrl + oauthService;
	protected String client_id     = PropertyUtil.getProperty(PROPERTY_FILE, "client_id");
	protected String client_secret = PropertyUtil.getProperty(PROPERTY_FILE, "client_secret");
	protected String username      = PropertyUtil.getProperty(PROPERTY_FILE, "username");
	protected String password      = PropertyUtil.getProperty(PROPERTY_FILE, "password");
	protected String proxyHost     = PropertyUtil.getProperty(PROPERTY_FILE, "proxyHost");
	protected String proxyPort     = PropertyUtil.getProperty(PROPERTY_FILE, "proxyPort");
	protected String proxyUsername = PropertyUtil.getProperty(PROPERTY_FILE, "proxyUsername");
	protected String proxyPassword = PropertyUtil.getProperty(PROPERTY_FILE, "proxyPassword");

	protected String truststore         = PropertyUtil.getProperty(PROPERTY_FILE, "truststore");
	protected String truststorePassword = PropertyUtil.getProperty(PROPERTY_FILE, "truststorePassword");

	protected boolean useProxy = false;
	

	@Test
	public void testRetrieveTokenViaGetLikePost() throws Exception {
		
		String tokenUrl = tokenEndpoint 
				+"?grant_type=password"
				+"&username="+ URLEncoder.encode(username)
				+"&password="+ URLEncoder.encode(password)
				+"&client_id="+ client_id
				+"&client_secret="+ client_secret;
		
		HttpSender sender = new HttpSender();
		sender.setUrl(tokenUrl);
		sender.setMethodType(HttpMethod.POST);
		if (useProxy) {
			sender.setProxyHost(proxyHost);
			sender.setProxyPort(Integer.parseInt(proxyPort));
			sender.setProxyUsername(proxyUsername);
			sender.setProxyPassword(proxyPassword);
		}
		if (StringUtils.isNotEmpty(truststore)) {
			sender.setTruststore(truststore);
			sender.setTruststorePassword(truststorePassword);
		}
		sender.setAllowSelfSignedCertificates(true);
		sender.setResultStatusCodeSessionKey("StatusCode");
		sender.setTimeout(10000);
		
		sender.setHeadersParams("Accept,Content-Type");
		
//		sender.addParameter(new Parameter("Accept", "application/json"));
//		sender.addParameter(new Parameter("Content-Type", "application/json"));
		
		sender.configure();
		sender.open();
		
		PipeLineSession session = new PipeLineSession();
		
		Message result = sender.sendMessage(new Message("<dummy/>"), session);
		
		System.out.println("result: "+result.asString());
		assertEquals("200", session.getMessage("StatusCode").asString());
	}
	
	@Test
	public void testEmbeddedOAuth() throws Exception {
		HttpSender sender = new HttpSender();
		sender.setUrl(url);
		sender.setTokenEndpoint(tokenEndpoint);
		sender.setClientId(client_id);
		sender.setClientSecret(client_secret);
		sender.setUsername(username);
		sender.setPassword(password);
		if (useProxy) {
			sender.setProxyHost(proxyHost);
			sender.setProxyPort(Integer.parseInt(proxyPort));
			sender.setProxyUsername(proxyUsername);
			sender.setProxyPassword(proxyPassword);
		}
		if (StringUtils.isNotEmpty(truststore)) {
			sender.setTruststore(truststore);
			sender.setTruststorePassword(truststorePassword);
		}
		sender.setAllowSelfSignedCertificates(true);
		sender.setResultStatusCodeSessionKey("StatusCode");
		sender.setTimeout(1000);
		sender.setMaxExecuteRetries(0);
		
		sender.configure();
		sender.open();
		
		PipeLineSession session = new PipeLineSession();
		
		Message result = sender.sendMessage(new Message(""), session);
		
		System.out.println("result: "+result.asString());
		assertEquals("200", session.getMessage("StatusCode").asString());
	}
}
