package nl.nn.adapterframework.soap;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.validation.SchemaUtils;
import nl.nn.adapterframework.validation.XSD;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;

/**
 * TODO It seems that XSD class was refactored, breaking all tests.
 * @author Michiel Meeuwissen
 */
public class XSDTest {

	@Test
	public void xsdName() throws URISyntaxException, XMLStreamException, IOException {
		XSD xsd = new XSD(
                ClassUtils.getResourceURL("v1 test.xsd"),
                "http://test",
                false,
                null,
                true);
		assertEquals("v1 test.xsd", xsd.getName());
	}

	@Test
	public void xsdNamespace() throws URISyntaxException, XMLStreamException, IOException {
        XSD xsd = new XSD(
                ClassUtils.getResourceURL("v1 test.xsd"),
                "http://test",
                false,
                null,
                true);
        assertEquals("http://test",
                xsd.namespace);
        assertEquals("http://www.ing.com/pim",
                xsd.targetNamespace);
	}


	@Test
	public void baseUrlXsd() throws URISyntaxException, IOException, XMLStreamException {
        URL url = ClassUtils.getResourceURL("XSDTest/pim_imported.xsd");
		XSD xsd = new XSD(
				url,
                "http://test",
                false,
                null,
                true);
		assertEquals("XSDTest/", xsd.getBaseUrl());
	}

	@Test
	public void baseUrlXsdWebsphere() throws URISyntaxException, XMLStreamException, IOException {
		XSD xsd = new XSD(
                new URL("file:/data/WAS/6.1/wasap02/appl/Ibis4WUB-010_20111221-1815_wasap02.ear/Ibis4WUB.war/WEB-INF/classes/CalculateQuoteAndPolicyValuesLifeRetail/xsd/Calculation.xsd"),
                "http://test",
                false, null, true);

		assertEquals("CalculateQuoteAndPolicyValuesLifeRetail/xsd/", xsd.getBaseUrl());
	}


	@Test
	public void writeXSD() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, URISyntaxException {
		XSD xsd = new XSD(
                ClassUtils.getResourceURL("XSDTest/test.xsd"),
                "http://test", false, null, true);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter writer = WsdlUtils.getWriter(out, false);
		SchemaUtils.xsdToXmlStreamWriter(xsd, writer, true);

		DocumentBuilder dbuilder = WsdlTest.createDocumentBuilder();
		Document result = dbuilder.parse(new ByteArrayInputStream(out.toByteArray()));
		Document expected = dbuilder.parse(getClass().getClassLoader().getResourceAsStream("XSDTest/test_expected.xsd"));
		System.out.println(new String(out.toByteArray()));
		XMLUnit.setIgnoreWhitespace(false);

		assertXMLEqual("test xml not similar to control xml", expected, result);


	}
    /*
	@Test
	public void writeXSDStripSchemaLocation() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, URISyntaxException {
		XSD xsd = new XSD(
                ClassUtils.getResourceURL("XSDTest/test.xsd"),
                "http://test",


		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter writer = WsdlUtils.getWriter(out, false);
        SchemaUtils.xsdToXmlStreamWriter(xsd, writer, true);
		DocumentBuilder dbuilder = WsdlTest.createDocumentBuilder();
		Document result = dbuilder.parse(new ByteArrayInputStream(out.toByteArray()));
		Document expected = dbuilder.parse(getClass().getClassLoader().getResourceAsStream("XSDTest/test_expected_removed_imported_namespaces.xsd"));
		System.out.println(new String(out.toByteArray()));
		XMLUnit.setIgnoreWhitespace(false);

		assertXMLEqual("test xml not similar to control xml", expected, result);


	}
    /*
	@Test
	public void importedXsds() throws URISyntaxException, XMLStreamException, IOException {
		XSD xsd = new XSD("", "http://test", ClassUtils.getResourceURL("XSDTest/test.xsd").toURI(), 0);
		assertEquals(2, xsd.getImportXSDs(null).size());
		System.out.println("" + xsd.getImportXSDs(null));
	}
*/
}
