package org.frankframework.extensions.akamai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.akamai.NetStorageSender.Action;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;

@Log4j2
public class NetStorageSenderResultTest {
	private final String BASEDIR = "/Http/Responses/";

	public NetStorageSender createHttpSender() throws IOException {
		InputStream dummyXmlString = new ByteArrayInputStream("<dummy result/>".getBytes());
		return createHttpSender(dummyXmlString, null);
	}

	public NetStorageSender createHttpSender(InputStream responseStream, String contentType) throws IOException {
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		HttpEntity httpEntity = mock(HttpEntity.class);

		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);

		if(contentType != null) {
			Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
			when(httpEntity.getContentType()).thenReturn(contentTypeHeader);
		}
		when(httpEntity.getContent()).thenReturn(responseStream);
		when(httpResponse.getEntity()).thenReturn(httpEntity);

		// Mock all requests
		when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class))).thenReturn(httpResponse);

		NetStorageSender sender = spy(new NetStorageSender());
		when(sender.getHttpClient()).thenReturn(httpClient);

		// Some default settings, url will be mocked.
		sender.setUrl("http://127.0.0.1/");
		sender.setVerifyHostname(false);
		sender.setAllowSelfSignedCertificates(true);
		sender.setCpCode("dummy-code");

		return sender;
	}

	public NetStorageSender createHttpSenderFromFile(String testFile) throws IOException {
		InputStream file = getFile(testFile);
		byte[] fileArray = StreamUtil.streamToBytes(file);
		String contentType = null;

		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileArray)));
		for (String line = null; null != (line = reader.readLine());) {
			if(line.startsWith("Content-Type")) {
				contentType = line.substring(line.indexOf(":") + 1).trim();
				break;
			}
		}
		reader.close();

		if(contentType != null)
			log.info("found Content-Type ["+contentType+"]");

		InputStream dummyXmlString = new ByteArrayInputStream(fileArray);
		return createHttpSender(dummyXmlString, contentType);
	}

	private InputStream getFile(String file) throws IOException {
		URL url = this.getClass().getResource(BASEDIR+file);
		if (url == null) {
			throw new IOException("file not found");
		}
		return url.openStream();
	}

	// Actual tests

	@Test
	public void du() throws Exception {
		NetStorageSender sender = createHttpSenderFromFile("du-api-response.xml");
		sender.setAction(Action.DU);

		sender.configure();
		sender.start();

		PipeLineSession pls = new PipeLineSession();
		String result = sender.sendMessage(new Message("tralala"), pls).getResult().asString();
		assertEquals(StreamUtil.streamToString(getFile("du-sender-result.xml")), result);
	}

	@Test
	public void dir() throws Exception {
		NetStorageSender sender = createHttpSenderFromFile("dir-api-response.xml");
		sender.setAction(Action.DIR);

		sender.configure();
		sender.start();

		PipeLineSession pls = new PipeLineSession();
		String result = sender.sendMessage(new Message("tralala"), pls).getResult().asString();
		assertEquals(StreamUtil.streamToString(getFile("dir-sender-result.xml")), result);
	}
}
