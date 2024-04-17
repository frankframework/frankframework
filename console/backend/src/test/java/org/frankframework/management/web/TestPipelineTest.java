package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.bus.message.MessageBase;

public class TestPipelineTest extends FrankApiTestBase<TestPipeline> {

	@Override
	public TestPipeline createJaxRsResource() {
		return new TestPipeline();
	}

	private static class DefaultSuccessAnswer implements Answer<JsonMessage> {
		@Override
		public JsonMessage answer(InvocationOnMock invocation) {
			Object input = invocation.getArguments()[0];
			JsonMessage response = new JsonMessage(input);
			response.setHeader(MessageBase.STATE_KEY, "SUCCESS");
			return response;
		}

	}

	@Test
	public void testMessage() {
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new StringAttachment("message", "<dummy-message/>"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		String expected = "{\"result\":\"{\\\"topic\\\":\\\"TEST_PIPELINE\\\",\\\"action\\\":\\\"UPLOAD\\\"}\",\"state\":\"SUCCESS\",\"message\":\"<dummy-message/>\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testFileMessage() {
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("<dummy-message/>".getBytes()), "my-file.xml"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		assertEquals("application/json", response.getHeaderString("Content-Type"));
		String expected = "{\"result\":\"{\\\"topic\\\":\\\"TEST_PIPELINE\\\",\\\"action\\\":\\\"UPLOAD\\\"}\",\"state\":\"SUCCESS\",\"message\":\"<dummy-message/>\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testStoredZipMessage() throws Exception {
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		URL zip = TestPipelineTest.class.getResource("/TestPipeline/stored.zip");

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new FileAttachment("file", zip.openStream(), "archive2.zip"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		assertEquals("application/json", response.getHeaderString("Content-Type"));
		String expected = "{\"result\":\"msg-7.xml: SUCCESS\\n\",\"state\":\"SUCCESS\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testDeflatedZipMessage() throws Exception {
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		URL zip = TestPipelineTest.class.getResource("/TestPipeline/deflated.zip");

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new FileAttachment("file", zip.openStream(), "archive3.zip"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		assertEquals("application/json", response.getHeaderString("Content-Type"));
		String expected = "{\"result\":\"msg-2.xml: SUCCESS\\n\",\"state\":\"SUCCESS\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testPipelineBinaryResponse() {
		doAnswer(e -> new BinaryMessage("dummy data".getBytes())).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new StringAttachment("message", "<dummy-message/>"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		assertEquals("application/json", response.getHeaderString("Content-Type")); //The endpoint always returns JSON
		assertEquals("{\"result\":\"dummy data\",\"state\":null,\"message\":\"<dummy-message/>\"}", response.getEntity().toString());
	}

	@Test
	public void testPipelineUnknownResponse() {
		doAnswer(e -> new GenericMessage<>(123L)).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new StringAttachment("message", "<dummy-message/>"));

		assertThrows(ApiException.class, () -> dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments));
	}

	@Test
	void testMessageLineEndingsConversion() {
		String input = "\"\n<asdf>asd\rasd\rasd\rasd\rad\rccc\rttt\rbbb\ryyy\ruuu\roooo\r</asdf>\"";
		InputStream message = new ByteArrayInputStream(input.getBytes()); // Stream is needed to test the line endings conversion
		GenericMessage<InputStream> genericMessage = new GenericMessage<>(message, new MessageHeaders(null));
		doAnswer(e -> genericMessage).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "FakeMainConfig"));
		attachments.add(new StringAttachment("adapter", "FakeXsltPipe"));
		attachments.add(new StringAttachment("message", input));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		String expected = "{\"result\":\"\\\"\\n<asdf>asd\\nasd\\nasd\\nasd\\nad\\nccc\\nttt\\nbbb\\nyyy\\nuuu\\noooo\\n</asdf>\\\"\",\"state\":null,\"message\":\"\\\"\\n<asdf>asd\\nasd\\nasd\\nasd\\nad\\nccc\\nttt\\nbbb\\nyyy\\nuuu\\noooo\\n</asdf>\\\"\"}";
		assertEquals(expected, response.getEntity().toString());
	}
}
