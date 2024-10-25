package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Objects;

import jakarta.xml.soap.SOAPConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXException;

import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.xml.XmlWriter;

class CompactSaxHandlerTest {

	private static final String DEFAULT_BODY = "<root xmlns=\"urn:fakenamespace\"><attrib>1</attrib><attrib>2</attrib></root>\n";
	private static final String DEFAULT_HEADER = "<soapenv:Header><info>234</info></soapenv:Header>\n";
	private static final String SOAP_MESSAGE = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\">" + DEFAULT_HEADER + "<soapenv:Body>" + DEFAULT_BODY + "</soapenv:Body></soapenv:Envelope>";
	public PipeLineSession session;

	private static Message defaultInputMessage;

	private CompactSaxHandler handler;
	private final XmlWriter xmlWriter = new XmlWriter();

	@BeforeAll
	static void loadFile() throws IOException {
		defaultInputMessage = MessageTestUtils.getMessage("/Util/CompactSaxHandler/input.xml");
	}

	@BeforeEach
	void setUp() {
		handler = new CompactSaxHandler(xmlWriter);
		handler.setRemoveCompactMsgNamespaces(false);
		session = new PipeLineSession();
		handler.setContext(session);
	}

	@AfterEach
	void tearDown() {
		session.close();
	}

	@Test
	void testBasicXMLParsing() throws IOException, SAXException {
		XmlUtils.parseXml(SOAP_MESSAGE, handler);

		assertEquals(SOAP_MESSAGE, xmlWriter.toString());
	}

	@Test
	void testRemoveCompactNamespaces() throws IOException, SAXException {
		// Arrange
		handler.setRemoveCompactMsgNamespaces(true);

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output.xml");
		assertEquals(testOutputFile, xmlWriter.toString());
	}

	@Test
	void testRemoveCompactNamespacesDisabled() throws IOException, SAXException {
		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		assertEquals(Objects.requireNonNull(defaultInputMessage.asString()).trim(), xmlWriter.toString());
	}

	@Test
	void testElementToMoveFeature() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(32);
		handler.setElementToMove("mutatiesoort");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-moved.xml");
		assertEquals(testOutputFile, xmlWriter.toString());
		assertEquals("T", session.getString("ref_mutatiesoort"));
	}

	@Test
	void testElementToMoveFeatureSessionKey() throws IOException, SAXException {
		// Arrange
		handler.setChompCharSize("1KB");
		handler.setElementToMove("identificatie");
		handler.setElementToMoveSessionKey("sessionKey");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-moved2.xml");
		assertEquals("DC2023-00020", session.getString("sessionKey"));
		assertEquals("DC2022-012345", session.getString("sessionKey2"));
		assertEquals(testOutputFile, xmlWriter.toString());
	}

	@Test
	void testElementToMoveFeatureOnNestedElement() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(32);
		handler.setElementToMove("edcLk01");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		assertNull(session.getString("edcLk01"));
		assertEquals(Objects.requireNonNull(defaultInputMessage.asString()).trim(), xmlWriter.toString());
	}

	@Test
	void testElementChompSizeTooLong() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(8);
		handler.setRemoveCompactMsgNamespaces(true);

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-chompsize.xml");
		assertEquals(testOutputFile, xmlWriter.toString());
	}

	@Test
	void testElementToMoveChain() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(80);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setElementToMoveChain("Envelope;Body;edcLk01;object;identificatie");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-chaintest.xml");
		assertEquals("DC2023-00020", session.getString("ref_identificatie"));
		assertEquals("DC2022-012345", session.getString("ref_identificatie2"));
		assertEquals(testOutputFile, xmlWriter.toString());

		// Act 2: retry with already parsed input
		handler = new CompactSaxHandler(new XmlWriter());
		handler.setChompLength(80);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setContext(session);
		handler.setElementToMoveChain("Envelope;Body;edcLk01;object;identificatie");
		XmlUtils.parseXml(testOutputFile, handler);

		// Assert 2: everything should be still the same
		assertEquals("DC2023-00020", session.getString("ref_identificatie"));
		assertEquals("DC2022-012345", session.getString("ref_identificatie2"));
		assertEquals(testOutputFile, xmlWriter.toString());
	}

	@Test
	void testElementToMoveChainNotChomped() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(10);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setElementToMoveChain("Envelope;Body;edcLk01;stuurgegevens;tijdstipBericht");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-chainchomptest.xml");
		assertEquals("20230825095316895", session.getString("ref_tijdstipBericht"));
		assertEquals(testOutputFile, xmlWriter.toString());
	}

	@Test
	void testElementToMoveBigData() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(200_000);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setElementToMove("message");
		String expectedOutput = """
				<event timestamp="0" level="DEBUG">
				  <message>{sessionKey:ref_message}</message>
				</event>\
				""";

		// Act
		Message bigInputMessage = MessageTestUtils.getMessage("/Logging/xml-of-pdf-file.log");
		XmlUtils.parseXml(bigInputMessage.asInputSource(), handler);

		// Assert
		assertEquals(expectedOutput, xmlWriter.toString());
		assertEquals(101_541, Objects.requireNonNull(session.getString("ref_message")).length());
	}

	@Test
	void testElementToMoveChainOnlyRightLocation() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(80);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setElementToMoveChain("Envelope;Body;edcLk01;object;identificatie");

		// Act
		XmlUtils.parseXml(MessageTestUtils.getMessage("/Util/CompactSaxHandler/input-chaintest.xml").asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-chaintest2.xml");
		assertEquals("DC2023-00020", session.getString("ref_identificatie"));
		assertEquals("DC2022-012345", session.getString("ref_identificatie2"));
		assertEquals(testOutputFile, xmlWriter.toString());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/Util/CompactSaxHandler/input-invalid-moved-ref-1.xml",
			"/Util/CompactSaxHandler/input-invalid-moved-ref-2.xml",
			"/Util/CompactSaxHandler/input-invalid-moved-ref-3.xml",
			"/Util/CompactSaxHandler/input-invalid-moved-ref-4.xml",
			"/Util/CompactSaxHandler/input-invalid-moved-ref-5.xml",
			"/Util/CompactSaxHandler/input-invalid-moved-ref-6.xml"
	})
	void testElementToMoveEdgeCases(String inputUri) throws IOException, SAXException {
		// Arrange
		handler.setRemoveCompactMsgNamespaces(false);
		handler.setElementToMove("identificatie");
		handler.setElementToMoveSessionKey("sessionKey");

		// Act
		XmlUtils.parseXml(MessageTestUtils.getMessage(inputUri).asInputSource(), handler);

		// Assert
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-moved3.xml");
		assertEquals(2, session.size());
		assertEquals(testOutputFile, xmlWriter.toString());
	}
}
