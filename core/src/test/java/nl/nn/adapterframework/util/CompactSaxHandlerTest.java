package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Objects;

import javax.xml.soap.SOAPConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

class CompactSaxHandlerTest {

	private static final String DEFAULT_BODY = "<root xmlns=\"urn:fakenamespace\" xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><attrib>1</attrib><attrib>2</attrib></root>";
	private static final String DEFAULT_HEADER = "<soapenv:Header><info>234</info></soapenv:Header>";
	private static final String SOAP_MESSAGE = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\">" + DEFAULT_HEADER + "<soapenv:Body>" + DEFAULT_BODY + "</soapenv:Body></soapenv:Envelope>";

	private CompactSaxHandler handler;
	@BeforeEach
	void setUp() {
		handler = new CompactSaxHandler();
		handler.setRemoveCompactMsgNamespaces(false);
	}

	@Test
	void testBasicXMLParsing() throws IOException, SAXException {
		handler.setContext(new PipeLineSession());

		Message message = Message.asMessage(SOAP_MESSAGE);
		XmlUtils.parseXml(message.asInputSource(), handler);

		assertEquals(SOAP_MESSAGE, handler.getXmlString());
	}

	@Test
	void testRemoveCompactNamespaces() throws IOException, SAXException {
		// Arrange
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setContext(new PipeLineSession());
		Message message = MessageTestUtils.getMessage("/Util/CompactSaxHandler/input.xml");

		// Act
		XmlUtils.parseXml(message.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output.xml");
		assertEquals(testOutputFile, handler.getXmlString());
	}

	@Test
	void testRemoveCompactNamespacesDisabled() throws IOException, SAXException {
		// Arrange
		handler.setRemoveCompactMsgNamespaces(false);
		handler.setContext(new PipeLineSession());
		Message message = MessageTestUtils.getMessage("/Util/CompactSaxHandler/input.xml");

		// Act
		XmlUtils.parseXml(message.asInputSource(), handler);

		// Assert
		assertEquals(Objects.requireNonNull(message.asString()).trim(), handler.getXmlString());
	}
}
