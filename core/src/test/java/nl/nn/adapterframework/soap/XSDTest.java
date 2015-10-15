package nl.nn.adapterframework.soap;

import nl.nn.adapterframework.configuration.ConfigurationException;
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
import org.junit.Ignore;
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
	public void xsdName() throws URISyntaxException, XMLStreamException, IOException, ConfigurationException {
		XSD xsd = new XSD();
		xsd.setResource("v1 test.xsd");
		xsd.setNamespace("http://test");
		xsd.init();
		assertEquals("v1 test.xsd", xsd.getResourceTarget());
	}

	@Test
	public void xsdNamespace() throws URISyntaxException, XMLStreamException, IOException, ConfigurationException {
        XSD xsd = new XSD();
        xsd.setResource("v1 test.xsd");
        xsd.setNamespace("http://test");
        xsd.init();
        assertEquals("http://test",
                xsd.getNamespace());
        assertEquals("http://www.ing.com/pim",
                xsd.getTargetNamespace());
	}

	@Test
    @Ignore("Fails!!")
	public void writeXSD() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, URISyntaxException, ConfigurationException {
		XSD xsd = new XSD();
		xsd.setResource("XSDTest/test.xsd");
		xsd.setNamespace("http://test");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter writer = WsdlUtils.getWriter(out, false);
		SchemaUtils.xsdToXmlStreamWriter(xsd, writer);

		DocumentBuilder dbuilder = WsdlTest.createDocumentBuilder();
		Document result = dbuilder.parse(new ByteArrayInputStream(out.toByteArray()));
		Document expected = dbuilder.parse(getClass().getClassLoader().getResourceAsStream("XSDTest/test_expected.xsd"));
		XMLUnit.setIgnoreWhitespace(false);

		assertXMLEqual("expected xml (XSDTest/test_expected.xsd) not similar to result xml:\n" + new String(out.toByteArray()), expected, result);


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
