package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.frankframework.management.web.ApiException;
import org.junit.jupiter.api.Test;

public class RequestUtilsTest {

	public static class FileAttachment extends Attachment {
		private InputStream stream;

		public FileAttachment(String id, InputStream stream, String filename) {
			super(id, (InputStream) null, new ContentDisposition("attachment;filename="+filename));
			this.stream = stream;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getObject(Class<T> cls) {
			return (T) getObject();
		}

		@Override
		public Object getObject() {
			return stream;
		}
	}

	public static class StringAttachment extends Attachment {

		public StringAttachment(String name, String value) {
			this(name, value, StandardCharsets.UTF_8);
		}

		public StringAttachment(String name, String value, Charset charset) {
			super(name, "text/plain;charset="+charset.name(), new ByteArrayInputStream(value.getBytes(charset)));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getObject(Class<T> cls) {
			return (T) getObject();
		}
	}

	@Test
	public void testConversions() throws Exception {
		InputStream string = new ByteArrayInputStream("string".getBytes());
		InputStream number = new ByteArrayInputStream("50".getBytes());
		InputStream boolTrue = new ByteArrayInputStream("true".getBytes());
		InputStream boolFalse = new ByteArrayInputStream("false".getBytes());
		InputStream boolNull = new ByteArrayInputStream("".getBytes());

		assertEquals("string", RequestUtils.convert(String.class, string));
		assertEquals(true, RequestUtils.convert(boolean.class, boolTrue));
		assertEquals(false, RequestUtils.convert(Boolean.class, boolFalse));
		assertEquals(false, RequestUtils.convert(boolean.class, boolNull));
		assertEquals(50, RequestUtils.convert(Integer.class, number).intValue());
		assertEquals(string, RequestUtils.convert(InputStream.class, string));
	}

	@Test
	public void testGetValue() {
		// Arrange
		Map<String, Object> input = new HashMap<>();
		input.put("string", "value");
		input.put("integer", "123");
		input.put("boolean", "true");
		input.put("empty", "");
		input.put("null", null);

		// Act + Assert
		assertEquals("value", RequestUtils.getValue(input, "string"));
		assertEquals(123, RequestUtils.getIntegerValue(input, "integer"));
		assertTrue(RequestUtils.getBooleanValue(input, "boolean"));
		assertFalse(RequestUtils.getBooleanValue(input, "empty"));
		assertNull(RequestUtils.getIntegerValue(input, "non-existing-integer"));
		assertNull(RequestUtils.getBooleanValue(input, "non-existing-boolean"));
		assertEquals("", RequestUtils.getValue(input, "empty"));
		assertNull(RequestUtils.getValue(input, "null"));
	}

	// Streams can only be read once
	private MultipartBody createMultipartBody() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("string", "value"));
		attachments.add(new StringAttachment("empty", ""));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("dummy".getBytes()), "script.zip"));
		attachments.add(new StringAttachment("encoding", "håndværkere", StandardCharsets.ISO_8859_1));
		attachments.add(new FileAttachment("encoding2", new ByteArrayInputStream("håndværkere".getBytes(StandardCharsets.ISO_8859_1)), "script.zip"));
		return new MultipartBody(attachments);
	}

	//Explicitly added these tests because of CXF, see issue #3054
	@Test
	public void testMultipartBodyStringWithAndWithoutValue() {
		MultipartBody multipartBody = createMultipartBody(); // sonar RuntimeExceptions
		ApiException ex = assertThrows(ApiException.class, () -> RequestUtils.resolveStringFromMap(multipartBody, "empty"));
		assertEquals("Key [empty] may not be empty", ex.getMessage());

		assertEquals("value", RequestUtils.resolveStringFromMap(createMultipartBody(), "string"));
		assertEquals("value", RequestUtils.resolveStringFromMap(createMultipartBody(), "string", "default"));
		assertEquals("default", RequestUtils.resolveStringFromMap(createMultipartBody(), "empty", "default"));
		assertEquals("håndværkere", RequestUtils.resolveStringWithEncoding(createMultipartBody(), "encoding", null));
		assertEquals("h�ndv�rkere", RequestUtils.resolveStringWithEncoding(createMultipartBody(), "encoding2", null));
		assertEquals("håndværkere", RequestUtils.resolveStringWithEncoding(createMultipartBody(), "encoding2", "ISO-8859-1"));
	}
}
