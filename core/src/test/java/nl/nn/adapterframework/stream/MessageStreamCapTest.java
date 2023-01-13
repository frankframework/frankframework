package nl.nn.adapterframework.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeRunResult;

public class MessageStreamCapTest {

	@Test
	public void testStringCap() throws Exception {
		INamedObject owner = new Owner();
		IForwardTarget forward = null;
		String responseMessage = "fakeResponseMessage";
		StringWriter captureWriter = null;
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner,forward)) {
			captureWriter = cap.captureCharacterStream();
			try (Writer capWriter = cap.asWriter()) {
			
				Object capNative = cap.asNative();
				
				assertEquals(capWriter.getClass(), capNative.getClass());
				assertEquals(capWriter, capNative);
				
				capWriter.write(responseMessage);
			}
			PipeRunResult result = cap.getPipeRunResult();
			
			assertEquals(responseMessage,result.getResult().asString());
		}
		assertEquals(responseMessage,captureWriter.toString());
	}

	@Test
	public void testBytesCap() throws Exception {
		INamedObject owner = new Owner();
		IForwardTarget forward = null;
		byte[] responseMessage = "fakeResponseMessage".getBytes();
		StringWriter captureWriter = null;
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner,forward)) {
			captureWriter = cap.captureCharacterStream();
			try (OutputStream capStream = cap.asStream()) {
			
				Object capNative = cap.asNative();
				
				assertEquals(capStream.getClass(), capNative.getClass());
				assertEquals(capStream, capNative);
				
				capStream.write(responseMessage);
			}
			PipeRunResult result = cap.getPipeRunResult();
			
			assertEquals(responseMessage.getClass(),result.getResult().asObject().getClass());
			assertEquals(new String(responseMessage),new String((byte[])result.getResult().asObject()));
		}
		assertEquals(new String(responseMessage),captureWriter.toString());
	}

	@Test
	/*
	 * if used as a ContentHandler, then the underlying buffer must be a Writer, probably a StringWriter
	 */
	public void testContentHandlerCap() throws Exception {
		INamedObject owner = new Owner();
		IForwardTarget forward = null;
		String expectedResponseMessage = "<root/>";
		StringWriter captureWriter = null;
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner,forward)) {
			captureWriter = cap.captureCharacterStream();
			ContentHandler capContentHandler = cap.asContentHandler();
			
			Object capNative = cap.asNative();
			
			assertTrue(capNative instanceof Writer);
			
			capContentHandler.startDocument();
			capContentHandler.startElement("", "root", "root", new AttributesImpl());
			capContentHandler.endElement("", "root", "root");
			capContentHandler.endDocument();

			PipeRunResult result = cap.getPipeRunResult();
			
			assertEquals(expectedResponseMessage,result.getResult().asString());
		}
		assertEquals(expectedResponseMessage,captureWriter.toString());
	}

	@Test
	/*
	 * if used as 'native', then the underlying buffer must be a Writer, probably a StringWriter
	 */
	public void testNativeCap() throws Exception {
		INamedObject owner = new Owner();
		IForwardTarget forward = null;
		String responseMessage = "fakeResponseMessage";
		StringWriter captureWriter = null;
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner,forward)) {
			captureWriter = cap.captureCharacterStream();
			Object capNative = cap.asNative();
			assertTrue(capNative instanceof Writer);

			try (Writer capWriter = (Writer)capNative) {
				capWriter.write(responseMessage);
			}
			PipeRunResult result = cap.getPipeRunResult();
			
			assertEquals(responseMessage,result.getResult().asString());
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
