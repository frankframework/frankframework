/*
   Copyright 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

public class WebServiceSenderResultTest extends Mockito {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private WebServiceSender sender = null;

	public WebServiceSender createWebServiceSender(InputStream responseStream, String contentType, int statusCode) throws IOException {
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		HttpEntity httpEntity = mock(HttpEntity.class);

		when(statusLine.getStatusCode()).thenReturn(statusCode);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);

		if(contentType != null) {
			Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
			when(httpEntity.getContentType()).thenReturn(contentTypeHeader);
		}
		when(httpEntity.getContent()).thenReturn(responseStream);
		when(httpResponse.getEntity()).thenReturn(httpEntity);

		//Mock all requests
		when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class))).thenReturn(httpResponse);

		WebServiceSender sender = spy(new WebServiceSender());
		when(sender.getLocalResource()).thenReturn(httpClient);

		//Some default settings, url will be mocked.
		sender.setUrl("http://127.0.0.1/");
		sender.setIgnoreRedirects(true);
		sender.setVerifyHostname(false);
		sender.setAllowSelfSignedCertificates(true);

		this.sender = sender;
		return sender;
	}

	public WebServiceSender createWebServiceSenderFromFile(String testFile, String contentType, int statusCode) throws IOException {
		InputStream file = getFile(testFile);
		return createWebServiceSender(file, contentType, statusCode);
	}

	@After
	public void setDown() throws SenderException {
		if (sender != null) {
			sender.close();
			sender = null;
		}
	}

	private final String BASEDIR = "/nl/nn/adapterframework/http/";
	private InputStream getFile(String file) throws IOException {
		URL url = this.getClass().getResource(BASEDIR+file);
		if (url == null) {
			throw new IOException("file not found");
		}
		return url.openStream();
	}

	@Test
	public void simpleSoapMultiPartResponseMocked() throws Exception {
		WebServiceSender sender = createWebServiceSenderFromFile("soapMultipart.txt", "multipart/form-data", 200);

		PipeLineSession pls = new PipeLineSession();

		sender.configure();
		sender.open();

		String result = sender.sendMessageOrThrow(new Message("tralala"), pls).asString();
		assertEquals("<TestElement xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">test value</TestElement>", result);

		int multipartAttachmentCount = 0;
		for (Map.Entry<String, Object> entry : pls.entrySet()) {
			System.out.println("found multipart ["+entry.getKey()+"]");
			multipartAttachmentCount++;
		}
		assertEquals(2, multipartAttachmentCount);

		InputStream multipart1 = pls.getMessage("multipart1").asInputStream();
		assertEquals("Content of a txt file.", StreamUtil.streamToString(multipart1).trim());

		InputStream multipart2 = pls.getMessage("multipart2").asInputStream();
		assertEquals("<!DOCTYPE html><title>Content of a html file.</title>", StreamUtil.streamToString(multipart2).trim());
	}

	@Test
	public void simpleSoapMultiPartResponseMocked500StatusCode() throws Exception {
		thrown.expect(SenderException.class);
		WebServiceSender sender = createWebServiceSenderFromFile("soapMultipart.txt", "multipart/form-data", 500);

		PipeLineSession pls = new PipeLineSession();

		sender.configure();
		sender.open();

		sender.sendMessageOrThrow(new Message("tralala"), pls).asString();
	}

	@Test
	public void simpleInvalidMultipartResponse() throws Exception {
		thrown.expectMessage("Missing start boundary");

		WebServiceSender sender = createWebServiceSenderFromFile("soapMultipart2.txt", "multipart/form-data", 200);

		PipeLineSession pls = new PipeLineSession();

		sender.configure();
		sender.open();

		sender.sendMessageOrThrow(new Message("tralala"), pls).asString();
	}

	@Test
	public void soapFault() throws Exception {
		thrown.expect(SenderException.class);
		thrown.expectMessage("SOAP fault [soapenv:Client]: much error");

		WebServiceSender sender = createWebServiceSenderFromFile("soapFault.txt", "text/xml", 500);

		PipeLineSession pls = new PipeLineSession();

		sender.configure();
		sender.open();

		sender.sendMessageOrThrow(new Message("tralala"), pls);
	}

	@Test
	public void simpleMtomResponseMockedHttpGet() throws Exception {
		WebServiceSender sender = createWebServiceSenderFromFile("mtom-multipart2.txt", "multipart/related", 200);

		PipeLineSession pls = new PipeLineSession();

		sender.configure();
		sender.open();

		String result = sender.sendMessageOrThrow(new Message("tralala"), pls).asString();
		assertEquals("<TestElement xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">test value</TestElement>", result.trim());

		int multipartAttachmentCount = 0;
		for (Map.Entry<String, Object> entry : pls.entrySet()) {
			System.out.println("found multipart key["+entry.getKey()+"] type["+(entry.getValue().getClass())+"]");
			multipartAttachmentCount++;
		}
		assertEquals(1, multipartAttachmentCount);

		InputStream multipart1 = pls.getMessage("multipart1").asInputStream();
		assertEquals("PDF-1.4 content", StreamUtil.streamToString(multipart1).trim());
	}
}
