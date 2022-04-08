package nl.nn.adapterframework.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.mail.internet.MimeBodyPart;

import org.junit.Test;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.stream.MessageContext;

public class PartMessageTest {

	@Test
	public void testCharset() throws Exception {
		TestPart testPart = new TestPart("application/pdf; charset=UTF-8");

		PartMessage partMessage = new PartMessage(testPart);

		assertEquals("UTF-8", partMessage.getCharset());
		assertEquals(MimeType.valueOf("application/pdf; charset=UTF-8"), partMessage.getContext().get(MessageContext.METADATA_MIMETYPE));
	}

	@Test
	public void testIllegalCharset() throws Exception {

		TestPart testPart = new TestPart("application/pdf; charset=application/pdf");

		PartMessage partMessage = new PartMessage(testPart);

		assertNull(partMessage.getCharset());
		assertNull(partMessage.getContext().get(MessageContext.METADATA_MIMETYPE));
	}

	private class TestPart extends MimeBodyPart {

		TestPart(String contentType) {
			super();
			headers.addHeader("Content-Type", contentType);
		}
	}
}
