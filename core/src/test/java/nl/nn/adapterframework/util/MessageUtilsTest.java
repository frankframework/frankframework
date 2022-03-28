package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.stream.MessageContext;

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
}
