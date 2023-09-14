package nl.nn.adapterframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.soap.SOAPConstants;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;

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
		TextMessage mock = Mockito.mock(TextMessage.class);
		when(mock.getText()).thenReturn(SOAP_MESSAGE);
		when(mock.getPropertyNames()).thenReturn(Collections.emptyEnumeration());

		// Act
		message = jmsFacade.extractMessage(mock, pipeLineSession, true, "key", soapWrapper);
		pipeLineSession.close();

		// Assert
		assertEquals(DEFAULT_BODY, message.asString());
		assertEquals("<info xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">234</info>", pipeLineSession.get("key"));
	}


}
