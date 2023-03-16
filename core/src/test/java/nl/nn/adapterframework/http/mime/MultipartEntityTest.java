package nl.nn.adapterframework.http.mime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.junit.Test;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.MessageTestUtils.MessageType;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

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
	public void testFormdataBoundary() throws Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		MultipartEntity entity = builder.build();
		String contentType = entity.getContentType().getValue();
		String boundary = contentType.split("boundary=")[1];
		assertTrue("boundary was ["+boundary+"]", boundary.length() >= 30);
	}

	@Test
	public void testMtomBoundary() throws Exception {
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

		Message repeatable = Message.asMessage(new Message("dummy-content-here").asByteArray());

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

		Message repeatable = Message.asMessage(new Message("dummy-content-here").asByteArray());
		Message nonRepeatable = Message.asMessage(new FilterInputStream(repeatable.asInputStream()) {});

		builder.addPart("part1", repeatable);
		builder.addPart("part2", nonRepeatable);
		MultipartEntity entity = builder.build();

		assertFalse(entity.isRepeatable());
		assertTrue(entity.isChunked());
		assertFalse(entity.isStreaming());
		assertEquals(FORMDATA_BOUNDARY, entity.getContentType().getValue());
		assertEquals(-1, entity.getContentLength());
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/Http/Entity/multipart-message.txt"), toString(entity));
	}

	@Test
	public void testWriteToCharacterData() throws Exception {
		testWriteToCharacterData(MessageType.CHARACTER_UTF8);
		testWriteToCharacterData(MessageType.CHARACTER_ISO88591);
		testWriteToCharacterData(MessageType.BINARY);
	}
	public void testWriteToCharacterData(MessageType type) throws Exception {
		Message charMessage = MessageTestUtils.getMessage(type);

		charMessage.preserve();
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
