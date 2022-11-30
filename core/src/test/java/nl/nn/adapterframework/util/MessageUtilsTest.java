package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class MessageUtilsTest {

	@Test
	public void testCharset() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCharacterEncoding("tja"); //overridden by content-type header
		request.setContentType("application/pdf;  charset=utf-8"); //2 spaces
		request.setContent("request".getBytes());
		MessageContext context = MessageUtils.getContext(request);

		assertEquals("UTF-8", context.get(MessageContext.METADATA_CHARSET));
		assertEquals((long) 7, context.get(MessageContext.METADATA_SIZE));
		assertEquals(MimeType.valueOf("application/pdf; charset=UTF-8"), context.get(MessageContext.METADATA_MIMETYPE));
	}

	@Test
	public void testMd5Hash() throws Exception {
		Message message = new Message("test message");
		String hash = MessageUtils.generateMD5Hash(message);
		assertNotNull(hash);
		assertEquals("hash should be the same", MessageUtils.generateMD5Hash(message), hash);
		assertEquals("c72b9698fa1927e1dd12d3cf26ed84b2", hash);
	}

	@Test
	public void testComputeMimeTypeWithISO8559Charset() throws Exception {
		URL url = ClassUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue("Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]", mimeType.toString().contains("text/plain"));
		assertTrue("Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]", mimeType.toString().contains("charset=ISO-8859-1"));
	}

	@Test
	public void testComputeMimeTypeBinaryContent() throws Exception {
		URL url = ClassUtils.getResourceURL("/Logging/pdf-parsed-with-wrong-charset.pdf");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue("Content-Type header ["+mimeType.toString()+"] does not contain [application/pdf]", mimeType.toString().contains("application/pdf"));
		assertNull("Content-Type header ["+mimeType.toString()+"] may not contain a charset", mimeType.getParameter("charset"));
	}

	@Test
	public void testComputeMimeTypeBinaryContentTwice() throws Exception {
		URL url = ClassUtils.getResourceURL("/Logging/pdf-parsed-with-wrong-charset.pdf");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		MessageUtils.computeMimeType(message);
		assertTrue("Content-Type header ["+mimeType.toString()+"] does not contain [application/pdf]", mimeType.toString().contains("application/pdf"));
		assertNull("Content-Type header ["+mimeType.toString()+"] may not contain a charset", mimeType.getParameter("charset"));
	}

	@Test
	public void testComputeMimeTypeWithISO8559CharsetAuto() throws Exception {
		URL url = ClassUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		message.getContext().put(MessageContext.METADATA_CHARSET, "auto");

		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue("Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]", mimeType.toString().contains("text/plain"));
		assertTrue("Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]", mimeType.toString().contains("charset=ISO-8859-1"));
	}

	@Test
	public void testComputeMimeTypeWithISO8559CharsetUTF8() throws Exception {
		URL url = ClassUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		message.getContext().put(MessageContext.METADATA_CHARSET, "utf-8"); //Is wrong, but it's been set, to must be used...

		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue("Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]", mimeType.toString().contains("text/plain"));
		assertTrue("Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]", mimeType.toString().contains("charset=UTF-8"));
	}

	@Test
	public void testMessageDataSource() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/file.xml");
		assertNotNull(url);
		Message message = new UrlMessage(url);
		MessageDataSource ds = new MessageDataSource(message);
		assertEquals("filename should be the same", "file.xml", ds.getName());
		assertEquals("content-type should be the same", "application/xml", ds.getContentType()); //determined from file extension
		assertEquals("contents should be the same", Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()));
		assertEquals("should be able to read the content twice", Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()));
	}

	@Test
	public void testMessageDataSourceFromStringDataWithoutXmlDeclaration() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/file.xml");
		assertNotNull(url);
		Message message = new UrlMessage(url);
		MessageDataSource ds = new MessageDataSource(Message.asMessage(message.asString()));
		assertEquals("filename is unknown", null, ds.getName());
		assertEquals("content-type cannot be determined", "application/octet-stream", ds.getContentType());
		assertEquals("contents should be the same", Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()));
		assertEquals("should be able to read the content twice", Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()));
	}

	@Test
	public void testMessageDataSourceFromStringDataWithXmlDeclaration() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/log4j4ibis.xml");
		assertNotNull(url);
		Message message = new UrlMessage(url);
		MessageDataSource ds = new MessageDataSource(Message.asMessage(message.asString()));
		assertEquals("filename is unknown", null, ds.getName());
		assertEquals("content-type cannot be determined", "application/xml", ds.getContentType());
		assertEquals("contents should be the same", Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()));
		assertEquals("should be able to read the content twice", Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()));
	}
}
