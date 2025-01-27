package org.frankframework.validation;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IScopeProvider;
import org.frankframework.soap.WsdlGeneratorUtils;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.XmlUtils;
import org.frankframework.validation.xsd.ResourceXsd;

public class XSDTest {

	private final IScopeProvider scopeProvider = new TestScopeProvider();

	@Test
	public void xsdName() throws Exception {
		AbstractXSD xsd = new ResourceXsd();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/v1 test.xsd");
		assertEquals("XSDTest/v1 test.xsd", xsd.getResourceTarget());
	}

	@Test
	public void xsdNamespace() throws Exception {
		AbstractXSD xsd = new ResourceXsd();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/v1 test.xsd");
		assertEquals("http://test", xsd.getNamespace());
		assertEquals("http://www.ing.com/pim", xsd.getTargetNamespace());
	}

	@Test
	public void xsdDuplicateNSPrefix() throws Exception {
		// Arrange
		AbstractXSD xsd = new ResourceXsd();
		xsd.initNamespace("http://www.frankframework.org/test", scopeProvider, "XSDTest/MultipleIncludesClashingPrefixes/root1.xsd");

		assertEquals("http://www.frankframework.org/test", xsd.getNamespace());

		Set<IXSD> xsds = AbstractXSD.getXsdsRecursive(Set.of(xsd));

		// Act
		ConfigurationException exception = assertThrows(ConfigurationException.class, () -> SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(scopeProvider, SchemaUtils.groupXsdsByNamespace(xsds, false)));

		assertThat(exception.getMessage(), containsString("Prefix [dup] defined in multiple files with different namespaces"));
	}

	@Test
	public void xsdUrlEncoded() throws Exception {
		AbstractXSD xsd = new ResourceXsd();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/v1%20test.xsd");
		assertEquals("http://test", xsd.getNamespace());
		assertEquals("http://www.ing.com/pim", xsd.getTargetNamespace());
	}

	@Test
	public void writeXSD() throws Exception {
		AbstractXSD xsd = new ResourceXsd();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/test.xsd");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter writer = WsdlGeneratorUtils.getWriter(out, false);
		SchemaUtils.writeStandaloneXsd(xsd, writer);

		String result = out.toString();
		String expected = TestFileUtils.getTestFile("/XSDTest/test.xsd");

		MatchUtils.assertXmlEquals("expected xml (XSDTest/test_expected.xsd) not similar to result xml:\n" + out, expected, result);
	}

	@ParameterizedTest
	@CsvSource({ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_OK, ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE})
	public void testAddNamespacesToSchema(String schemaLocation) throws Exception {
		String expectedSchemaLocation = schemaLocation + "-after-adding-namespace.xsd";

		String expectedSchema = TestFileUtils.getTestFile(expectedSchemaLocation.trim().split("\\s+")[1]);

		AbstractXSD xsd = getXSD(schemaLocation);
		xsd.setAddNamespaceToSchema(true);

		StringWriter writer = new StringWriter();
		XMLStreamWriter w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(writer);
		SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(scopeProvider, Map.of(xsd.getNamespace(), Set.of(xsd)), w);
		w.flush();
		String actual = writer.toString();
		MatchUtils.assertXmlEquals("Schema Merge results do not match expected outcome", expectedSchema, actual, false, true);
	}

	@Test
	public void testMergeXsds() throws Exception {
		// Arrange
		AbstractXSD xsd1 = getXSD("http://www.frankframework.org/test XSDTest/XsdMergingTest/root_schema1.xsd");
		AbstractXSD xsd2 = getXSD("http://www.frankframework.org/test XSDTest/XsdMergingTest/root_schema2.xsd");
		xsd1.setAddNamespaceToSchema(true);

		Set<IXSD> xsds = new LinkedHashSet<>();
		xsds.add(xsd1);
		xsds.add(xsd2);
		Map<String, Set<IXSD>> xsdMap = Map.of(xsd1.getNamespace(), xsds);

		String expectedOutput = TestFileUtils.getTestFile("/XSDTest/XsdMergingTest/merged_xsd_expected_output.xsd");

		// Act
		Set<IXSD> result = SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(scopeProvider, xsdMap);

		// Assert
		IXSD resultXsd = result.iterator().next();
		MatchUtils.assertXmlEquals("Schema merge results do not match expected result", expectedOutput, resultXsd.asString(), false, true);
	}


	public AbstractXSD getXSD(String schemaLocation) throws ConfigurationException {
		String[] split =  schemaLocation.trim().split("\\s+");
		AbstractXSD xsd = new ResourceXsd();
		xsd.initNamespace(split[0], scopeProvider, split[1]);
		return xsd;
	}
}
