package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Objects;

import javax.xml.soap.SOAPConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.xml.XmlWriter;

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
		assertEquals(testOutputFile, handler.toString());
	}

	@Test
	void testRemoveCompactNamespacesDisabled() throws IOException, SAXException {
		// Arrange
		handler.setRemoveCompactMsgNamespaces(false);

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		assertEquals(Objects.requireNonNull(defaultInputMessage.asString()).trim(), handler.toString());
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
		assertEquals(testOutputFile, handler.toString());
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
		assertEquals(testOutputFile, handler.toString());
		assertEquals("DC2023-00020", session.getString("sessionKey"));
		assertEquals("DC2022-012345", session.getString("sessionKey2"));
	}

	@Test
	void testElementToMoveFeatureOnNestedElement() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(32);
		handler.setElementToMove("edcLk01");

		// Act
		XmlUtils.parseXml(defaultInputMessage.asInputSource(), handler);

		// Assert
		assertEquals(Objects.requireNonNull(defaultInputMessage.asString()).trim(), handler.toString());
		assertNull(session.getString("edcLk01"));
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
		assertEquals(testOutputFile, handler.toString());
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
		assertEquals(testOutputFile, handler.toString());
		assertEquals("DC2023-00020", session.getString("ref_identificatie"));
		assertEquals("DC2022-012345", session.getString("ref_identificatie2"));

		// Act 2: retry with already parsed input
		handler = new CompactSaxHandler(new XmlWriter());
		handler.setChompLength(80);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setContext(session);
		handler.setElementToMoveChain("Envelope;Body;edcLk01;object;identificatie");
		XmlUtils.parseXml(testOutputFile, handler);

		// Assert 2: everything should be still the same
		assertEquals(testOutputFile, handler.toString());
		assertEquals("DC2023-00020", session.getString("ref_identificatie"));
		assertEquals("DC2022-012345", session.getString("ref_identificatie2"));
	}

	@Test
	void testElementToMoveBigData() throws IOException, SAXException {
		// Arrange
		handler.setChompLength(200_000);
		handler.setRemoveCompactMsgNamespaces(true);
		handler.setElementToMove("message");
		String expectedOutput = "<event timestamp=\"0\" level=\"DEBUG\">\n" +
				"  <message>{sessionKey:ref_message}</message>\n" +
				"</event>";

		// Act
		Message bigInputMessage = MessageTestUtils.getMessage("/Logging/xml-of-pdf-file.log");
		XmlUtils.parseXml(bigInputMessage.asInputSource(), handler);

		// Assert
		assertEquals(expectedOutput, handler.toString());
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
		assertEquals(testOutputFile, handler.toString());
		assertEquals("DC2023-00020", session.getString("ref_identificatie"));
		assertEquals("DC2022-012345", session.getString("ref_identificatie2"));
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
		String testOutputFile = TestFileUtils.getTestFile("/Util/CompactSaxHandler/output-moved2.xml");
		assertEquals(testOutputFile, handler.toString());
	}

}
