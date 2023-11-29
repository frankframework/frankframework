package nl.nn.adapterframework.validation;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLStreamWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.soap.WsdlGeneratorUtils;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.TestScopeProvider;
import nl.nn.adapterframework.validation.xsd.ResourceXsd;

public class XSDTest {

	private final IScopeProvider scopeProvider = new TestScopeProvider();

	@Test
	public void xsdName() throws Exception {
		XSD xsd = new ResourceXsd();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/v1 test.xsd");
		assertEquals("XSDTest/v1 test.xsd", xsd.getResourceTarget());
	}

	@Test
	public void xsdNamespace() throws Exception {
		XSD xsd = new ResourceXsd();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/v1 test.xsd");
		assertEquals("http://test", xsd.getNamespace());
		assertEquals("http://www.ing.com/pim", xsd.getTargetNamespace());
	}

	@Test
	public void xsdUrlEncoded() throws Exception {
		XSD xsd = new ResourceXsd();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/v1%20test.xsd");
		assertEquals("http://test", xsd.getNamespace());
		assertEquals("http://www.ing.com/pim", xsd.getTargetNamespace());
	}

	@Test
	public void writeXSD() throws Exception {
		XSD xsd = new ResourceXsd();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/test.xsd");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter writer = WsdlGeneratorUtils.getWriter(out, false);
		SchemaUtils.xsdToXmlStreamWriter(xsd, writer);

		String result = out.toString();
		String expected = TestFileUtils.getTestFile("/XSDTest/test.xsd");

		MatchUtils.assertXmlEquals("expected xml (XSDTest/test_expected.xsd) not similar to result xml:\n" + out, expected, result);
	}

	@ParameterizedTest
	@CsvSource({ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_OK, ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE})
	public void testAddNamespacesToSchema(String schemaLocation) throws Exception {
		String expectedSchemaLocation = schemaLocation + "-after-adding-namespace.xsd";

		String expectedSchema = TestFileUtils.getTestFile(expectedSchemaLocation.trim().split("\\s+")[1]);

		XSD xsd = getXSD(schemaLocation);
		xsd.setAddNamespaceToSchema(true);

		String actual = xsd.addTargetNamespace();
		MatchUtils.assertXmlEquals(expectedSchema, actual);
	}

	public XSD getXSD(String schemaLocation) throws ConfigurationException {
		String[] split =  schemaLocation.trim().split("\\s+");
		XSD xsd = new ResourceXsd();
		xsd.initNamespace(split[0], scopeProvider, split[1]);
		return xsd;
	}

}
