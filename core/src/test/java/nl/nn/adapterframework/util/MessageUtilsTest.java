package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
	}

	@Test
	public void testMessageDataSource() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/file.xml");
		assertNotNull(url);
		Message message = new UrlMessage(url);
		MessageDataSource ds = new MessageDataSource(message);
		assertEquals("filename should be the same", "file.xml", ds.getName());
		assertEquals("content-type should be the same", "application/xml", ds.getContentType());
		assertEquals("contents should be the same", Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()));
		assertEquals("should be able to read the content twice", Misc.streamToString(url.openStream()), Misc.streamToString(ds.getInputStream()));
	}
}
