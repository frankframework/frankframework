package org.frankframework.http.cxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Source;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.http.SoapActionServiceListener;
import org.frankframework.http.WebServiceListener;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.util.XmlUtils;

/**
 * We already test the main functionality in {@link SoapProviderTest}.
 * This is to test the {@link ServiceDispatcher}
 */
public class NamespaceUriProviderTest {
	private NamespaceUriProvider provider;
	private WebServiceContext webServiceContext = new WebServiceContextStub();
	private ServiceDispatcher dispatcher = ServiceDispatcher.getInstance();

	@BeforeEach
	public void setup() {
		provider = new NamespaceUriProvider();
		provider.webServiceContext = webServiceContext;

		dispatcher.getRegisteredListenerNames().forEach(dispatcher::unregisterServiceClient);
	}

	private Message getFile(String file) throws IOException {
		URL url = this.getClass().getResource("/Soap/"+file);
		if (url == null) {
			throw new IOException("file not found");
		}
		return new UrlMessage(url);
	}

	private SOAPMessage createMessage(String filename, boolean isSoap11) throws Exception {
		MessageFactory factory = MessageFactory.newInstance(isSoap11 ? SOAPConstants.SOAP_1_1_PROTOCOL : SOAPConstants.SOAP_1_2_PROTOCOL);
		SOAPMessage soapMessage = factory.createMessage();
		Source streamSource = getFile(filename).asSource();
		soapMessage.getSOAPPart().setContent(streamSource);
		return soapMessage;
	}

	@Test
	public void testCannotFindNamespaceAndCallServiceClient() throws Exception {
		SOAPMessage request = createMessage("VrijeBerichten_PipelineRequest.xml", true);
		WebServiceException e = assertThrows(WebServiceException.class, () -> provider.invoke(request));
		assertEquals("Could not process SOAP message: service [http://www.egem.nl/StUF/sector/zkn/0310] is not registered or not of required type", e.getMessage());
	}

	@Test
	public void testCanFindNamespaceButServiceClientOfWrongType() throws Exception {
		ServiceClient service = mock(ServiceClient.class);
		dispatcher.registerServiceClient("http://www.egem.nl/StUF/sector/zkn/0310", service);

		SOAPMessage request = createMessage("VrijeBerichten_PipelineRequest.xml", true);

		WebServiceException e = assertThrows(WebServiceException.class, () -> provider.invoke(request));
		assertEquals("Could not process SOAP message: service [http://www.egem.nl/StUF/sector/zkn/0310] is not registered or not of required type", e.getMessage());
	}

	@Test
	public void testFindNamespaceAndCallServiceClient() throws Exception {
		Message pipelineResult;
		try (Message plr = getFile("VrijeBerichten_PipelineResult.xml")) {
			pipelineResult = spy(new Message(new FilterInputStream(plr.asInputStream()) {}));
		}

		WebServiceListener listener = mock(WebServiceListener.class);
		doAnswer(e -> {
			Message message = e.getArgument(0);

			try {
				MatchUtils.assertXmlEquals(getFile("VrijeBerichten_PipelineRequest.xml").asString(), message.asString());
				return pipelineResult;
			} catch (IOException ex) {
				fail("unable to read response message: " + ex.getMessage(), ex);
				return null;
			}
		}).when(listener).processRequest(any(Message.class), any(PipeLineSession.class));

		dispatcher.registerServiceClient("http://www.egem.nl/StUF/sector/zkn/0310", listener);

		SOAPMessage request = createMessage("VrijeBerichten_PipelineRequest.xml", true);
		SOAPMessage message = provider.invoke(request);

		verify(pipelineResult, times(0)).close();

		String result = XmlUtils.nodeToString(message.getSOAPPart());
		String expected = getFile("VrijeBerichten_PipelineResult.xml").asString();
		MatchUtils.assertXmlEquals(expected, result);
	}

	@Test
	public void testFindSoap11ActionAndCallServiceClient() throws Exception {
		Message pipelineResult;
		try (Message plr = getFile("soapmsg1_1.xml")) {
			pipelineResult = spy(new Message(new FilterInputStream(plr.asInputStream()) {}));
		}

		SoapActionServiceListener listener = mock(SoapActionServiceListener.class);
		doAnswer(e -> {
			Message message = e.getArgument(0);

			try {
				MatchUtils.assertXmlEquals(getFile("soapmsg1_1.xml").asString(), message.asString());
				return pipelineResult;
			} catch (IOException ex) {
				fail("unable to read response message: " + ex.getMessage(), ex);
				return null;
			}
		}).when(listener).processRequest(any(Message.class), any(PipeLineSession.class));

		dispatcher.registerServiceClient("http://www.egem.nl/StUF/sector/zkn/0310", listener);

		SOAPMessage request = createMessage("soapmsg1_1.xml", true);
		webServiceContext.getMessageContext().put(SoapBindingConstants.SOAP_ACTION, "http://www.egem.nl/StUF/sector/zkn/0310");
		webServiceContext.getMessageContext().put("Content-Type", "text/xml; action=\"ignored\"");
		SOAPMessage message = provider.invoke(request);

		verify(pipelineResult, times(0)).close();

		String result = XmlUtils.nodeToString(message.getSOAPPart());
		String expected = getFile("soapmsg1_1.xml").asString();
		MatchUtils.assertXmlEquals(expected, result);
	}

	@Test
	public void testFindSoap12ActionAndCallServiceClient() throws Exception {
		Message pipelineResult;
		try (Message plr = getFile("soapmsg1_2.xml")) {
			pipelineResult = spy(new Message(new FilterInputStream(plr.asInputStream()) {}));
		}

		SoapActionServiceListener listener = mock(SoapActionServiceListener.class);
		doAnswer(e -> {
			Message message = e.getArgument(0);

			try {
				MatchUtils.assertXmlEquals(getFile("soapmsg1_2.xml").asString(), message.asString());
				return pipelineResult;
			} catch (IOException ex) {
				fail("unable to read response message: " + ex.getMessage(), ex);
				return null;
			}
		}).when(listener).processRequest(any(Message.class), any(PipeLineSession.class));

		dispatcher.registerServiceClient("http://www.egem.nl/StUF/sector/zkn/0310", listener);

		SOAPMessage request = createMessage("soapmsg1_2.xml", false);
		webServiceContext.getMessageContext().put(SoapBindingConstants.SOAP_ACTION, "ingored");
		webServiceContext.getMessageContext().put("Content-Type",
				"application/soap+xml;charset=UTF-8;action=\"http://www.egem.nl/StUF/sector/zkn/0310\"");
		SOAPMessage message = provider.invoke(request);

		verify(pipelineResult, times(0)).close();

		String result = XmlUtils.nodeToString(message.getSOAPPart());
		String expected = getFile("soapmsg1_2.xml").asString();
		MatchUtils.assertXmlEquals(expected, result);
	}
}
