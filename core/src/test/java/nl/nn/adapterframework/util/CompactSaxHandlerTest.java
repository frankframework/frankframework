package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Objects;

import javax.xml.soap.SOAPConstants;

import org.junit.jupiter.api.BeforeAll;
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

	private static Message defaultInputMessage;

	private CompactSaxHandler handler;

	@BeforeAll
	static void loadFile() throws IOException {
		defaultInputMessage = MessageTestUtils.getMessage("/Util/CompactSaxHandler/input.xml");
	}

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

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output.xml");
		assertEquals(testOutputFile, handler.getXmlString());
	}

	@Test
	void testRemoveCompactNamespacesDisabled() throws IOException, SAXException {
		// Arrange
		handler.setRemoveCompactMsgNamespaces(false);
		handler.setContext(new PipeLineSession());

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		assertEquals(Objects.requireNonNull(defaultInputMessage.asString()).trim(), handler.getXmlString());
	}

	@Test
	void testElementToMoveFeature() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(32);
		PipeLineSession session = new PipeLineSession();
		handler.setContext(session);
		handler.setElementToMove("mutatiesoort");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-moved.xml");
		assertEquals(testOutputFile, handler.getXmlString());
		assertEquals("T", session.get("ref_mutatiesoort"));
	}

	@Test
	void testElementToMoveFeatureSessionKey() throws IOException, SAXException {
		// Arrange
		handler.setChompCharSize("1KB");
		PipeLineSession session = new PipeLineSession();
		handler.setContext(session);
		handler.setElementToMove("identificatie");
		handler.setElementToMoveSessionKey("sessionKey");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-moved2.xml");
		assertEquals(testOutputFile, handler.getXmlString());
		assertEquals("DC2023-00020", session.get("sessionKey"));
		assertEquals("DC2022-012345", session.get("sessionKey2"));
	}

	@Test
	void testElementChompSizeTooLong() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(8);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setContext(new PipeLineSession());

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-chompsize.xml");
		assertEquals(testOutputFile, handler.getXmlString());
	}

	@Test
	void testElementToMoveChain() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(80);
		handler.setRemoveCompactMsgNamespaces(true);
		PipeLineSession session = new PipeLineSession();
		handler.setContext(session);
		handler.setElementToMoveChain("Envelope;Body;edcLk01;object;identificatie");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-chaintest.xml");
		assertEquals(testOutputFile, handler.getXmlString());
		assertEquals("DC2023-00020", session.get("ref_identificatie"));
		assertEquals("DC2022-012345", session.get("ref_identificatie2"));

		// Act 2: retry with already parsed input
		handler = new CompactSaxHandler();
		handler.setChompLength(80);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setContext(session);
		handler.setElementToMoveChain("Envelope;Body;edcLk01;object;identificatie");
		XmlUtils.parseXml(testOutputFile, handler);

		// Assert 2: everything should be still the same
		assertEquals(testOutputFile, handler.getXmlString());
		assertEquals("DC2023-00020", session.get("ref_identificatie"));
		assertEquals("DC2022-012345", session.get("ref_identificatie2"));
	}

	@Test
	void testElementToMoveBigData() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(200_000);
		handler.setRemoveCompactMsgNamespaces(true);
		PipeLineSession session = new PipeLineSession();
		handler.setContext(session);
		handler.setElementToMove("message");
		String expectedOutput = "<event timestamp=\"0\" level=\"DEBUG\">\n" +
				"  <message>{sessionKey:ref_message}</message>\n" +
				"</event>";

		// Act
		Message bigInputMessage = MessageTestUtils.getMessage("/Logging/xml-of-pdf-file.log");
		XmlUtils.parseXml(bigInputMessage.asInputSource(), handler);

		// Assert
		assertEquals(expectedOutput, handler.getXmlString());
		assertEquals(101_541, ((String) session.get("ref_message")).length());
	}

	@Test
	void testElementToMoveChainOnlyRightLocation() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(80);
		handler.setRemoveCompactMsgNamespaces(true);
		PipeLineSession session = new PipeLineSession();
		handler.setContext(session);
		handler.setElementToMoveChain("Envelope;Body;edcLk01;object;identificatie");

		// Act
		XmlUtils.parseXml(MessageTestUtils.getMessage("/Util/CompactSaxHandler/input-chaintest.xml").asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-chaintest2.xml");
		assertEquals(testOutputFile, handler.getXmlString());
		assertEquals("DC2023-00020", session.get("ref_identificatie"));
		assertEquals("DC2022-012345", session.get("ref_identificatie2"));
	}

}
