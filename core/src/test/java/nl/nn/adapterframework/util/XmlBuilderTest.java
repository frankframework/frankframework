package nl.nn.adapterframework.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.unboundid.ldap.sdk.LDAPException;

public class XmlBuilderTest {

	@Before
	public void initXMLUnit() throws LDAPException, IOException {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
	}

	@Test
	public void test() throws Exception {
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

		compareXML(sb.toString(), summaryXML.toXML());
	}

	@Test
	public void test2() throws Exception {
		XmlBuilder schema = new XmlBuilder("schema");
		schema.addAttribute("xmlns", "http://www.w3.org/2001/XMLSchema");
		schema.addAttribute("targetNamespace",
				"http://schemas.ibissource.org/Adapter.xsd");
		schema.addAttribute("xmlns:tns",
				"http://schemas.ibissource.org/Adapter.xsd");
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
				"<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://schemas.ibissource.org/Adapter.xsd\" xmlns:tns=\"http://schemas.ibissource.org/Adapter.xsd\" elementFormDefault=\"qualified\">");
		sb.append("<complexType name=\"IOS-AdapteringType\">");
		sb.append("<sequence>");
		sb.append("<element name=\"adapter\" type=\"tns:adapterType\"/>");
		sb.append("</sequence>");
		sb.append("</complexType>");
		sb.append("</schema>");

		compareXML(sb.toString(), schema.toXML());
	}

	@Test
	// test3 equals test2 except that 'addSubElement' is done directly after
	// creating instead of at the end.
	public void test3() throws Exception {
		XmlBuilder schema = new XmlBuilder("schema");
		schema.addAttribute("xmlns", "http://www.w3.org/2001/XMLSchema");
		schema.addAttribute("targetNamespace",
				"http://schemas.ibissource.org/Adapter.xsd");
		schema.addAttribute("xmlns:tns",
				"http://schemas.ibissource.org/Adapter.xsd");
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
				"<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://schemas.ibissource.org/Adapter.xsd\" xmlns:tns=\"http://schemas.ibissource.org/Adapter.xsd\" elementFormDefault=\"qualified\">");
		sb.append("<complexType name=\"IOS-AdapteringType\">");
		sb.append("<sequence>");
		sb.append("<element name=\"adapter\" type=\"tns:adapterType\"/>");
		sb.append("</sequence>");
		sb.append("</complexType>");
		sb.append("</schema>");

		compareXML(sb.toString(), schema.toXML());
	}

	@Test
	public void testAddEmbeddedCdata() throws SAXException, IOException {
		
		String value = "<xml>&amp; <![CDATA[cdatastring < > & <tag/> ]]>rest</xml>";
		
		XmlBuilder root = new XmlBuilder("root");
		root.setValue(value);
		
		String expected = "<root>"+XmlUtils.encodeChars(value)+"</root>";
		compareXML(expected, root.toXML(false));
	}

	private void compareXML(String expected, String actual)
			throws SAXException, IOException {
		// System.out.println(expected);
		// System.out.println(actual);
		Diff diff = XMLUnit.compareXML(expected, actual);
		assertTrue(diff.toString(), diff.identical());
	}
}
