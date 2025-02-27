package org.frankframework.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.util.MimeType;

public class MessageContextTest {

	@Test
	public void testEmpty() {
		MessageContext context = new MessageContext();
		context.withMimeType("multipart/related=text/xml"); //very invalid
		MimeType mimetype = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		String charset = (String) context.get(MessageContext.METADATA_CHARSET);

		assertNull(mimetype);
		assertNull(charset);
	}

	@Test
	public void testMtomMultipartContentType() {
		MessageContext context = new MessageContext();
		context.withMimeType("multipart/related;boundary=4444ab3725b048db84aa8d60c8db9a76;type=application/xop+xml;start=rootpart@root.part;start-info=text/xml");
		MimeType mimetype = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		String charset = (String) context.get(MessageContext.METADATA_CHARSET);

		assertEquals("multipart/related", mimetype.getType() + "/" + mimetype.getSubtype());
		assertNull(charset);
	}

	@Test
	public void testInvalidFileNameWithQuotes() {
		MessageContext context = new MessageContext();
		context.withMimeType("application/pdf;name=\"581031 23-019523BP Kwitantie bon_new.pdf\"");
		MimeType mimetype = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		String charset = (String) context.get(MessageContext.METADATA_CHARSET);

		assertEquals("application/pdf", mimetype.getType() + "/" + mimetype.getSubtype());
		assertEquals("\"581031 23-019523BP Kwitantie bon_new.pdf\"", mimetype.getParameter("name"));
		assertNull(charset);
	}

	@Test
	public void testInvalidFileNameNoQuotes() {
		MessageContext context = new MessageContext();
		context.withMimeType("application/pdf;name=581031 23-019523BP Kwitantie bon_new.pdf");
		MimeType mimetype = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		String charset = (String) context.get(MessageContext.METADATA_CHARSET);

		assertEquals("application/pdf", mimetype.getType() + "/" + mimetype.getSubtype());
		assertNull(mimetype.getParameter("name"));
		assertNull(charset);
	}

	@Test
	public void testValidMimeTypeWithCharset() {
		MessageContext context = new MessageContext();
		context.withMimeType("text/xml;charset=\"utf-8\";name=\"581031 23-019523BP Kwitantie bon_new.pdf\"");
		MimeType mimetype = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		String charset = (String) context.get(MessageContext.METADATA_CHARSET);

		assertEquals("text/xml", mimetype.getType() + "/" + mimetype.getSubtype());
		assertEquals("\"581031 23-019523BP Kwitantie bon_new.pdf\"", mimetype.getParameter("name"));
		assertEquals("UTF-8", charset);
	}

	@Test
	public void testMimeTypeWithCharsetAndInvalidName() {
		MessageContext context = new MessageContext();
		context.withMimeType("text/xml;charset=\"utf-8\";name=581031 23-019523BP Kwitantie bon_new.pdf");
		MimeType mimetype = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		String charset = (String) context.get(MessageContext.METADATA_CHARSET);

		assertEquals("text/xml", mimetype.getType() + "/" + mimetype.getSubtype());
		assertNull(mimetype.getParameter("name"));
		assertNull(charset);
	}

	@Test // When it cannot parse a parameter, only parses the mimetype and thus also skips the charset.
	public void testValidMimeTypeWithInvalidCharset() {
		MessageContext context = new MessageContext();
		context.withMimeType("text/xml;charset=\"text/xml\";name=\"tralala.xml\"");
		MimeType mimetype = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		String charset = (String) context.get(MessageContext.METADATA_CHARSET);

		assertEquals("text/xml", mimetype.getType() + "/" + mimetype.getSubtype());
		assertNull(mimetype.getParameter("name"));
		assertNull(charset);
	}

	@Test
	public void testPutNullRemovesKey() {
		MessageContext context = new MessageContext();

		context.put("a", "value");

		assertTrue(context.containsKey("a"), "Key 'a' should be in MessageContext but not found");
		assertEquals("value", context.get("a"));

		context.put("a", null);

		assertFalse(context.containsKey("a"), "Key 'a' should have been removed from MessageContext but still found");
	}
}
