package org.frankframework.http.mime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;

public class MultipartEntityTest {
	private static final String FORMDATA_BOUNDARY = "multipart/form-data; boundary=test-boundary";

	@Test
	public void testFormdataEntity() throws Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setBoundary("test-boundary");
		builder.addTextBody("part1", "content1");
		builder.addTextBody("part2", "content2");
		MultipartEntity entity = builder.build();

		assertTrue(entity.isRepeatable());
		assertFalse(entity.isChunked());
		assertTrue(entity.isStreaming());
		assertEquals(FORMDATA_BOUNDARY, entity.getContentType().getValue());
		assertEquals(327, entity.getContentLength());
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart.txt"), toString(entity));
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart.txt"), toString(entity)); //Test repeatability
	}

	@Test
	public void testMtomEntity() throws Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMtomMultipart();
		builder.setBoundary("test-boundary");
		builder.addTextBody("part1", "content1");
		builder.addTextBody("part2", "content2");
		MultipartEntity entity = builder.build();

		assertTrue(entity.isRepeatable());
		assertFalse(entity.isChunked());
		assertTrue(entity.isStreaming());
		assertEquals("multipart/related; boundary=test-boundary; type=\"application/xop+xml\"; start=\"<part1>\"; start-info=\"text/xml\"", entity.getContentType().getValue());
		assertEquals(277, entity.getContentLength());
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/mtom.txt"), toString(entity));
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/mtom.txt"), toString(entity)); //Test repeatability
	}

	@Test
	public void testCharset() throws Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setCharset(Charset.forName("UTF-8"));
		builder.setBoundary("test-boundary");
		builder.addTextBody("part1", "content1");
		builder.addTextBody("part2", "content2");
		MultipartEntity entity = builder.build();

		assertTrue(entity.isRepeatable());
		assertFalse(entity.isChunked());
		assertTrue(entity.isStreaming());
		assertEquals(FORMDATA_BOUNDARY+"; charset=UTF-8", entity.getContentType().getValue());
		assertEquals(327, entity.getContentLength());
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart.txt"), toString(entity)); //Charset should not change the parts
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart.txt"), toString(entity)); //Test repeatability
	}

	@Test
	public void testContentType() throws Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setContentType(ContentType.TEXT_XML); //ContentType with charset ISO-8859-1
		builder.setBoundary("test-boundary");
		builder.addTextBody("part1", "content1");
		builder.addTextBody("part2", "content2");
		MultipartEntity entity = builder.build();

		assertTrue(entity.isStreaming());
		assertEquals("text/xml; boundary=test-boundary; charset=ISO-8859-1", entity.getContentType().getValue());
		assertEquals(327, entity.getContentLength());
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart.txt"), toString(entity)); //Charset should not change the parts
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart.txt"), toString(entity)); //Test repeatability
	}

	@Test
	public void testContentTypeAndCharset() throws Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setCharset(Charset.forName("UTF-8")); //Should be used even though defined earlier
		builder.setContentType(ContentType.TEXT_XML); //ContentType with charset ISO-8859-1
		builder.setBoundary("test-boundary");
		builder.addTextBody("part1", "content1");
		builder.addTextBody("part2", "content2");
		MultipartEntity entity = builder.build();

		assertTrue(entity.isStreaming());
		assertEquals("text/xml; boundary=test-boundary; charset=UTF-8", entity.getContentType().getValue());
		assertEquals(327, entity.getContentLength());
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart.txt"), toString(entity)); //Charset should not change the parts
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart.txt"), toString(entity)); //Test repeatability
	}

	@Test
	public void testFormdataBoundary() {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		MultipartEntity entity = builder.build();
		String contentType = entity.getContentType().getValue();
		String boundary = contentType.split("boundary=")[1];
		assertTrue(boundary.length() >= 30, "boundary was ["+boundary+"]");
	}

	@Test
	public void testMtomBoundary() {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMtomMultipart();
		MultipartEntity entity = builder.build();
		String contentType = entity.getContentType().getValue();
		String boundary = contentType.split("boundary=")[1];
		assertTrue(boundary.startsWith("\"----="));
	}

	@Test
	public void testWithRepeatableMessage() throws Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setBoundary("test-boundary");

		Message repeatable = new Message(new Message("dummy-content-here").asByteArray());

		builder.addPart("part1", repeatable);
		builder.addPart("part2", repeatable);
		MultipartEntity entity = builder.build();

		assertTrue(entity.isRepeatable());
		assertFalse(entity.isChunked());
		assertTrue(entity.isStreaming());
		assertEquals(FORMDATA_BOUNDARY, entity.getContentType().getValue());
		assertEquals(339, entity.getContentLength());
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart-message.txt"), toString(entity));
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart-message.txt"), toString(entity)); //Test repeatability
	}

	@Test
	public void testWithNonRepeatableMessage() throws Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setBoundary("test-boundary");

		Message repeatable = Message.asMessage("dummy-content-here".getBytes(StandardCharsets.UTF_8));
		Message nonRepeatable = new Message(new FilterInputStream(repeatable.asInputStream()) {});

		builder.addPart("part1", repeatable);
		builder.addPart("part2", nonRepeatable);
		MultipartEntity entity = builder.build();

		assertTrue(entity.isRepeatable());
		assertFalse(entity.isChunked());
		assertTrue(entity.isStreaming());
		assertEquals(FORMDATA_BOUNDARY, entity.getContentType().getValue());
		assertEquals(339L, entity.getContentLength());
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart-message.txt"), toString(entity));
	}

	@ParameterizedTest
	@EnumSource(value = MessageType.class)
	public void testWriteToCharacterData(MessageType type) throws Exception {
		Message charMessage = MessageTestUtils.getMessage(type);

		MessageContentBody contentBody = new MessageContentBody(charMessage);

		// Act
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		contentBody.writeTo(boas);

		// Assert
		assertEquals(charMessage.asString(), boas.toString().replace("\ufeff", ""));//remove BOM if present
	}

	private String toString(HttpEntity entity) throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		entity.writeTo(boas);
		return boas.toString();
	}
}
