package org.frankframework.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.stream.XMLEventReader;

import org.junit.jupiter.api.Test;

import microsoft.exchange.webservices.data.core.EwsServiceMultiResponseXmlReader;
import microsoft.exchange.webservices.data.core.EwsXmlReader;
import microsoft.exchange.webservices.data.security.XmlNodeType;

public class StaxParserTest {

	private final String validDocument   = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>testContent</test>";
	private final String invalidDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>test&#x3;Content</test>";

	@Test
	public void getProperXMLEventReader() throws Exception {
		InputStream input = new ByteArrayInputStream(validDocument.getBytes("UTF-8"));
		EwsServiceMultiResponseXmlReader impl = EwsServiceMultiResponseXmlReader.create(input, null);
		impl.read(new XmlNodeType(XmlNodeType.START_DOCUMENT));
		impl.read(new XmlNodeType(XmlNodeType.START_ELEMENT));
		XMLEventReader ewsXmlReader = impl.getXmlReaderForNode();
		assertTrue(ewsXmlReader instanceof XMLEventReader);
		impl.read(new XmlNodeType(XmlNodeType.END_DOCUMENT));
	}

	@Test
	public void testReadValidDocument() throws Exception {
		byte[] bytes = validDocument.getBytes("UTF-8");
		EwsXmlReader impl = new EwsXmlReader(new ByteArrayInputStream(bytes));
		impl.read(new XmlNodeType(XmlNodeType.START_DOCUMENT));
		impl.read(new XmlNodeType(XmlNodeType.START_ELEMENT));
		String content = impl.readValue();
		assertEquals(content, "testContent");
		impl.read(new XmlNodeType(XmlNodeType.END_DOCUMENT));
	}

	@Test
	public void testAcceptXml10InvalidCharacters() throws Exception {
		byte[] bytes = invalidDocument.getBytes("UTF-8");
		EwsXmlReader impl = new EwsXmlReader(new ByteArrayInputStream(bytes));
		impl.read(new XmlNodeType(XmlNodeType.START_DOCUMENT));
		impl.read(new XmlNodeType(XmlNodeType.START_ELEMENT));
		String content = impl.readValue();
		assertEquals(content, "test\u0003Content");
		impl.read(new XmlNodeType(XmlNodeType.END_DOCUMENT));
	}
}
