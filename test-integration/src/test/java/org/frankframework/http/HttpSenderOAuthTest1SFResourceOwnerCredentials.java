package org.frankframework.http;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.HttpSender.PostType;
import org.frankframework.http.HttpSenderBase.HttpMethod;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.util.LogUtil;

public class HttpSenderOAuthTest1SFResourceOwnerCredentials {
	protected Logger log = LogUtil.getLogger(this);

	protected String PROPERTY_FILE = "HttpSenderOAuth.properties";


	protected String tokenBaseUrl  = PropertyUtil.getProperty(PROPERTY_FILE, "tokenBaseUrl");
	protected String dataBaseUrl   = PropertyUtil.getProperty(PROPERTY_FILE, "dataBaseUrl");
	protected String apiContext    = PropertyUtil.getProperty(PROPERTY_FILE, "apiContext");
	protected String tokenContext  = PropertyUtil.getProperty(PROPERTY_FILE, "tokenContext");
	protected String url           = dataBaseUrl + apiContext;
	protected String tokenEndpoint = tokenBaseUrl + tokenContext;
	protected String client_id     = PropertyUtil.getProperty(PROPERTY_FILE, "client_id");
	protected String client_secret = PropertyUtil.getProperty(PROPERTY_FILE, "client_secret");
	protected boolean authTokenReq = PropertyUtil.getProperty(PROPERTY_FILE, "authenticatedTokenRequest", false);
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
	public void testSendResourceOwnerCredentialsRequestWithBothCredentialsAsParameters() throws Exception {

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

		Message result = sender.sendMessageOrThrow(new Message("<dummy/>"), session);

		log.debug("result: "+result.asString());
		assertEquals("200", session.getMessage("StatusCode").asString());
		assertThat(result.asString(), containsString("\"access_token\":"));
	}

	@Test
	@Disabled("This does not work for this provider")
	public void testSendResourceOwnerCredentialsRequestWithClientCredentialsAsBasicAuthentication() throws Exception {

		String tokenUrl = tokenEndpoint
				+"?grant_type=password"
				+"&username="+ URLEncoder.encode(username)
				+"&password="+ URLEncoder.encode(password);

		HttpSender sender = new HttpSender();
		sender.setUrl(tokenUrl);
		sender.setMethodType(HttpMethod.POST);
		sender.setUsername(client_id);
		sender.setPassword(client_secret);
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

		Message result = sender.sendMessageOrThrow(new Message("<dummy/>"), session);

		log.debug("result: "+result.asString());
		assertEquals("200", session.getMessage("StatusCode").asString());
		assertThat(result.asString(), containsString("\"access_token\":"));
	}



	@Test
	public void testEmbeddedOAuthResourceOwnerCredentialsFlow() throws Exception {
		HttpSender sender = new HttpSender();
		sender.setUrl(url);
		sender.setTokenEndpoint(tokenEndpoint);
		sender.setClientId(client_id);
		sender.setClientSecret(client_secret);
		sender.setAuthenticatedTokenRequest(authTokenReq);
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



	@Test
	public void test3917MultipleBinaryParts() throws Exception {
		String url = PropertyUtil.getProperty(PROPERTY_FILE, "salesforceUrl");
		String accept = "application/xml";
		String multiPartXml = "<parts>\n<part sessionKey=\"entity_document\" mimeType=\"application/xml\"/>\n"+
				"<part name=\"versiondata\" sessionKey=\"versiondata\" mimeType=\"image/png\"/>\n</parts>\n";
		String entity_document = "<document>\n" +
		"<PathOnClient>file1.png</PathOnClient>\n" +
		"</document>";

		String versiondataB64 = "iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAIAAACQKrqGAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAfpJREFUeNo0UkuP0zAYtBM3TZt0U9LuImilslR099DyEBISFw78A8QN7lz4Y5UQB7j1zF44VnCohCr6TNs0NF3n4cSxHUzYndt8Go/n8xiyPFcByHA4S8P5rykCUC1rJQiCa6xCePGo12q1QAFFJQywjDgbutoYWtVgOUpYeghdZ8cz5nneEeP/UgQ+fSG+f3BmgEZ3hEr2nglVx91f32/2Pn5onZ97rosAME9OUPr1cxxHwWoe+E45YCUCOAUpwNv+5WTyipUq7mI2Xy76/T76XYOL+QKHLudUNzSrZpCUbik4u7ikGZ9OpxQfkyRezGpIffO2+vxlup4TkWdmzU8EEumf1fr1+3etp8+2u30ShZZlhUkM81zkOfCWq1CvILOaU6ZqyverqxePn5zZ9d1mjzHOIaAsk4khhDmyLbY/6CwRQiQp5zjertZS2jhtZlykKcmiCOVSC2BJKJAzBZmGpo1/frt3tzGZTGazhW3bjVO7UilLCwSLNxNAyEAIqeMf4xBHDx90zZqVJMlms8H42Ol0KKXopglFieN4uVw6jjMYDOr1erlclnNd12VWzrl0VcAtfN8PgqDb7bbbbUkJIVEUNZtNSaWuaOsW/84pShiGo9FI2khXXkDO5eLyJ9xINU2TFw2Hw4QQSYtdQdUwWAFZVa/X+yvAAHMyLPKLDsUAAAAAAElFTkSuQmCC";
		byte[] versiondata = Base64.decodeBase64(versiondataB64);

		PipeLineSession session = new PipeLineSession();
		session.put("multiPartXml", multiPartXml);
		session.put("entity_document", new Message(entity_document, new MessageContext().withName("entity.xml")));
		session.put("versiondata", versiondata);

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

		sender.setHeadersParams("Accept");
		sender.setUrl(url);
		sender.setMethodType(HttpMethod.POST);
		sender.setPostType(PostType.FORMDATA);
		sender.setMultipartXmlSessionKey("multiPartXml");

		sender.setTimeout(20000);

		sender.addParameter(new Parameter("Accept", accept));
		sender.setResultStatusCodeSessionKey("resultCode");

		sender.configure();
		sender.open();

		Message result = sender.sendMessageOrThrow(new Message(multiPartXml), session);

		assertThat(result.asString(), StringContains.containsString("success"));
		assertEquals("201", session.get("resultCode"));
	}

}
