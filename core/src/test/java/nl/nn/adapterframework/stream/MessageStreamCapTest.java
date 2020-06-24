package nl.nn.adapterframework.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.Writer;

import org.junit.Test;
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
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner,forward)) {
			try (Writer capWriter = cap.asWriter()) {
			
				Object capNative = cap.asNative();
				
				assertEquals(capWriter.getClass(), capNative.getClass());
				assertEquals(capWriter, capNative);
				
				capWriter.write(responseMessage);
			}
			PipeRunResult result = cap.getPipeRunResult();
			
			assertEquals(responseMessage,result.getResult().asString());
		}
	}

	@Test
	public void testBytesCap() throws Exception {
		INamedObject owner = new Owner();
		IForwardTarget forward = null;
		byte[] responseMessage = "fakeResponseMessage".getBytes();
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner,forward)) {
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
	}

	@Test
	/*
	 * if used as a ContentHandler, then the underlying buffer must be a Writer, probably a StringWriter
	 */
	public void testContentHandlerCap() throws Exception {
		INamedObject owner = new Owner();
		IForwardTarget forward = null;
		String expectedResponseMessage = "<root/>";
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner,forward)) {
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
	}

	@Test
	/*
	 * if used as 'native', then the underlying buffer must be a Writer, probably a StringWriter
	 */
	public void testNativeCap() throws Exception {
		INamedObject owner = new Owner();
		IForwardTarget forward = null;
		String responseMessage = "fakeResponseMessage";
		try (MessageOutputStreamCap cap = new MessageOutputStreamCap(owner,forward)) {
			Object capNative = cap.asNative();
			assertTrue(capNative instanceof Writer);

			try (Writer capWriter = (Writer)capNative) {
				capWriter.write(responseMessage);
			}
			PipeRunResult result = cap.getPipeRunResult();
			
			assertEquals(responseMessage,result.getResult().asString());
		}
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
