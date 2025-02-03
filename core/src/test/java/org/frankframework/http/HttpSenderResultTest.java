/*
   Copyright 2018-2020 Nationale-Nederlanden

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
package org.frankframework.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.io.EmptyInputStream;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.http.AbstractHttpSender.HttpMethod;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;

@Log4j2
public class HttpSenderResultTest extends Mockito {

	private HttpSender sender = null;

	public HttpSender createHttpSender() throws IOException {
		InputStream dummyXmlString = new ByteArrayInputStream("<dummy result/>".getBytes());
		return createHttpSender(dummyXmlString, null);
	}

	public HttpSender createHttpSender(InputStream responseStream, String contentType) throws IOException {
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);

		ContentType cType = (contentType != null) ? ContentType.parse(contentType) : null;
		HttpEntity httpEntity = new InputStreamEntity(responseStream, responseStream.available(), cType);

		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(httpResponse.getEntity()).thenReturn(httpEntity);

		// Mock all requests
		when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class))).thenReturn(httpResponse);

		HttpSender sender = spy(new HttpSender());
		when(sender.getHttpClient()).thenReturn(httpClient);

		// Some default settings, url will be mocked.
		sender.setUrl("http://127.0.0.1/");
		sender.setIgnoreRedirects(true);
		sender.setVerifyHostname(false);
		sender.setAllowSelfSignedCertificates(true);

		this.sender = sender;
		return sender;
	}

	public HttpSender createHttpSenderFromFile(String testFile) throws IOException {
		InputStream file = getFile(testFile);
		byte[] fileArray = StreamUtil.streamToBytes(file);
		String contentType = null;

		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileArray)));
		for (String line; null != (line = reader.readLine()); ) {
			if (line.startsWith("Content-Type")) {
				contentType = line.substring(line.indexOf(":") + 1).trim();
				break;
			}
		}
		reader.close();

		if (contentType != null) {
			log.info("found Content-Type [" + contentType + "]");
		}

		InputStream dummyXmlString = new ByteArrayInputStream(fileArray);
		return createHttpSender(dummyXmlString, contentType);
	}

	@AfterEach
	public void setDown() {
		if (sender != null) {
			sender.stop();
			sender = null;
		}
	}

	private InputStream getFile(String file) throws IOException {
		String baseDir = "/org/frankframework/http/";
		URL url = this.getClass().getResource(baseDir + file);
		if (url == null) {
			throw new IOException("file not found");
		}
		return url.openStream();
	}

	@Test
	void simpleMockedHttpGet() throws Exception {
		HttpSender sender = createHttpSender();

		PipeLineSession session = new PipeLineSession();

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();

		// Use InputStream 'content' as result.
		String result = sender.sendMessageOrThrow(new Message(""), session).asString();
		assertEquals("<dummy result/>", result);
	}

	@Test
	void simpleMockedHttpPost() throws Exception {
		InputStream inputStream = spy(EmptyInputStream.INSTANCE);
		HttpSender sender = createHttpSender(inputStream, null);

		try (PipeLineSession session = new PipeLineSession()) {
			sender.setContentType("application/json");
			sender.setMethodType(HttpMethod.POST);

			sender.configure();
			sender.start();

			// Use InputStream 'content' as result.
			Message result = sender.sendMessageOrThrow(new Message("{\"temperature\": 0}"), session);
			result.closeOnCloseOf(session, "tralala");
			assertTrue(result.isNull());
		}

		verify(inputStream, times(1)).close();
	}

	@Test
	void simpleMultiPartResponse() throws Exception {
		HttpSender sender = createHttpSenderFromFile("multipart1.txt");

		PipeLineSession pls = new PipeLineSession();

		sender.setMethodType(HttpMethod.GET);
		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(new Message("tralala"), pls).asString();
		assertEquals("text default", result);

		int multipartAttachmentCount = 0;
		for (Map.Entry<String, Object> entry : pls.entrySet()) {
			log.debug("found multipart [" + entry.getKey() + "]");
			multipartAttachmentCount++;
		}
		assertEquals(2, multipartAttachmentCount);

		assertEquals("Content of a txt file.", pls.getMessage("multipart1").asString().trim());
		assertEquals("<!DOCTYPE html><title>Content of a html file.</title>", pls.getMessage("multipart2").asString().trim());
	}

	@Test
	void simpleMtomResponse() throws Exception {
		HttpSender sender = createHttpSenderFromFile("mtom-multipart.txt");

		PipeLineSession pls = new PipeLineSession();

		sender.setMethodType(HttpMethod.GET);
		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(new Message("tralala"), pls).asString();
		assertEquals("<soap:Envelope/>", result.trim());

		int multipartAttachmentCount = 0;
		for (Map.Entry<String, Object> entry : pls.entrySet()) {
			log.debug("found multipart key[" + entry.getKey() + "] type[" + (entry.getValue().getClass()) + "]");
			multipartAttachmentCount++;
		}
		assertEquals(1, multipartAttachmentCount);

		assertEquals("PDF-1.4 content", pls.getMessage("multipart1").asString().trim());
	}
}
