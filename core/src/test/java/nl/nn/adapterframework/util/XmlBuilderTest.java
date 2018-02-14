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
		summaryXML.addSubElement(messagesXML);

		StringBuilder sb = new StringBuilder("<summary>");
		sb.append("<adapterState started=\"9\" stopped=\"1&#39;&gt;&amp;quot;\" error=\"3\"/>");
		// sb.append("<adapterState started=\"9\" stopped=\"1'&gt;&amp;quot;\" error=\"3\"/>");
		sb.append("<messages>");
		sb.append("<message>hello</message>");
		sb.append("<message><![CDATA[<xml>world</xml>]]></message>");
		sb.append("<message><xml>world</xml></message>");
		sb.append("<message>&quot;quot&quot; &#39;apos&#39; &gt;gt&gt;</message>");
		// sb.append("<message>\"quot\" 'apos' >gt></message>");
		sb.append("<message>\"quot\" 'apos' >gt></message>");
		sb.append("</messages>");
		sb.append("</summary>");

		compareXML(sb.toString(), summaryXML.toXML());
	}

	private void compareXML(String expected, String actual)
			throws SAXException, IOException {
//		System.out.println(expected);
//		System.out.println(actual);
		Diff diff = XMLUnit.compareXML(expected, actual);
		assertTrue(diff.toString(), diff.identical());
	}
}
