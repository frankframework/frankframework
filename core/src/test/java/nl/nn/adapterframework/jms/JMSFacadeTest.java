package nl.nn.adapterframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import javax.jms.JMSException;
import javax.xml.soap.SOAPConstants;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;

class JMSFacadeTest {

	private JMSFacade jmsFacade;
	private Message message;
	private static SoapWrapper soapWrapper;

	private static final String DEFAULT_BODY = "<root xmlns=\"urn:fakenamespace\" xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><attrib>1</attrib><attrib>2</attrib></root>";
	private static final String DEFAULT_HEADER = "<soapenv:Header><info>234</info></soapenv:Header>";
	private static final String SOAP_MESSAGE = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\">" + DEFAULT_HEADER + "<soapenv:Body>" + DEFAULT_BODY + "</soapenv:Body></soapenv:Envelope>";

	@BeforeAll
	static void setUpOnce() throws ConfigurationException {
		soapWrapper = SoapWrapper.getInstance();
	}

	@BeforeEach
	void setUp() {
		jmsFacade = new JMSFacade();
	}

	@AfterEach
	void tearDown() throws IOException {
		message.close();
		jmsFacade.close();
	}

	@Test
	void testExtractMessageWithHeaders() throws JMSException, IOException, TransformerException, SAXException {
		// Arrange
		PipeLineSession pipeLineSession = new PipeLineSession();
		try (MockedStatic<Message> mockedStaticMessage = Mockito.mockStatic(Message.class)) {
			mockedStaticMessage.when(() -> Message.asMessage(Mockito.any())).thenReturn(new Message(SOAP_MESSAGE, new MessageContext()));

			// Act
			message = jmsFacade.extractMessage(Mockito.mock(javax.jms.Message.class), pipeLineSession, true, "key", soapWrapper);
		}

		// Assert
		assertEquals(DEFAULT_BODY, message.asString());
		assertEquals("<info xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">234</info>", pipeLineSession.get("key"));
	}


}
