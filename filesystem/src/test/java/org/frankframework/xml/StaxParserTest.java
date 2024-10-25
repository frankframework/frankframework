package org.frankframework.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLEventReader;

import org.junit.jupiter.api.Test;

import microsoft.exchange.webservices.data.core.EwsServiceMultiResponseXmlReader;
import microsoft.exchange.webservices.data.core.EwsXmlReader;
import microsoft.exchange.webservices.data.security.XmlNodeType;

/**
 * Tests if XML 1.1 has been enabled, to avoid errors like:
 * Illegal character entity: expansion character (code 0x3 at [row,col {unknown-source}]: [1,53]
 * 
 * Required for MS Exchange.
 * See {@link StaxParserFactory}.
 */
public class StaxParserTest {

	private final String validDocument   = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>testContent</test>";
	private final String invalidDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>test&#x3;Content</test>";

	@Test
	public void getProperXMLEventReader() throws Exception {
		InputStream input = new ByteArrayInputStream(validDocument.getBytes(StandardCharsets.UTF_8));
		EwsServiceMultiResponseXmlReader impl = EwsServiceMultiResponseXmlReader.create(input, null);
		impl.read(new XmlNodeType(XmlNodeType.START_DOCUMENT));
		impl.read(new XmlNodeType(XmlNodeType.START_ELEMENT));
		XMLEventReader ewsXmlReader = impl.getXmlReaderForNode();
		assertInstanceOf(XMLEventReader.class, ewsXmlReader);
		impl.read(new XmlNodeType(XmlNodeType.END_DOCUMENT));
	}

	@Test
	public void testReadValidDocument() throws Exception {
		byte[] bytes = validDocument.getBytes(StandardCharsets.UTF_8);
		EwsXmlReader impl = new EwsXmlReader(new ByteArrayInputStream(bytes));
		impl.read(new XmlNodeType(XmlNodeType.START_DOCUMENT));
		impl.read(new XmlNodeType(XmlNodeType.START_ELEMENT));
		String content = impl.readValue();
		assertEquals("testContent", content);
		impl.read(new XmlNodeType(XmlNodeType.END_DOCUMENT));
	}

	@Test
	public void testAcceptXml10InvalidCharacters() throws Exception {
		byte[] bytes = invalidDocument.getBytes(StandardCharsets.UTF_8);
		EwsXmlReader impl = new EwsXmlReader(new ByteArrayInputStream(bytes));
		impl.read(new XmlNodeType(XmlNodeType.START_DOCUMENT));
		impl.read(new XmlNodeType(XmlNodeType.START_ELEMENT));
		String content = impl.readValue();
		assertEquals("test\u0003Content", content);
		impl.read(new XmlNodeType(XmlNodeType.END_DOCUMENT));
	}
}
