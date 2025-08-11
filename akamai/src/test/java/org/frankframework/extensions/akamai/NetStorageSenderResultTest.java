package org.frankframework.extensions.akamai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.extensions.akamai.NetStorageSender.Action;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TimeProvider;

@Log4j2
public class NetStorageSenderResultTest {
	private static final String BASEDIR = "/http/responses/";

	@AfterEach
	void tearDown() {
		TimeProvider.resetClock();
	}

	private NetStorageSender createHttpSender(CloseableHttpResponse httpResponse) throws IOException {
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

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

	private CloseableHttpResponse createHttpResponse(InputStream responseStream, int statusCode, String contentType, Map<String, String> headers) throws IOException {
		StatusLine statusLine = mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(statusCode);

		CloseableBasicHttpResponse httpResponse = new CloseableBasicHttpResponse(statusLine);

		ContentType cType = (contentType != null) ? ContentType.parse(contentType) : null;
		httpResponse.setEntity(new InputStreamEntity(responseStream, responseStream.available(), cType));

		headers.entrySet().stream()
				.map(entry -> new BasicHeader(entry.getKey(), entry.getValue()))
				.forEach(httpResponse::addHeader);

		return httpResponse;
	}

	private static class CloseableBasicHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {
		public CloseableBasicHttpResponse(StatusLine statusLine) {
			super(statusLine);
		}

		@Override
		public void close() throws IOException {
			HttpEntity entity = getEntity();
			if (entity != null) {
				entity.getContent().close();
			}
		}
	}

	private NetStorageSender createHttpSenderFromFile(String testFile) throws IOException {
		InputStream file = getFile(testFile);
		byte[] fileArray = StreamUtil.streamToBytes(file);
		String contentType = "text/xml";

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

		InputStream responseStream = new ByteArrayInputStream(fileArray);
		CloseableHttpResponse httpResponse = createHttpResponse(responseStream, 200, contentType, Map.of());

		return createHttpSender(httpResponse);
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

	@Test
	public void errorScenarioOutTime() throws Exception {
		CloseableHttpResponse httpResponse = createHttpResponse(EmptyInputStream.INSTANCE, 400, null, Map.of("Date", "Wed, 01 Jan 2020 00:00:00 GMT"));
		NetStorageSender sender = createHttpSender(httpResponse);
		sender.setResultStatusCodeSessionKey("dummy");
		sender.setAction(Action.DIR);

		sender.configure();
		sender.start();

		PipeLineSession pls = new PipeLineSession();
		SenderException ex = assertThrows(SenderException.class, () -> sender.sendMessage(new Message("tralala"), pls));
		assertEquals("Local server Date is more than 30s out of sync with Remote server", ex.getMessage());
	}

	@Test
	public void errorScenarioInTime() throws Exception {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
		InputStream content = new ByteArrayInputStream("<error />".getBytes());

		CloseableHttpResponse httpResponse = createHttpResponse(content, 400, "text/xml", Map.of("Date", dateFormatter.format(TimeProvider.now())));
		NetStorageSender sender = createHttpSender(httpResponse);
		sender.setResultStatusCodeSessionKey("dummy");
		sender.setAction(Action.DIR);

		sender.configure();
		sender.start();

		PipeLineSession pls = new PipeLineSession();
		String result = sender.sendMessage(new Message("tralala"), pls).getResult().asString();
		assertEquals("""
				<result>
					<statuscode>400</statuscode>
					<error>&lt;error /&gt;</error>
				</result>""", result);
	}

	@Test
	public void detectDrift() {
		NetStorageSender sender = new NetStorageSender();
		assertFalse(sender.detectedTimeDrift("", 10));
		assertTrue(sender.detectedTimeDrift("Wed, 01 Jan 2020 00:00:00 GMT", 10));

		TimeProvider.setTime(ZonedDateTime.of(2025, 7, 25, 0, 0, 10, 0, ZoneId.of("GMT")));
		assertFalse(sender.detectedTimeDrift("Fri, 25 Jul 2025 00:00:10 GMT", 1)); // Drift is 0 ms
		assertTrue(sender.detectedTimeDrift("Fri, 25 Jul 2025 00:00:00 GMT", 9999)); // Drift is 10_000 ms
		assertFalse(sender.detectedTimeDrift("Fri, 25 Jul 2025 00:00:00 GMT", 30_000)); // Drift is 10_000 ms
		assertFalse(sender.detectedTimeDrift("geen-date", 30_000)); // Tja, unable to parse...
	}
}
