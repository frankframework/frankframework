package nl.nn.adapterframework.http;

import static org.junit.Assert.assertNull;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.junit.Test;

public class PartMessageTest {

	@Test
	public void testIllegalCharset() throws MessagingException {

		TestPart testPart = new TestPart("application/pdf; charset=application/pdf");

		PartMessage partMessage = new PartMessage(testPart);

		assertNull(partMessage.getCharset());
	}

	private class TestPart extends MimeBodyPart {

		TestPart(String contentType) {
			super();
			headers.addHeader("Content-Type", contentType);
		}
	}
}
