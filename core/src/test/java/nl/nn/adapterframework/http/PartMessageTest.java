package nl.nn.adapterframework.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.junit.Test;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.credentialprovider.util.Misc;

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
		private InputStream stream = null;//non-repeatable

		public TestPart(String contentType) {
			super();
			headers.addHeader("Content-Type", contentType);
		}

		public TestPart(URL url) throws IOException {
			this.stream = url.openStream();
			headers.addHeader("Content-Type", "application/octet-stream");
		}

		@Override
		public InputStream getInputStream() throws IOException, MessagingException {
			return stream;
		}
	}

	@Test
	public void testParameterValue() throws Exception {
		String sessionKey = "file";

		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey(sessionKey);
		p.configure();

		PipeLineSession session = new PipeLineSession();
		TestPart testPart = new TestPart(TestFileUtils.getTestFileURL("/file.xml"));
		session.put(sessionKey, new PartMessage(testPart));

		ParameterValueList pvl = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object value = p.getValue(pvl, message, session, false);
		assertTrue(value instanceof InputStream);
		assertEquals("<file>in root of classpath</file>", Misc.streamToString((InputStream) value));
	}
}
