package org.frankframework.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import org.frankframework.core.INamedObject;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public class MessageStreamCapTest {

	@Test
	public void testStringCap() throws Exception {
		INamedObject owner = new Owner();
		String responseMessage = "fakeResponseMessage";
		StringWriter captureWriter;
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner)) {
			captureWriter = cap.captureCharacterStream();
			try (Writer capWriter = cap.asWriter()) {

				Object capNative = cap.asNative();

				assertEquals(capWriter.getClass(), capNative.getClass());
				assertEquals(capWriter, capNative);

				capWriter.write(responseMessage);
			}
			Message result = cap.getResponse();

			assertEquals(responseMessage,result.asString());
		}
		assertEquals(responseMessage,captureWriter.toString());
	}

	@Test
	public void testBytesCap() throws Exception {
		INamedObject owner = new Owner();
		byte[] responseMessage = "fakeResponseMessage".getBytes();
		StringWriter captureWriter;
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner)) {
			captureWriter = cap.captureCharacterStream();
			try (OutputStream capStream = cap.asStream()) {

				Object capNative = cap.asNative();

				assertEquals(capStream.getClass(), capNative.getClass());
				assertEquals(capStream, capNative);

				capStream.write(responseMessage);
			}
			Message result = cap.getResponse();

			assertTrue("byte[]".equalsIgnoreCase(result.getRequestClass()));
			assertArrayEquals(responseMessage, result.asByteArray());
		}
		assertEquals(new String(responseMessage),captureWriter.toString());
	}

	@Test
	/*
	 * if used as a ContentHandler, then the underlying buffer must be a Writer, probably a StringWriter
	 */
	public void testContentHandlerCap() throws Exception {
		INamedObject owner = new Owner();
		String expectedResponseMessage = "<root/>";
		StringWriter captureWriter;
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner)) {
			captureWriter = cap.captureCharacterStream();
			ContentHandler capContentHandler = cap.asContentHandler();

			Object capNative = cap.asNative();
      		assertInstanceOf(Writer.class, capNative);

			capContentHandler.startDocument();
			capContentHandler.startElement("", "root", "root", new AttributesImpl());
			capContentHandler.endElement("", "root", "root");
			capContentHandler.endDocument();

			Message result = cap.getResponse();

			assertEquals(expectedResponseMessage,result.asString());
		}
		assertEquals(expectedResponseMessage,captureWriter.toString());
	}

	@Test
	/*
	 * if used as 'native', then the underlying buffer must be a Writer, probably a StringWriter
	 */
	public void testNativeCap() throws Exception {
		INamedObject owner = new Owner();
		String responseMessage = "fakeResponseMessage";
		StringWriter captureWriter = null;
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner)) {
			captureWriter = cap.captureCharacterStream();
			Object capNative = cap.asNative();
			assertTrue(capNative instanceof Writer);

			try (Writer capWriter = (Writer)capNative) {
				capWriter.write(responseMessage);
			}
			Message result = cap.getResponse();

			assertEquals(responseMessage,result.asString());
		}
		assertEquals(responseMessage,captureWriter.toString());
	}

	private class Owner implements INamedObject {
		@Override
		public String getName() {
			return "fakeOwner";
		}

		@Override
		public void setName(String name) {
			// ignore
		}


	}
}
