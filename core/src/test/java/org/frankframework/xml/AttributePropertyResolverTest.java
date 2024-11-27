package org.frankframework.xml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.XmlUtils;

public class AttributePropertyResolverTest {

	@Test
	public void testPropertyResolving() throws Exception {
		String input    = TestFileUtils.getTestFile("/Entities/AttributePropertyResolver/property-resolving.xml");
		String expected = TestFileUtils.getTestFile("/Entities/AttributePropertyResolver/property-resolving-result.xml");
		URL propsURL = TestFileUtils.getTestFileURL("/Entities/AttributePropertyResolver/data.properties");
		assertNotNull(propsURL);

		Properties properties = new Properties();
		properties.load(propsURL.openStream());

		XmlWriter writer = new XmlWriter();
		AttributePropertyResolver filter = new AttributePropertyResolver(writer, properties, null);
		XmlUtils.parseXml(input, filter);
		MatchUtils.assertXmlEquals(expected, writer.toString());
	}
}
