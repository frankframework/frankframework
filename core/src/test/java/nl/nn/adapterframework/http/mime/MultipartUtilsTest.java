package nl.nn.adapterframework.http.mime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeBodyPart;

public class MultipartUtilsTest {

	@Test
	public void testBinaryFile() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", "form-data; name=\"file\"; filename=\"dummy.jpg\"");
		assertTrue(MultipartUtils.isBinary(part));
	}

	@Test
	public void testBinaryText() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", "form-data; name=\"text\"");
		assertFalse(MultipartUtils.isBinary(part));
	}

	@Test
	public void testBinaryMtomFile() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Transfer-Encoding", "binary");
		assertTrue(MultipartUtils.isBinary(part));
	}

	@Test
	public void testTextMtomFile() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Transfer-Encoding", "8bit");
		assertFalse(MultipartUtils.isBinary(part));
	}

	@Test
	public void testBinaryAttachment() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", "attachment; filename=\"dummy.jpg\"");
		assertTrue(MultipartUtils.isBinary(part));
	}
}
