package nl.nn.adapterframework.extensions.cmis;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
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

import nl.nn.adapterframework.http.HttpResponseMock;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.StreamUtil;

public class CmisHttpInvokerTest {

	private BindingSession session = mock(BindingSession.class);

	@BeforeEach
	public void setup() {
		when(session.get(eq(SessionParameter.USER_AGENT), anyString())).thenReturn("Mockito mock-agent");
	}

	private CmisHttpInvoker createInvoker() {
		return new CmisHttpInvoker() {
			@Override
			protected CmisHttpSender createSender() {
				try {
					CmisHttpSender sender = spy(super.createSender());

					CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

					//Mock all requests
					when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class))).thenAnswer(new HttpResponseMock());
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

	private void assertResponse(String string, Response response) throws IOException {
		String result = StreamUtil.streamToString(response.getStream());
		String expected = TestFileUtils.getTestFile(string);
		assertNotNull("cannot find test file", expected);

		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}

	@Test
	public void testGet() throws Exception {
		CmisHttpInvoker invoker = createInvoker();
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Response response = invoker.invokeGET(url, session);

		assertResponse("/HttpInvokerResponse/simpleGet.txt", response);
	}

	@Test
	public void testPost() throws Exception {
		CmisHttpInvoker invoker = createInvoker();
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Output writer = createOutputFromFile("/HttpInvokerRequest/postMessage.txt");
		Response response = invoker.invokePOST(url, "text/plain", writer, session);

		assertResponse("/HttpInvokerResponse/simplePost.txt", response);
	}

	@Test
	public void testPut() throws Exception {
		CmisHttpInvoker invoker = createInvoker();
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Output writer = createOutputFromFile("/HttpInvokerRequest/putMessage.txt");
		Map<String, String> headers = new HashMap<>();
		headers.put("test-header", "test-value");

		Response response = invoker.invokePUT(url, "text/plain", headers, writer, session);

		assertResponse("/HttpInvokerResponse/simplePut.txt", response);
	}

	@Test
	public void testDelete() throws Exception {
		CmisHttpInvoker invoker = createInvoker();
		UrlBuilder url = new UrlBuilder("https://dummy.url.com");
		Response response = invoker.invokeDELETE(url, session);

		assertResponse("/HttpInvokerResponse/simpleDelete.txt", response);
	}
}
