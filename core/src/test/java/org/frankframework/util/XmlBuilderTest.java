package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.testutil.MatchUtils;

public class XmlBuilderTest {
	private final String UNICODE_CHARACTERS = " aâΔع你好ಡತ";
	//private final String JAVA_ESCAPED_UNICODE_CHARACTERS = "\u0010 a\u00E2\u0394\u0639\u4F60\u597D\u0CA1\u0CA4";
	private final String XML_RENDERED_UNICODE_CHARACTERS = "¿#16; aâΔع你好ಡತ";

	@BeforeEach
	public void initXMLUnit() {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
	}

	@Test
	public void test() {
		XmlBuilder summaryXML = new XmlBuilder("summary");
		XmlBuilder adapterStateXML = new XmlBuilder("adapterState");
		adapterStateXML.addAttribute("started", 9);
		adapterStateXML.addAttribute("stopped", "1'>&quot;");
		adapterStateXML.addAttribute("error", 3);
		summaryXML.addSubElement(adapterStateXML);
		XmlBuilder messagesXML = new XmlBuilder("messages");
		XmlBuilder messageXML = new XmlBuilder("message");
		messageXML.setValue("hello");
		messagesXML.addSubElement(messageXML);
		messageXML = new XmlBuilder("message");
		messageXML.setCdataValue("<xml>world</xml>");
		messagesXML.addSubElement(messageXML);
		messageXML = new XmlBuilder("message");
		messageXML.setValue("<xml>world</xml>", false);
		messagesXML.addSubElement(messageXML);
		messageXML = new XmlBuilder("message");
		messageXML.setValue("\"quot\" 'apos' >gt>");
		messagesXML.addSubElement(messageXML);
		messageXML = new XmlBuilder("message");
		messageXML.setValue("\"quot\" 'apos' >gt>", false);
		messagesXML.addSubElement(messageXML);
		messageXML = new XmlBuilder("message");
		messageXML.addAttribute("xmlns", "http://nn.nl/XmlBuilder");
		XmlBuilder subMessageXML = new XmlBuilder("subMessage");
		subMessageXML.setValue("hello");
		messageXML.addSubElement(subMessageXML);
		messagesXML.addSubElement(messageXML);
		messageXML = new XmlBuilder("message");
		messageXML.addAttribute("xmlns:xb", "http://nn.nl/XmlBuilder");
		subMessageXML = new XmlBuilder("subMessage");
		subMessageXML.setValue("hello");
		messageXML.addSubElement(subMessageXML);
		messagesXML.addSubElement(messageXML);
		summaryXML.addSubElement(messagesXML);

		StringBuilder sb = new StringBuilder("<summary>");
		sb.append(
				"<adapterState started=\"9\" stopped=\"1&#39;&gt;&amp;quot;\" error=\"3\"/>");
		sb.append("<messages>");
		sb.append("<message>hello</message>");
		sb.append("<message><![CDATA[<xml>world</xml>]]></message>");
		sb.append("<message><xml>world</xml></message>");
		sb.append(
				"<message>&quot;quot&quot; &#39;apos&#39; &gt;gt&gt;</message>");
		sb.append("<message>\"quot\" 'apos' >gt></message>");
		sb.append("<message xmlns=\"http://nn.nl/XmlBuilder\">");
		sb.append("<subMessage>hello</subMessage>");
		sb.append("</message>");
		sb.append("<message xmlns:xb=\"http://nn.nl/XmlBuilder\">");
		sb.append("<subMessage>hello</subMessage>");
		sb.append("</message>");
		sb.append("</messages>");
		sb.append("</summary>");

		MatchUtils.assertXmlEquals(sb.toString(), summaryXML.asXmlString());
	}

	@Test
	public void test2() {
		XmlBuilder schema = new XmlBuilder("schema");
		schema.addAttribute("xmlns", "http://www.w3.org/2001/XMLSchema");
		schema.addAttribute("targetNamespace",
				"http://schemas.frankframework.org/Adapter.xsd");
		schema.addAttribute("xmlns:tns",
				"http://schemas.frankframework.org/Adapter.xsd");
		schema.addAttribute("elementFormDefault", "qualified");
		XmlBuilder complexType = new XmlBuilder("complexType");
		complexType.addAttribute("name", "IOS-AdapteringType");
		XmlBuilder sequence = new XmlBuilder("sequence");
		XmlBuilder element = new XmlBuilder("element");
		element.addAttribute("name", "adapter");
		element.addAttribute("type", "tns:adapterType");
		sequence.addSubElement(element);
		complexType.addSubElement(sequence);
		schema.addSubElement(complexType);

		StringBuilder sb = new StringBuilder(
				"<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://schemas.frankframework.org/Adapter.xsd\" xmlns:tns=\"http://schemas.frankframework.org/Adapter.xsd\" elementFormDefault=\"qualified\">");
		sb.append("<complexType name=\"IOS-AdapteringType\">");
		sb.append("<sequence>");
		sb.append("<element name=\"adapter\" type=\"tns:adapterType\"/>");
		sb.append("</sequence>");
		sb.append("</complexType>");
		sb.append("</schema>");

		MatchUtils.assertXmlEquals(sb.toString(), schema.asXmlString());
	}

	@Test
	// test3 equals test2 except that 'addSubElement' is done directly after
	// creating instead of at the end.
	public void test3() {
		XmlBuilder schema = new XmlBuilder("schema");
		schema.addAttribute("xmlns", "http://www.w3.org/2001/XMLSchema");
		schema.addAttribute("targetNamespace",
				"http://schemas.frankframework.org/Adapter.xsd");
		schema.addAttribute("xmlns:tns",
				"http://schemas.frankframework.org/Adapter.xsd");
		schema.addAttribute("elementFormDefault", "qualified");
		XmlBuilder complexType = new XmlBuilder("complexType");
		schema.addSubElement(complexType);
		complexType.addAttribute("name", "IOS-AdapteringType");
		XmlBuilder sequence = new XmlBuilder("sequence");
		complexType.addSubElement(sequence);
		XmlBuilder element = new XmlBuilder("element");
		sequence.addSubElement(element);
		element.addAttribute("name", "adapter");
		element.addAttribute("type", "tns:adapterType");

		StringBuilder sb = new StringBuilder(
				"<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://schemas.frankframework.org/Adapter.xsd\" xmlns:tns=\"http://schemas.frankframework.org/Adapter.xsd\" elementFormDefault=\"qualified\">");
		sb.append("<complexType name=\"IOS-AdapteringType\">");
		sb.append("<sequence>");
		sb.append("<element name=\"adapter\" type=\"tns:adapterType\"/>");
		sb.append("</sequence>");
		sb.append("</complexType>");
		sb.append("</schema>");

		MatchUtils.assertXmlEquals(sb.toString(), schema.asXmlString());
	}

	@Test
	public void testAddEmbeddedCdata1() {

		String value = "<xml>&amp; <![CDATA[cdatastring < > & <tag/> ]]>rest</xml>";

		XmlBuilder root = new XmlBuilder("root");
		root.setValue(value);

		String expected = "<root>" + XmlEncodingUtils.encodeChars(value) + "</root>";
		MatchUtils.assertXmlEquals(expected, root.asXmlString());
	}

	@Test
	public void testAddEmbeddedCdata2() {
		String CDATA_START="<![CDATA[";
		String CDATA_END="]]>";
		String CDATA_END_REPLACEMENT = CDATA_END.substring(0, 1) + CDATA_END + CDATA_START + CDATA_END.substring(1);

		String value = "<xml>&amp; " + CDATA_START + "cdatastring < > & <tag/> " + CDATA_END + "rest</xml>";

		XmlBuilder root = new XmlBuilder("root");
		root.setCdataValue(value);

		String expected = "<root>" + CDATA_START + value.replace(CDATA_END, CDATA_END_REPLACEMENT) + CDATA_END + "</root>";
		MatchUtils.assertXmlEquals(expected, root.asXmlString());
	}

	@Test
	public void testControlCharacters1() {
		XmlBuilder root = new XmlBuilder("root");
		XmlBuilder subElement = new XmlBuilder("element");
		subElement.setValue("control char 1a [\u001a]");
		root.addSubElement(subElement);

		String expected = "<root><element>control char 1a [¿#26;]</element></root>";

		MatchUtils.assertXmlEquals(expected, root.asXmlString());
	}

	@Test
	public void testControlCharacters2() {
		XmlBuilder root = new XmlBuilder("root");
		XmlBuilder subElement = new XmlBuilder("element");
		subElement.setValue("control char 1a [¿#26;]");
		root.addSubElement(subElement);

		String expected = "<root><element>control char 1a [¿#26;]</element></root>";

		MatchUtils.assertXmlEquals(expected, root.asXmlString());
	}

	@Test
	public void testPrettyPrint() {
		XmlBuilder root = new XmlBuilder("root");
		root.addSubElement("elementName", "elementValue");
		String expected = "<root>\n\t<elementName>elementValue</elementName>\n</root>";
		String actual = root.asXmlString().trim().replace("  ", "\t").replaceAll(System.lineSeparator(), "\n");
		assertEquals(expected, actual);
	}

	@Test
	public void testUnicodeMessage() {
		XmlBuilder root = new XmlBuilder("root");
		XmlBuilder subElement = new XmlBuilder("element");
		subElement.setValue("Unicode characters [" + UNICODE_CHARACTERS + "]");
		root.addSubElement(subElement);

		String expected = "<root><element>Unicode characters [" + XML_RENDERED_UNICODE_CHARACTERS + "]</element></root>";

		MatchUtils.assertXmlEquals(expected, root.asXmlString());
	}

	@Test
	public void testAttributeNormalization() {
		XmlBuilder root = new XmlBuilder("root");
		root.addAttribute("attr1", "a b  c\td\re\nf\r\n\t\ng");
		String expected = "<root attr1=\"a b  c d e f   g\" />";

		MatchUtils.assertXmlEquals(expected, root.asXmlString());
	}
}
