package org.frankframework.http;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.HttpSenderBase.HttpMethod;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.util.LogUtil;

public class HttpSenderOAuthTest3NNClientCredentials {
	protected Logger log = LogUtil.getLogger(this);

	protected String PROPERTY_FILE = "HttpSenderOAuth3.properties";


	protected String tokenBaseUrl  = PropertyUtil.getProperty(PROPERTY_FILE, "tokenBaseUrl");
	protected String dataBaseUrl   = PropertyUtil.getProperty(PROPERTY_FILE, "dataBaseUrl");
	protected String apiContext    = PropertyUtil.getProperty(PROPERTY_FILE, "apiContext");
	protected String tokenContext  = PropertyUtil.getProperty(PROPERTY_FILE, "tokenContext");
	protected String url           = dataBaseUrl + apiContext;
	protected String tokenEndpoint = tokenBaseUrl + tokenContext;
	protected String client_id     = PropertyUtil.getProperty(PROPERTY_FILE, "client_id");
	protected String client_secret = PropertyUtil.getProperty(PROPERTY_FILE, "client_secret");
	protected boolean authTokenReq = PropertyUtil.getProperty(PROPERTY_FILE, "authenticatedTokenRequest", false);
//	protected String username      = PropertyUtil.getProperty(PROPERTY_FILE, "username");
//	protected String password      = PropertyUtil.getProperty(PROPERTY_FILE, "password");
	protected String proxyHost     = PropertyUtil.getProperty(PROPERTY_FILE, "proxyHost");
	protected String proxyPort     = PropertyUtil.getProperty(PROPERTY_FILE, "proxyPort");
	protected String proxyUsername = PropertyUtil.getProperty(PROPERTY_FILE, "proxyUsername");
	protected String proxyPassword = PropertyUtil.getProperty(PROPERTY_FILE, "proxyPassword");

	protected String truststore         = PropertyUtil.getProperty(PROPERTY_FILE, "truststore");
	protected String truststorePassword = PropertyUtil.getProperty(PROPERTY_FILE, "truststorePassword");

	protected boolean useProxy = false;


	@Test
	@Disabled("must use basic authentication for this provider")
	public void testSendClientCredentialsTokenRequestUsingRequestParameters() throws Exception {

		String tokenUrl = tokenEndpoint+
				"?grant_type=client_credentials"
				+"&client_id="+ client_id
				+"&client_secret="+ client_secret;

		HttpSender sender = new HttpSender();
		sender.setContentType("application/x-www-form-urlencoded");
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

		Message result = sender.sendMessageOrThrow(new Message("dummy"), session);

		log.debug("result: "+result.asString());
		assertEquals("200", session.getMessage("StatusCode").asString());
		assertThat(result.asString(), containsString("\"access_token\":"));
	}

	@Test
	public void testSendClientCredentialsTokenRequestUsingBasicAuthentication() throws Exception {

		String tokenUrl = tokenEndpoint+"?grant_type=client_credentials";

		HttpSender sender = new HttpSender();
		sender.setContentType("application/x-www-form-urlencoded");
		sender.setUsername(client_id);
		sender.setPassword(client_secret);
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

		Message result = sender.sendMessageOrThrow(new Message("dummy"), session);

		log.debug("result: "+result.asString());
		assertEquals("200", session.getMessage("StatusCode").asString());
		assertThat(result.asString(), containsString("\"access_token\":"));
	}



	@Test
	public void testEmbeddedOAuthWitClientCredentialsOnly() throws Exception {
		HttpSender sender = new HttpSender();
		sender.setUrl(url);
		sender.setTokenEndpoint(tokenEndpoint);
		sender.setClientId(client_id);
		sender.setClientSecret(client_secret);
		sender.setAuthenticatedTokenRequest(authTokenReq);

		sender.setMethodType(HttpMethod.POST);
		sender.setHeadersParams("Accept");
//		sender.setUsername(username);
//		sender.setPassword(password);
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

		sender.addParameter(new Parameter("Accept", "application/json"));

		sender.configure();
		sender.open();

		PipeLineSession session = new PipeLineSession();

		Message result = sender.sendMessageOrThrow(new Message(""), session);
		log.debug("result: "+result.asString());
		assertEquals("200", session.getMessage("StatusCode").asString());

		log.debug("Wait 1 second");
		Thread.sleep(1000);
		log.debug("Test again");

		result = sender.sendMessageOrThrow(new Message(""), session);
		log.debug("result: "+result.asString());
		assertEquals("200", session.getMessage("StatusCode").asString());
	}


}
