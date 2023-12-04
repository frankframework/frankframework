package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.TestScopeProvider;
import nl.nn.adapterframework.xml.StringBuilderContentHandler;
import nl.nn.adapterframework.xml.XmlWriter;

@Log4j2
public class XmlUtilsTest extends FunctionalTransformerPoolTestBase {

	private static final TimeZone CI_TZ = Calendar.getInstance().getTimeZone();
	private static final TimeZone TEST_TZ = TimeZone.getTimeZone("UTC");

	public void testGetRootNamespace(String input, String expected) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeGetRootNamespaceXslt(), input, expected);
		testTransformerPool(XmlUtils.getGetRootNamespaceTransformerPool(), input, expected);
	}

	public void testAddRootNamespace(String namespace, String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeAddRootNamespaceXslt(namespace, omitXmlDeclaration, indent), input, expected);
		testTransformerPool(XmlUtils.getAddRootNamespaceTransformerPool(namespace, omitXmlDeclaration, indent), input, expected);
	}

	public void testChangeRoot(String root, String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeChangeRootXslt(root, omitXmlDeclaration, indent), input, expected);
		testTransformerPool(XmlUtils.getChangeRootTransformerPool(root, omitXmlDeclaration, indent), input, expected);
	}

	public void testRemoveUnusedNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeRemoveUnusedNamespacesXslt(omitXmlDeclaration, indent), input, expected);
		testTransformerPool(XmlUtils.getRemoveUnusedNamespacesTransformerPool(omitXmlDeclaration, indent), input, expected);
	}

	public void testRemoveUnusedNamespacesXslt2(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException {
		testXslt(XmlUtils.makeRemoveUnusedNamespacesXslt2(omitXmlDeclaration, indent),input,expected);
//		testTransformerPool(XmlUtils.getRemoveUnusedNamespacesXslt2TransformerPool(omitXmlDeclaration, indent),input,expected);
	}


	@Test
	public void testGetRootNamespace() throws SAXException, TransformerException, IOException, ConfigurationException {
		testGetRootNamespace("<root><a>a</a><b></b><c/></root>", "");
		testGetRootNamespace("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "xyz");
		testGetRootNamespace("<root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></root>", "");
		testGetRootNamespace("<xx:root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></xx:root>", "xyz");
	}

	@Test
	public void testAddRootNamespace() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator = System.getProperty("line.separator");
		testAddRootNamespace("xyz", "<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>", false, false);
		testAddRootNamespace("xyz", "<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\">" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", false, true);
		testAddRootNamespace("xyz", "<root><a>a</a><b></b><c/></root>", "<root xmlns=\"xyz\"><a>a</a><b/><c/></root>", true, false);
		testAddRootNamespace("xyz", "<root><a>a</a><b></b><c/></root>", "<root xmlns=\"xyz\">" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", true, true);
	}

	@Test
	public void testChangeRoot() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator = System.getProperty("line.separator");
		testChangeRoot("switch", "<root><a>a</a></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><switch><a>a</a></switch>", false, false);
		testChangeRoot("switch", "<root><a>a</a></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><switch>" + lineSeparator + "<a>a</a>" + lineSeparator + "</switch>", false, true);
		testChangeRoot("switch", "<root><a>a</a></root>", "<switch><a>a</a></switch>", true, false);
		testChangeRoot("switch", "<root><a>a</a></root>", "<switch>" + lineSeparator + "<a>a</a>" + lineSeparator + "</switch>", true, true);
	}

	@Test()
	public void testRemoveUnusedNamespaces() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator = System.getProperty("line.separator");
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>", false, false);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", false, true);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>", "<root><a>a</a><b/><c/></root>", true, false);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>", "<root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", true, true);

		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>", false, false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", false, true);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>", "<root><a>a</a><b/><c/></root>", true, false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>", "<root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", true, true);

		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>", false, false);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\">" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", false, true);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "<root xmlns=\"xyz\"><a>a</a><b/><c/></root>", true, false);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "<root xmlns=\"xyz\">" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", true, true);

		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c xmlns=\"xyz\"/></root>", false, false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c xmlns=\"xyz\"/>" + lineSeparator + "</root>", false, true);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>", "<root><a>a</a><b/><c xmlns=\"xyz\"/></root>", true, false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>", "<root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c xmlns=\"xyz\"/>" + lineSeparator + "</root>", true, true);

	}

	@Test
	public void testIdentityTransformWithDefaultEntityResolver() throws Exception { //External EntityResolving is still possible with the XMLEntityResolver
		Resource resource = Resource.getResource(new TestScopeProvider(), "XmlUtils/EntityResolution/in-file-entity-c-temp.xml");
		SAXException thrown = assertThrows(SAXException.class, () -> {
			XmlUtils.parseXml(resource, new XmlWriter());
		});

		String errorMessage = "Cannot get resource for publicId [null] with systemId [file:///c:/temp/test.xml] in scope [URLResource ";
		assertTrue(thrown.getMessage().startsWith(errorMessage), "SaxParseException should start with [Cannot get resource ...] but is [" + thrown.getMessage() + "]");
	}

	@Test
	@Disabled("Saxon 9.6 does not return parameters, transformer.getParameter() is nowhere used in framework code")
	public void testSettingTransformerParameters() throws IOException, TransformerConfigurationException {
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

		assertTrue(transformer.getParameter("stringParamKey") instanceof String);
		assertTrue(transformer.getParameter("byteArrayParamKey") instanceof String);
		assertTrue(transformer.getParameter("baisParamKey") instanceof String);
		assertTrue(transformer.getParameter("readerParamKey") instanceof String);
		assertTrue(transformer.getParameter("messageParamKey") instanceof String);

		assertTrue(transformer.getParameter("integerParamKey") instanceof Integer);
		assertTrue(transformer.getParameter("booleanParamKey") instanceof Boolean);
	}

	@Test
	public void testCanonicalizeWithNewLinesAndSpaces() throws Exception {
		String newLinesAndSpaces = XmlUtils.canonicalize("<test>\n<a>9</a>\n  <b>2</b>  \n<c>7</c>\n</test>\n");
		assertEquals("<test>\n" +
				"	<a>9</a>\n" +
				"	<b>2</b>\n" +
				"	<c>7</c>\n" +
				"</test>", newLinesAndSpaces);
	}

	@Test
	public void testCanonicalizeWithAttributes() throws Exception {
		String attributes = XmlUtils.canonicalize("<test><a a=\"1\"   c=\"3\"	b=\"2\">9</a></test>");
		assertEquals("<test>\n	<a a=\"1\" b=\"2\" c=\"3\">9</a>\n</test>", attributes);
	}

	@Test
	public void testParseXml() throws IOException, SAXException {
		String source = "<root><elem_a>val_a</elem_a><elem_b>val_b</elem_b></root>";
		String expected = "startDocument\n"
				+ "startElement root\n"
				+ "startElement elem_a\n"
				+ "characters [val_a]\n"
				+ "endElement elem_a\n"
				+ "startElement elem_b\n"
				+ "characters [val_b]\n"
				+ "endElement elem_b\n"
				+ "endElement root\n"
				+ "endDocument\n";
		StringBuilderContentHandler handler = new StringBuilderContentHandler();

		XmlUtils.parseXml(source, handler);

		assertEquals(expected, handler.toString());
	}

	@Test
	public void testParseNodeSet() throws IOException, SAXException {
		String source = "<elem_a>val_a</elem_a><elem_b>val_b</elem_b>";
		String expected = "startElement elem_a\n"
				+ "characters [val_a]\n"
				+ "endElement elem_a\n"
				+ "startElement elem_b\n"
				+ "characters [val_b]\n"
				+ "endElement elem_b\n";
		StringBuilderContentHandler handler = new StringBuilderContentHandler();

		XmlUtils.parseNodeSet(source, handler);

		assertEquals(expected, handler.toString());
	}

	@Test
	public void htmlToXhtml() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/HtmlCleaner/html.input.html");
		Message message = new UrlMessage(url);

		String actual = XmlUtils.toXhtml(message);
		String expected = TestFileUtils.getTestFile("/HtmlCleaner/html.output.html");
		MatchUtils.assertXmlEquals(expected, actual);
	}

	@Test
	public void xhtmlToXhtml() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/HtmlCleaner/xhtml.input.xhtml");
		Message message = new UrlMessage(url);

		String actual = XmlUtils.toXhtml(message);
		String expected = TestFileUtils.getTestFile("/HtmlCleaner/xhtml.output.xhtml");
		MatchUtils.assertXmlEquals(expected, actual);
	}

	@Test
	public void noDoctypeToXhtml() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/HtmlCleaner/html.without-doctype.input.html");
		Message message = new UrlMessage(url);

		String actual = XmlUtils.toXhtml(message);
		String expected = TestFileUtils.getTestFile("/HtmlCleaner/html.without-doctype.output.html");
		MatchUtils.assertXmlEquals(expected, actual);
	}

	@Test
	public void noHtmlToXhtml() throws Exception {
		Message message = new Message("<xml>tralalal</xml>");

		String actual = XmlUtils.toXhtml(message);
		assertNull(actual);
	}

	@Test
	public void nullToXhtml() throws Exception {
		Message message = Message.nullMessage();

		String actual = XmlUtils.toXhtml(message);
		assertNull(actual);
	}

	@Test
	public void emptyToXhtml() throws Exception {
		Message message = new Message("");

		String actual = XmlUtils.toXhtml(message);
		assertNull(actual);
	}

	@Test
	public void testConvertEndOfLines() {
		String input = "a\nb\rc\r\nd\r\re\n\nf\r\n\r\ng";
		String expected = "a\nb\nc\nd\n\ne\n\nf\n\ng";

		assertEquals(expected, XmlUtils.convertEndOfLines(input));
		assertEquals(null, XmlUtils.convertEndOfLines(null));
	}

	@Test
	public void testNormalizeWhitespace() {
		String input = "a b  c\td\re\nf\r\n\t\ng";
		String expected = "a b  c d e f    g";

		assertEquals(expected, XmlUtils.normalizeWhitespace(input));
		assertEquals(null, XmlUtils.normalizeWhitespace(null));
	}

	@Test
	public void testnormalizeAttributeValue() {
		String input = "a b  c\td\re\nf\r\n\t\ng";
		String expected = "a b  c d e f   g";

		assertEquals(expected, XmlUtils.normalizeAttributeValue(input));
		assertEquals(null, XmlUtils.normalizeAttributeValue(null));
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
		return Stream.of(Arguments.of("2013-12-10", 1386633600000L),
				Arguments.of("2013-12-10T12:41:43", 1386679303000L),
				Arguments.of("2023-12-09", 1702080000000L),
				Arguments.of("2024-02-29T00:00:00", 1709164800000L),
				Arguments.of("2400-02-29T18:08:05", 13574628485000L)
		);
	}

	@ParameterizedTest
	@MethodSource("xmlDateTimeData")
	public void testParseXmlDateTime(String s, long l) {
		Date date = XmlUtils.parseXmlDateTime(s);
		assertEquals(getCorrectedDate(l), date.getTime());
	}

	@ParameterizedTest
	@ValueSource(strings = {"2002-05-30T09:30:10-06:00", "2002-05-30T09:30:10+06:00", "2002-05-30T09:30:10.5", "2002-05-30T09:00:00"})
	public void shouldReturnDateObjectWhenStringWithDateIsProvided(String s) {
		assertInstanceOf(Date.class, XmlUtils.parseXmlDateTime(s));
	}

	@ParameterizedTest
	@ValueSource(strings = {"2023-13-09T24:08:05", "2023-13-09T18:60:05", "2023-13-09T18:08:60", "2023-13-09T18:08:05", "2023-11-31T18:08:05", "2100-02-29T18:08:05", "2013-12-10 12:41:43"})
	public void shouldThrowErrorWhenStringWithHoursOutOfBoundsIsProvided(String s) {
		assertThrows(IllegalArgumentException.class, () -> XmlUtils.parseXmlDateTime(s));
	}
}
