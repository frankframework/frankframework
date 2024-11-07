package org.frankframework.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.frankframework.core.PipeLineSession;
import org.frankframework.http.AbstractHttpSender.HttpMethod;
import org.frankframework.stream.Message;

public class SharedHttpSenderTest {

	private ArgumentCaptor<HttpContext> context;
	private PipeLineSession session;
	private HttpSender sender;
	private String sessionKey;

	@BeforeEach
	public void setup() throws Exception {
		sender = spy(HttpSender.class);

		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		context = forClass(HttpClientContext.class);

		//Mock all requests
		when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), context.capture())).thenAnswer(new HttpResponseMock());
		sessionKey = PipeLineSession.SYSTEM_MANAGED_RESOURCE_PREFIX + "HttpContext" + httpClient.hashCode();

		when(sender.getHttpClient()).thenReturn(httpClient);
		doNothing().when(sender).start();

		//Some default settings, url will be mocked.
		sender.setUrl("http://127.0.0.1/test");
		sender.setVerifyHostname(false);
		sender.setAllowSelfSignedCertificates(true);
		sender.setMethodType(HttpMethod.GET);

		session = new PipeLineSession();
	}

	@Test
	public void normalSenderShouldRecycleHttpContext() throws Exception {
		// Arrange
		sender.configure();

		// Act
		Message result1 = sender.sendMessageOrThrow(new Message("dummy"), session);

		// Fail-Fast Assert
		assertNotNull(result1);
		assertThat(result1.asString(), Matchers.containsString("GET /test"));

		int contextId1 = context.getValue().hashCode(); //Store value for later

		// Act
		Message result2 = sender.sendMessageOrThrow(new Message("dummy"), session);

		// Fail-Fast Assert
		assertNotNull(result2);
		assertThat(result2.asString(), Matchers.containsString("GET /test"));

		// Test Assertion
		int contextId2 = context.getValue().hashCode();
		assertEquals(contextId1, contextId2, "The same HttpContext should be used"); //The real test!
		assertNull(session.get(sessionKey), "Should not be in the session");
	}

	@Test
	public void sharedSenderShouldStoreContextInSession() throws Exception {
		sender.setSharedResourceRef("dummy");
		sender.configure();

		// Act
		Message result1 = sender.sendMessageOrThrow(new Message("dummy"), session);

		// Fail-Fast Assert
		assertNotNull(result1);
		assertThat(result1.asString(), Matchers.containsString("GET /test"));

		int contextId1 = context.getValue().hashCode(); //Store value for later

		// Act
		Message result2 = sender.sendMessageOrThrow(new Message("dummy"), session);

		// Fail-Fast Assert
		assertNotNull(result2);
		assertThat(result2.asString(), Matchers.containsString("GET /test"));

		// Test Assertion
		int contextId2 = context.getValue().hashCode();
		assertEquals(contextId1, contextId2, "The same HttpContext should be used"); //The real test!
		HttpContext context = (HttpContext) session.get(sessionKey);
		assertNotNull(context, "Should exist in the session");
		assertEquals(this.context.getValue(), context);
	}
}
