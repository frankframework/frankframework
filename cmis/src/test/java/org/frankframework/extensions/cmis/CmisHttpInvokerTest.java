package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Output;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Response;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.impl.UrlBuilder;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.http.HttpResponseMock;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.StreamUtil;

public class CmisHttpInvokerTest {

	private final BindingSession session = mock(BindingSession.class);

	@BeforeEach
	public void setup() {
		when(session.get(eq(SessionParameter.USER_AGENT), anyString())).thenReturn("Mockito mock-agent");
	}

	private CmisHttpInvoker createInvoker(int statusCode) {
		return new CmisHttpInvoker() {
			@Override
			protected CmisHttpSender createSender() {
				try {
					CmisHttpSender sender = spy(super.createSender());

					CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

					//Mock all requests
					when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class))).thenAnswer(new HttpResponseMock(statusCode));
					when(sender.getHttpClient()).thenReturn(httpClient);

					return sender;
				} catch (Throwable t) {
					t.printStackTrace();
					fail(t.getMessage());
					throw new IllegalStateException(t);
				}
			}
		};
	}

	private Output createOutputFromFile(String file) throws IOException {
		URL url = TestFileUtils.getTestFileURL(file);
		assertNotNull(url, "unable to find test file");

		return new Output() {
			@Override
			public void write(OutputStream out) throws Exception {
				StreamUtil.streamToStream(url.openStream(), out);
			}
		};
	}

	private void assertResponse(String string, InputStream response) throws IOException {
		assertResponse(string, StreamUtil.streamToString(response));
	}
	private void assertResponse(String string, String result) throws IOException {
		String expected = TestFileUtils.getTestFile(string);
		assertNotNull(expected, "cannot find test file");

		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}

	@Test
	public void testGet() throws Exception {
		CmisHttpInvoker invoker = createInvoker(200);
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Response response = invoker.invokeGET(url, session);

		assertNull(response.getErrorContent());
		assertNotNull(response.getStream());
		assertEquals(200, response.getResponseCode());
		assertResponse("/HttpInvokerResponse/simpleGet.txt", response.getStream());
	}

	@Test
	public void testPost() throws Exception {
		CmisHttpInvoker invoker = createInvoker(200);
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Output writer = createOutputFromFile("/HttpInvokerRequest/postMessage.txt");
		Response response = invoker.invokePOST(url, "text/plain", writer, session);

		assertNull(response.getErrorContent());
		assertNotNull(response.getStream());
		assertEquals(200, response.getResponseCode());
		assertResponse("/HttpInvokerResponse/simplePost.txt", response.getStream());
	}

	@Test
	public void testPut() throws Exception {
		CmisHttpInvoker invoker = createInvoker(200);
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Output writer = createOutputFromFile("/HttpInvokerRequest/putMessage.txt");
		Map<String, String> headers = new HashMap<>();
		headers.put("test-header", "test-value");

		Response response = invoker.invokePUT(url, "text/plain", headers, writer, session);

		assertNull(response.getErrorContent());
		assertNotNull(response.getStream());
		assertEquals(200, response.getResponseCode());
		assertResponse("/HttpInvokerResponse/simplePut.txt", response.getStream());
	}

	@Test
	public void testDelete() throws Exception {
		CmisHttpInvoker invoker = createInvoker(200);
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Response response = invoker.invokeDELETE(url, session);

		assertNull(response.getErrorContent());
		assertNotNull(response.getStream());
		assertEquals(200, response.getResponseCode());
		assertResponse("/HttpInvokerResponse/simpleDelete.txt", response.getStream());
	}

	@Test
	public void testException() throws Exception {
		CmisHttpInvoker invoker = createInvoker(400);
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Response response = invoker.invokeGET(url, session);
		assertNotNull(response.getErrorContent());
		assertNull(response.getStream());
		assertEquals(400, response.getResponseCode());
		assertTrue(response.getErrorContent().contains("HOST dummy.url.com"));
		assertResponse("/HttpInvokerResponse/simpleGet.txt", response.getErrorContent());
	}
}
