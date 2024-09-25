package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;

import org.frankframework.stream.UrlMessage;
import org.w3c.dom.Node;

import org.frankframework.stream.Message;

public class TestPipeDescriptionProvider {

	@Test
	public void testSchemaLocation() throws Exception {
		PipeDescriptionProvider provider = new PipeDescriptionProvider();
		PipeDescription description = new PipeDescription();

		URL url = TestPipeDescriptionProvider.class.getResource("/testSchemaLocation.xml");
		Message input = new UrlMessage(url);
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		Node root = db.parse(input.asInputSource());
		provider.addResourceNamesToPipeDescription(root.getFirstChild(), description);

		List<String> resourceNames = description.getResourceNames();
		assertEquals(1, resourceNames.size());
		assertTrue(resourceNames.contains("HelloLines/xsd/Lines.xsd"));
	}

	@Test
	public void testWithTwoSchemaLocations() throws Exception {
		PipeDescriptionProvider provider = new PipeDescriptionProvider();
		PipeDescription description = new PipeDescription();

		URL url = TestPipeDescriptionProvider.class.getResource("/testWithTwoSchemaLocations.xml");
		Message input = new UrlMessage(url);
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		Node root = db.parse(input.asInputSource());
		provider.addResourceNamesToPipeDescription(root.getFirstChild(), description);

		List<String> resourceNames = description.getResourceNames();
		assertEquals(2, resourceNames.size());
		assertTrue(resourceNames.contains("HelloLines/xsd/Lines.xsd"));
		assertTrue(resourceNames.contains("HelloLines/xsd/Lines2.xsd"));
	}

	@Test
	public void testFilename() throws Exception {
		PipeDescriptionProvider provider = new PipeDescriptionProvider();
		PipeDescription description = new PipeDescription();

		URL url = TestPipeDescriptionProvider.class.getResource("/testFilename.xml");
		Message input = new UrlMessage(url);
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		Node root = db.parse(input.asInputSource());
		provider.addResourceNamesToPipeDescription(root.getFirstChild(), description);

		List<String> resourceNames = description.getResourceNames();
		assertEquals(1, resourceNames.size());
		assertTrue(resourceNames.contains("HelloLines/xsd/Lines.xsd"));
	}
}
