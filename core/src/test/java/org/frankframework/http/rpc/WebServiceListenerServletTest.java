package org.frankframework.http.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URL;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.frankframework.core.IMessageHandler;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.WebServiceListener;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MatchUtils;

class WebServiceListenerServletTest {
	private static final ServiceDispatcher DISPATCHER = ServiceDispatcher.getInstance();
	private WebServiceListenerServlet servlet;

	@BeforeEach
	public void setup() throws ServletException {
		servlet = new WebServiceListenerServlet();
		servlet.init(mock(ServletConfig.class));
		DISPATCHER.getRegisteredListenerNames().forEach(DISPATCHER::unregisterServiceClient);
	}

	@AfterEach
	public void tearDown() {
		DISPATCHER.getRegisteredListenerNames().forEach(DISPATCHER::unregisterServiceClient);
	}

	private String doPost(String filename, String requestUri) throws Exception {
		return doPost(getFile(filename), requestUri);
	}

	private String doPost(Message content, String requestUri) throws Exception {
		MockHttpServletRequest request = createContextFromRawMessage(content, requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.doPost(request, response);

		return response.getContentAsString();
	}

	@Test
	void doPostNoServiceRegistered() throws Exception {
		String response = doPost("/soapmsg1_1.xml", "services/rpcrouter");

		assertEquals("""
						<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
						<soap:Body><soap:Fault>
						<faultcode>soap:Server</faultcode>
						<faultstring>no service found for uri</faultstring>
						</soap:Fault></soap:Body></soap:Envelope>""", response);
	}

	@Test
	public void testCanFindNamespaceButServiceClientOfWrongType() throws Exception {
		ServiceClient service = mock(ServiceClient.class);
		DISPATCHER.registerServiceClient("http://www.egem.nl/StUF/sector/zkn/0310", service);
		String response = doPost("VrijeBerichten_PipelineRequest.xml", "services/rpcrouter");

		assertEquals("""
						<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
						<soap:Body><soap:Fault>
						<faultcode>soap:Server</faultcode>
						<faultstring>no service found for uri</faultstring>
						</soap:Fault></soap:Body></soap:Envelope>""", response);
	}

	@Test
	void doPostNotSoap() throws Exception {
		String response = doPost(new Message("""
				<xml>
					<not-soap/>
				</xml>
				"""), "services/rpcrouter");

		assertEquals("""
						<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
						<soap:Body><soap:Fault>
						<faultcode>soap:Server</faultcode>
						<faultstring>Could not process SOAP message</faultstring>
						</soap:Fault></soap:Body></soap:Envelope>""", response);
	}

	@Test
	void doPostServiceRegistered() throws Exception {
		Message pipelineResult = getFile("VrijeBerichten_PipelineResult.xml");

		WebServiceListener listener = new WebServiceListener();
		IMessageHandler<Message> handler = mock(IMessageHandler.class);
		listener.setHandler(handler);

		doAnswer(e -> {
			MessageWrapper messageWrapper = e.getArgument(1);

			try {
				MatchUtils.assertXmlEquals(getFile("VrijeBerichten_PipelineRequest.xml").asString(), messageWrapper.getMessage().asString());
				return pipelineResult;
			} catch (IOException ex) {
				fail("unable to read response message: " + ex.getMessage(), ex);
				return null;
			}
		}).when(handler).processRequest(eq(listener), any(MessageWrapper.class), any(PipeLineSession.class));

		listener.setServiceNamespaceURI("http://www.egem.nl/StUF/sector/zkn/0310");
		listener.setSoap(false);
		listener.configure();
		listener.start();

		String response = doPost("VrijeBerichten_PipelineRequest.xml", "services/rpcrouter");

		String expected = getFile("VrijeBerichten_PipelineResult.xml").asString();
		MatchUtils.assertXmlEquals(expected, response);
	}

	private Message getFile(String file) {
		URL url = this.getClass().getResource("/Soap/"+file);
		if (url == null) {
			fail("file not found");
		}
		return new UrlMessage(url);
	}

	private MockHttpServletRequest createContextFromRawMessage(Message rawContent, String requestUri) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", requestUri);
		request.setPathInfo(StringUtils.substringAfter(requestUri, "/"));
		request.setContent(rawContent.asByteArray());
		return request;
	}
}
