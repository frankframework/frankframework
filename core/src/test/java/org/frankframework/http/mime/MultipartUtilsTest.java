package org.frankframework.http.mime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeBodyPart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class MultipartUtilsTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"form-data; name=\"file\"; filename=\"dummy.jpg\"",
			"form-data; name=\"file\"; filename=\"polisnr=123.jpg\"",
	})
	public void testBinaryFile(String headerValue) throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", headerValue);
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

	@ParameterizedTest
	@ValueSource(strings = {
			"attachment; filename",
			"attachment; filename=",
			"attachment; name=\"field_name\"",
	})
	public void testIsBinaryInvalidHeader(String headerValue) throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", headerValue);
		assertFalse(MultipartUtils.isBinary(part));
	}

	@ParameterizedTest
	@MethodSource
	public void testGetFileName(String headerValue, String expectedFilename) throws Exception {
		// Arrange
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", headerValue);

		// Act
		String fileName = MultipartUtils.getFileName(part);

		// Assert
		assertEquals(expectedFilename, fileName);
	}

	public static Stream<Arguments> testGetFileName() {
		return Stream.of(
				Arguments.of("attachment; filename=\"dummy.jpg\"; name=\"field_name\"", "dummy.jpg"),
				Arguments.of("attachment; filename=\"polis=123.pdf\"; name=\"field_name\"", "polis=123.pdf"),
				Arguments.of("attachment; filename=; name=\"field_name\"", null)
		);
	}

	@Test
	public void testGetFieldName() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", "attachment; filename=\"dummy.jpg\"; name=\"field_name\"");
		assertEquals("field_name", MultipartUtils.getFieldName(part));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"attachment; filename=\"dummy.jpg\"; ",
			"attachment; filename=; name=",
			"attachment; filename=\"dummy.jpg\"; name",
	})
	public void testGetFieldNameInvalidHeader(String headerValue) throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", headerValue);
		assertNull(MultipartUtils.getFieldName(part));
	}

	@Test
	public void testIsBinaryNullHeader() {
		BodyPart part = new MimeBodyPart();
		assertFalse(MultipartUtils.isBinary(part));
	}

	@Test
	public void testIsBinaryEmptyHeader() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-Disposition", "");
		assertFalse(MultipartUtils.isBinary(part));
	}

	@Test
	public void testGetFieldNameNullHeader() {
		BodyPart part = new MimeBodyPart();
		assertNull(MultipartUtils.getFieldName(part));
	}

	@Test
	public void testGetFieldNameFromContentId() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-ID", "<field_name>");
		assertEquals("field_name", MultipartUtils.getFieldName(part));
	}

	@Test
	public void testGetFieldNameFromContentIdEmptyHeader() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-ID", "");
		assertNull(MultipartUtils.getFieldName(part));
	}

	@Test
	public void testGetFieldNameTwoHeaders() throws Exception {
		BodyPart part = new MimeBodyPart();
		part.setHeader("Content-ID", "");
		part.setHeader("Content-Disposition", "attachment; filename=\"dummy.jpg\"; name=\"field_name\"");
		assertEquals("field_name", MultipartUtils.getFieldName(part));
	}
}
