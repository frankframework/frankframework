package org.frankframework.util;

import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreWhitespaces;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.Resource;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.xml.StringBuilderContentHandler;
import org.frankframework.xml.XmlWriter;

@Log4j2
public class XmlUtilsTest extends FunctionalTransformerPoolTestBase {

	private static final TimeZone CI_TZ = Calendar.getInstance().getTimeZone();
	private static final TimeZone TEST_TZ = TimeZone.getTimeZone("UTC");

	@Test
	void testIdentityTransformWithDefaultEntityResolver() throws Exception { // External EntityResolving is still possible with the XMLEntityResolver
		Resource resource = Resource.getResource(new TestScopeProvider(), "XmlUtils/EntityResolution/in-file-entity-c-temp.xml");
		SAXException thrown = assertThrows(SAXException.class, () -> {
			XmlUtils.parseXml(resource, new XmlWriter());
		});

		String errorMessage = "Cannot get resource for publicId [null] with systemId [file:///c:/temp/test.xml] in scope [URLResource ";
		assertTrue(thrown.getMessage().startsWith(errorMessage), "SaxParseException should start with [Cannot get resource ...] but is [" + thrown.getMessage() + "]");
	}

	@Test
	@Disabled("Saxon 9.6 does not return parameters, transformer.getParameter() is nowhere used in framework code")
	void testSettingTransformerParameters() throws IOException, TransformerConfigurationException {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("stringParamKey", "stringParamValue");
		parameters.put("byteArrayParamKey", "byteArrayParamValue".getBytes());
		parameters.put("baisParamKey", new ByteArrayInputStream("baisParamValue".getBytes()));
		parameters.put("readerParamKey", new StringReader("readerParamValue"));
		parameters.put("nullParamKey", null);
		parameters.put("messageParamKey", new Message("messageParamValue"));
		parameters.put("integerParamKey", 3);
		parameters.put("booleanParamKey", false);

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		XmlUtils.setTransformerParameters(transformer, parameters);

		assertInstanceOf(String.class, transformer.getParameter("stringParamKey"));
		assertInstanceOf(String.class, transformer.getParameter("byteArrayParamKey"));
		assertInstanceOf(String.class, transformer.getParameter("baisParamKey"));
		assertInstanceOf(String.class, transformer.getParameter("readerParamKey"));
		assertInstanceOf(String.class, transformer.getParameter("messageParamKey"));

		assertInstanceOf(Integer.class, transformer.getParameter("integerParamKey"));
		assertInstanceOf(Boolean.class, transformer.getParameter("booleanParamKey"));
	}

	@Test
	void testCanonicalizeWithNewLinesAndSpaces() throws Exception {
		String newLinesAndSpaces = XmlUtils.canonicalize("<test>\n<a>9</a>\n  <b>2</b>  \n<c>7</c>\n</test>\n");
		assertEquals("""
				<test>
					<a>9</a>
					<b>2</b>
					<c>7</c>
				</test>\
				""", newLinesAndSpaces);
	}

	@Test
	void testCanonicalizeWithAttributes() throws Exception {
		String attributes = XmlUtils.canonicalize("<test><a a=\"1\"   c=\"3\"	b=\"2\">9</a></test>");
		assertEquals("<test>\n	<a a=\"1\" b=\"2\" c=\"3\">9</a>\n</test>", attributes);
	}

	@Test
	void testParseXml() throws IOException, SAXException {
		String source = "<root><elem_a>val_a</elem_a><elem_b>val_b</elem_b></root>";
		String expected = """
				startDocument
				startElement root
				startElement elem_a
				characters [val_a]
				endElement elem_a
				startElement elem_b
				characters [val_b]
				endElement elem_b
				endElement root
				endDocument
				""";
		StringBuilderContentHandler handler = new StringBuilderContentHandler();

		XmlUtils.parseXml(source, handler);

		assertEquals(expected, handler.toString());
	}

	@Test
	void testParseNodeSet() throws IOException, SAXException {
		String source = "<elem_a>val_a</elem_a><elem_b>val_b</elem_b>";
		String expected = """
				startElement elem_a
				characters [val_a]
				endElement elem_a
				startElement elem_b
				characters [val_b]
				endElement elem_b
				""";
		StringBuilderContentHandler handler = new StringBuilderContentHandler();

		XmlUtils.parseNodeSet(source, handler);

		assertEquals(expected, handler.toString());
	}

	@Test
	void htmlToXhtml() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/HtmlCleaner/html.input.html");
		Message message = new UrlMessage(url);

		String actual = XmlUtils.toXhtml(message).asString();
		String expected = TestFileUtils.getTestFile("/HtmlCleaner/html.output.html");
		MatchUtils.assertXmlEquals(expected, actual);
	}

	@Test
	void xhtmlToXhtml() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/HtmlCleaner/xhtml.input.xhtml");
		Message message = new UrlMessage(url);

		String actual = XmlUtils.toXhtml(message).asString();
		String expected = TestFileUtils.getTestFile("/HtmlCleaner/xhtml.output.xhtml");
		MatchUtils.assertXmlEquals(expected, actual);
	}

	@Test
	void noDoctypeToXhtml() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/HtmlCleaner/html.without-doctype.input.html");
		Message message = new UrlMessage(url);

		String actual = XmlUtils.toXhtml(message).asString();
		String expected = TestFileUtils.getTestFile("/HtmlCleaner/html.without-doctype.output.html");
		MatchUtils.assertXmlEquals(expected, actual);
	}

	@Test
	void noHtmlToXhtml() throws Exception {
		Message message = new Message("<xml>tralalal</xml>");

		String actual = XmlUtils.toXhtml(message).asString();
		assertNull(actual);
	}

	@Test
	void nullToXhtml() throws Exception {
		Message message = Message.nullMessage();

		String actual = XmlUtils.toXhtml(message).asString();
		assertNull(actual);
	}

	@Test
	void emptyToXhtml() throws Exception {
		Message message = new Message("");

		String actual = XmlUtils.toXhtml(message).asString();
		assertNull(actual);
	}

	@Test
	void testConvertEndOfLines() {
		String input = "a\nb\rc\r\nd\r\re\n\nf\r\n\r\ng";
		String expected = "a\nb\nc\nd\n\ne\n\nf\n\ng";

		assertEquals(expected, XmlUtils.convertEndOfLines(input));
		assertNull(XmlUtils.convertEndOfLines(null));
	}

	@Test
	void testNormalizeWhitespace() {
		String input = "a b  c\td\re\nf\r\n\t\ng";
		String expected = "a b  c d e f    g";

		assertEquals(expected, XmlUtils.normalizeWhitespace(input));
		assertNull(XmlUtils.normalizeWhitespace(null));
	}

	@Test
	void testNormalizeAttributeValue() {
		String input = "a b  c\td\re\nf\r\n\t\ng";
		String expected = "a b  c d e f   g";

		assertEquals(expected, XmlUtils.normalizeAttributeValue(input));
		assertNull(XmlUtils.normalizeAttributeValue(null));
	}

	private static Date getCorrectedDate(Date date) {
		if (CI_TZ.hasSameRules(TEST_TZ)) {
			return date;
		} else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			int offset = CI_TZ.getOffset(calendar.getTime().getTime());
			calendar.add(Calendar.MILLISECOND, -offset);
			log.info("adjusting date [{}] with offset [{}] to [{}]", () -> date, () -> offset, calendar::getTime);
			return calendar.getTime();
		}
	}

	/**
	 * Tests have been written in UTC, adjust the TimeZone for CI running with a different default TimeZone
	 */
	public static long getCorrectedDate(long l) {
		Date date = new Date(l);
		return getCorrectedDate(date).getTime();
	}

	private static Stream<Arguments> xmlDateTimeData() {
		return Stream.of(
				of("2013-12-10", 1386633600000L),
				of("2013-12-10T12:41:43", 1386679303000L),
				of("2023-12-09", 1702080000000L),
				of("2024-02-29T00:00:00", 1709164800000L),
				of("2400-02-29T18:08:05", 13574628485000L)
		);
	}

	@ParameterizedTest
	@MethodSource("xmlDateTimeData")
	void testParseXmlDateTime(String s, long l) {
		Date date = XmlUtils.parseXmlDateTime(s);
		assertEquals(getCorrectedDate(l), date.getTime());
	}

	@ParameterizedTest
	@ValueSource(strings = {"2002-05-30T09:30:10-06:00", "2002-05-30T09:30:10+06:00", "2002-05-30T09:30:10.5", "2002-05-30T09:00:00"})
	void shouldReturnDateObjectWhenStringWithDateIsProvided(String s) {
		assertInstanceOf(Date.class, XmlUtils.parseXmlDateTime(s));
	}

	@ParameterizedTest
	@ValueSource(strings = {"2023-13-09T24:08:05", "2023-13-09T18:60:05", "2023-13-09T18:08:60", "2023-13-09T18:08:05", "2023-11-31T18:08:05", "2100-02-29T18:08:05", "2013-12-10 12:41:43"})
	void shouldThrowErrorWhenStringWithHoursOutOfBoundsIsProvided(String s) {
		assertThrows(IllegalArgumentException.class, () -> XmlUtils.parseXmlDateTime(s));
	}

	static Stream<Arguments> testReadMessageAsInputSourceWithUnspecifiedNonDefaultCharset() throws IOException, URISyntaxException {
		return MessageTestUtils.readFileInDifferentWays("/Util/MessageUtils/iso-8859-1_without_xml_declaration.xml");
	}
	@ParameterizedTest
	@MethodSource
	void testReadMessageAsInputSourceWithUnspecifiedNonDefaultCharset(Message message) throws Exception {
		// Arrange
		message.getContext().withCharset("ISO-8859-1");

		ContentHandler handler = new XmlWriter();
		XMLReader xmlReader = XmlUtils.getXMLReader(handler);

		// Act
		InputSource source = message.asInputSource();
		xmlReader.parse(source);

		// Assert
		assertTrue(handler.toString().contains("håndværkere"));
		assertTrue(handler.toString().contains("værgeløn"));
	}

	static Stream<Arguments> testReadBOMMessageAsInputSourceWithWrongEncodingSpecifiedExternally() throws IOException, URISyntaxException {
		return MessageTestUtils.readFileInDifferentWays("/Util/MessageUtils/utf8-with-bom.xml");
	}
	@ParameterizedTest
	@MethodSource
	void testReadBOMMessageAsInputSourceWithWrongEncodingSpecifiedExternally(Message message) throws Exception {
		// Arrange
		message.getContext().withCharset("ISO-8859-1");
		assertEquals("ISO-8859-1", message.getCharset());

		ContentHandler handler = new XmlWriter();
		XMLReader xmlReader = XmlUtils.getXMLReader(handler);
		System.err.println(message.asString());

		// Act
		InputSource source = message.asInputSource();
		xmlReader.parse(source);

		// Assert
		assertTrue(handler.toString().contains("testFile with BOM —•˜›"));
	}

	static Stream<Arguments> testReadMessageAsInputSourceWithWrongEncodingSpecifiedExternally() throws IOException, URISyntaxException {
		return MessageTestUtils.readFileInDifferentWays("/Util/MessageUtils/utf8-without-bom.xml");
	}
	@ParameterizedTest
	@MethodSource
	void testReadMessageAsInputSourceWithWrongEncodingSpecifiedExternally(Message message) throws Exception {
		// Arrange
		message.getContext().withCharset("ISO-8859-1");
		assertEquals("ISO-8859-1", message.getCharset());

		ContentHandler handler = new XmlWriter();
		XMLReader xmlReader = XmlUtils.getXMLReader(handler);

		// Act
		InputSource source = message.asInputSource();
		xmlReader.parse(source);

		// Assert
		// NB: This assert breaks, because the parser is not reading the input XML correctly.
		// Shows how the "fix" could be wrong. (But of course, when encoding is specified externally in metadata, and it doesn't match the contents, that is the real bug...)
		assertFalse(handler.toString().contains("testFile with BOM —•˜›"));
	}

	static Stream<Arguments> testReadMessageAsInputSourceWithNonDefaultCharset() throws IOException, URISyntaxException {
		return MessageTestUtils.readFileInDifferentWays("/Util/MessageUtils/iso-8859-1.xml");
	}
	@ParameterizedTest
	@MethodSource
	void testReadMessageAsInputSourceWithNonDefaultCharset(Message message) throws Exception {
		// Arrange
		ContentHandler handler = new XmlWriter();
		XMLReader xmlReader = XmlUtils.getXMLReader(handler);

		// Act
		InputSource source = message.asInputSource();
		xmlReader.parse(source);

		// Assert
		assertTrue(handler.toString().contains("håndværkere"));
		assertTrue(handler.toString().contains("værgeløn"));
	}

	static Stream<Arguments> testRemoveNamespaces() {
		return Stream.of(
				arguments("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?><root xmlns=\"xyz\"/>", true, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>"),
				arguments("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?><root xmlns=\"xyz\"/>", false, "<root/>"),
				arguments("<root xmlns=\"xyz\"/>", true, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>"),
				arguments("<root xmlns=\"xyz\"/>", false, "<root/>"),
				arguments("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?><root/>", true, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>"),
				arguments("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?><root/>", false, "<root/>"),
				arguments("<root/>", true, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>"),
				arguments("<root/>", false, "<root/>")
		);
	}
	@ParameterizedTest
	@MethodSource
	void testRemoveNamespaces(String input, boolean includeXmlDeclaration, String expected) throws XmlException, IOException {
		// Arrange
		Message message = new Message(input);

		// Act
		Message result = XmlUtils.removeNamespaces(message, includeXmlDeclaration);

		// Assert
		assertEqualsIgnoreWhitespaces(expected, result.asString());
	}

	static Stream<Arguments> testRemoveXmlDeclaration() {
		return Stream.of(
				arguments("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?><root/>", "<root/>"),
				arguments("<root/>", "<root/>")
		);
	}
	@ParameterizedTest
	@MethodSource
	void testRemoveXmlDeclaration(String input, String expected) throws XmlException, IOException {
		// Arrange
		Message message = new Message(input);

		// Act
		Message result = XmlUtils.removeXmlDeclaration(message);

		// Assert
		assertEqualsIgnoreWhitespaces(expected, result.asString());
	}
}
