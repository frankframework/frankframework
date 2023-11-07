package nl.nn.adapterframework.xml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;

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
