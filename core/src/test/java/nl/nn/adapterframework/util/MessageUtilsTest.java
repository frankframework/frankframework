package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;
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
		assertEquals(MessageUtils.generateMD5Hash(message), hash, "hash should be the same");
		assertEquals("c72b9698fa1927e1dd12d3cf26ed84b2", hash);
	}

	@Test
	public void testComputeMimeTypeWithISO8559Charset() throws Exception {
		URL url = ClassUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("text/plain"), "Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]");
		assertTrue(mimeType.toString().contains("charset=ISO-8859-1"), "Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]");
	}

	@Test
	public void testComputeMimeTypeBinaryContent() throws Exception {
		URL url = ClassUtils.getResourceURL("/Logging/pdf-parsed-with-wrong-charset.pdf");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("application/pdf"), "Content-Type header ["+mimeType.toString()+"] does not contain [application/pdf]");
		assertNull(mimeType.getParameter("charset"), "Content-Type header ["+mimeType.toString()+"] may not contain a charset");
	}

	@Test
	public void testComputeMimeTypeBinaryContentTwice() throws Exception {
		URL url = ClassUtils.getResourceURL("/Logging/pdf-parsed-with-wrong-charset.pdf");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("application/pdf"), "Content-Type header ["+mimeType.toString()+"] does not contain [application/pdf]");
		assertNull(mimeType.getParameter("charset"), "Content-Type header ["+mimeType.toString()+"] may not contain a charset");
	}

	@Test
	public void testComputeMimeTypeWithISO8559CharsetAuto() throws Exception {
		URL url = ClassUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		message.getContext().put(MessageContext.METADATA_CHARSET, "auto");

		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("text/plain"), "Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]");
		assertTrue(mimeType.toString().contains("charset=ISO-8859-1"), "Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]");
	}

	@Test
	public void testComputeMimeTypeWithISO8559CharsetUTF8() throws Exception {
		URL url = ClassUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		message.getContext().put(MessageContext.METADATA_CHARSET, "utf-8"); //Is wrong, but it's been set, to must be used...

		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("text/plain"), "Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]");
		assertTrue(mimeType.toString().contains("charset=UTF-8"), "Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]");
	}

	@Test
	public void testMessageDataSource() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/file.xml");
		assertNotNull(url);
		Message message = new UrlMessage(url);
		MessageDataSource ds = new MessageDataSource(message);
		assertEquals("file.xml", ds.getName(), "filename should be the same");
		assertEquals("application/xml", ds.getContentType(),"content-type should be the same"); //determined from file extension
		assertEquals(Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()), "contents should be the same");
		assertEquals(Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()), "should be able to read the content twice");
	}

	@Test
	public void testMessageDataSourceFromStringDataWithoutXmlDeclaration() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/file.xml");
		assertNotNull(url);
		Message message = new UrlMessage(url);
		MessageDataSource ds = new MessageDataSource(Message.asMessage(message.asString()));
		assertEquals(null, ds.getName(), "filename is unknown");
		assertEquals("application/octet-stream", ds.getContentType(), "content-type cannot be determined");
		assertEquals(Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()), "contents should be the same");
		assertEquals(Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()), "should be able to read the content twice");
	}

	@Test
	public void testMessageDataSourceFromStringDataWithXmlDeclaration() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/log4j4ibis.xml");
		assertNotNull(url);
		Message message = new UrlMessage(url);
		MessageDataSource ds = new MessageDataSource(Message.asMessage(message.asString()));
		assertEquals(null, ds.getName(), "filename is unknown");
		assertEquals(ds.getContentType(),"application/xml", "content-type cannot be determined");
		assertEquals(Misc.streamToString(ds.getInputStream()), Misc.streamToString(url.openStream()), "contents should be the same");
		assertEquals(Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()), "should be able to read the content twice");
	}
}
