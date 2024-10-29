package org.frankframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import jakarta.jms.BytesMessage;
import jakarta.jms.TextMessage;
import jakarta.xml.soap.SOAPConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mockrunner.mock.jms.MockBytesMessage;
import com.mockrunner.mock.jms.MockTextMessage;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.soap.SoapWrapper;
import org.frankframework.stream.Message;

class JMSFacadeTest {

	public static final String DEFAULT_HEADER_RESULT = "<info xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">234</info>";
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
	void tearDown() {
		message.close();
		jmsFacade.stop();
	}

	@Test
	void testExtractMessageWithHeaders() throws Exception {
		// Arrange
		PipeLineSession pipeLineSession = new PipeLineSession();
		TextMessage textMessage = new MockTextMessage();
		textMessage.setText(SOAP_MESSAGE);

		// Act
		message = jmsFacade.extractMessage(textMessage, pipeLineSession, true, "key", soapWrapper);
		pipeLineSession.close();

		// Assert
		assertEquals(DEFAULT_BODY, message.asString());
		assertEquals(DEFAULT_HEADER_RESULT, pipeLineSession.get("key"));
	}

	@Test
	void testExtractBytesMessage() throws Exception {
		// Arrange
		PipeLineSession pipeLineSession = new PipeLineSession();
		BytesMessage bytesMessage = new MockBytesMessage();
		bytesMessage.writeBytes(SOAP_MESSAGE.getBytes(StandardCharsets.UTF_8));
		bytesMessage.reset();

		// Act
		message = jmsFacade.extractMessage(bytesMessage, pipeLineSession, true, "key", soapWrapper);
		pipeLineSession.close();

		// Assert
		assertEquals(DEFAULT_BODY, message.asString());
		assertEquals(DEFAULT_HEADER_RESULT, pipeLineSession.get("key"));
	}
}
