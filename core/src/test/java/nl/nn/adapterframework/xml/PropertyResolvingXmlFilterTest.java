package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Properties;

import org.junit.Test;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class PropertyResolvingXmlFilterTest {

	@Test
	public void testBasicEntityResolving() throws Exception {
		String input    = TestFileUtils.getTestFile("/Entities/PropertyResolvingXmlFilter/entity-resolving.xml");
		String expected = TestFileUtils.getTestFile("/Entities/PropertyResolvingXmlFilter/entity-resolving-result.xml");
		XmlWriter xmlWriter = new XmlWriter();
		XmlUtils.parseXml(input, xmlWriter);
		TestAssertions.assertEqualsIgnoreCRLF(expected, xmlWriter.toString());
	}

	@Test
	public void testPropertyResolving() throws Exception {
		String input    = TestFileUtils.getTestFile("/Entities/PropertyResolvingXmlFilter/property-resolving.xml");
		String expected = TestFileUtils.getTestFile("/Entities/PropertyResolvingXmlFilter/property-resolving-result.xml");
		URL propsURL = TestFileUtils.getTestFileURL("/Entities/PropertyResolvingXmlFilter/data.properties");
		assertNotNull(propsURL);

		XmlWriter xmlWriter = new XmlWriter();
		Properties properties = new Properties();
		properties.load(propsURL.openStream());

		PropertyResolvingXmlFilter filter = new PropertyResolvingXmlFilter(xmlWriter, properties);
		XmlUtils.parseXml(input, filter);
		TestAssertions.assertEqualsIgnoreCRLF(expected, xmlWriter.toString());
	}
}
