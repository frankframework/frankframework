package org.frankframework.xml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.XmlUtils;

public class ElementPropertyResolverTest {

	@Test
	public void testBasicEntityResolving() throws Exception {
		String input    = TestFileUtils.getTestFile("/Entities/ElementPropertyResolver/entity-resolving.xml");
		String expected = TestFileUtils.getTestFile("/Entities/ElementPropertyResolver/entity-resolving-result.xml");
		XmlWriter xmlWriter = new XmlWriter();
		XmlUtils.parseXml(input, xmlWriter);
		TestAssertions.assertEqualsIgnoreCRLF(expected, xmlWriter.toString());
	}

	@Test
	public void testPropertyResolving() throws Exception {
		String input    = TestFileUtils.getTestFile("/Entities/ElementPropertyResolver/property-resolving.xml");
		String expected = TestFileUtils.getTestFile("/Entities/ElementPropertyResolver/property-resolving-result.xml");
		URL propsURL = TestFileUtils.getTestFileURL("/Entities/ElementPropertyResolver/data.properties");
		assertNotNull(propsURL);

		Properties properties = new Properties();
		properties.load(propsURL.openStream());

		XmlWriter writer = new XmlWriter();
		ElementPropertyResolver filter = new ElementPropertyResolver(writer, properties);
		XmlUtils.parseXml(input, filter);
		MatchUtils.assertXmlEquals(expected, writer.toString());
	}
}
