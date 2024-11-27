package org.frankframework.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.io.Writer;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.xml.sax.Attributes;

import org.frankframework.documentbuilder.json.JsonWriter;
import org.frankframework.xml.XmlWriter;

public class MessageBuilderTest {

	@Test
	public void testOutputStream() throws Exception {
		MessageBuilder msgBuilder = new MessageBuilder();
		try (OutputStream out = msgBuilder.asOutputStream()) {
			out.write("tralala".getBytes());
		}

		Message result = msgBuilder.build();
		assertEquals("tralala", result.asString());
		assertNull(result.getContext().get(MessageContext.METADATA_MIMETYPE), "should not have a mimetype");
	}

	@Test
	public void testWriter() throws Exception {
		MessageBuilder msgBuilder = new MessageBuilder();
		try (Writer writer = msgBuilder.asWriter()) {
			writer.append("tralala");
		}

		Message result = msgBuilder.build();
		assertEquals("tralala", result.asString());
		assertNull(result.getContext().get(MessageContext.METADATA_MIMETYPE), "should not have a mimetype");
	}

	@Test
	public void testXmlWriter() throws Exception {
		MessageBuilder msgBuilder = new MessageBuilder();
		XmlWriter writer = msgBuilder.asXmlWriter();
		writer.startDocument();
		writer.startElement(null, null, "root", mock(Attributes.class));
		writer.characters("tralala".toCharArray(), 0, 7);
		writer.endElement(null, null, "root");
		writer.endDocument();

		Message result = msgBuilder.build();
		assertEquals("<root>tralala</root>", result.asString());
		assertEquals(MediaType.APPLICATION_XML, result.getContext().get(MessageContext.METADATA_MIMETYPE));
	}

	@Test
	public void testJsonWriter() throws Exception {
		MessageBuilder msgBuilder = new MessageBuilder();
		JsonWriter writer = msgBuilder.asJsonWriter();
		writer.startDocument();
		writer.startObject();
		writer.startObjectEntry("tralala");
		writer.number("123");
		writer.endObject();
		writer.endDocument();

		Message result = msgBuilder.build();
		assertEquals("{\"tralala\":123}", result.asString());
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().get(MessageContext.METADATA_MIMETYPE));
	}
}
