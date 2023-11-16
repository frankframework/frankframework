package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.support.GenericMessage;

import nl.nn.adapterframework.management.bus.BinaryResponseMessage;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.ResponseMessageBase;

public class TestPipelineTest extends FrankApiTestBase<TestPipeline>{

	@Override
	public TestPipeline createJaxRsResource() {
		return new TestPipeline();
	}

	private static class DefaultSuccessAnswer implements Answer<JsonResponseMessage> {
		@Override
		public JsonResponseMessage answer(InvocationOnMock invocation) {
			Object input = invocation.getArguments()[0];
			JsonResponseMessage response = new JsonResponseMessage(input);
			response.setHeader(ResponseMessageBase.STATE_KEY, "SUCCESS");
			return response;
		}

	}

	@Test
	public void testMessage() throws Exception {
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new StringAttachment("message", "<dummy-message/>"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		String expected="{\"result\":\"{\\\"topic\\\":\\\"TEST_PIPELINE\\\",\\\"action\\\":\\\"UPLOAD\\\"}\",\"state\":\"SUCCESS\",\"message\":\"<dummy-message/>\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testFileMessage() throws Exception {
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("<dummy-message/>".getBytes()), "my-file.xml"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		assertEquals("application/json", response.getHeaderString("Content-Type"));
		String expected="{\"result\":\"{\\\"topic\\\":\\\"TEST_PIPELINE\\\",\\\"action\\\":\\\"UPLOAD\\\"}\",\"state\":\"SUCCESS\",\"message\":\"<dummy-message/>\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testZipMessage() throws Exception {
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		URL zip = TestPipelineTest.class.getResource("/TestPipeline/temp.zip");

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new FileAttachment("file", zip.openStream(), "archive.zip"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		assertEquals("application/json", response.getHeaderString("Content-Type"));
		String expected="{\"result\":\"Test1.txt: SUCCESS\\nTest2.txt: SUCCESS\\n\",\"state\":\"SUCCESS\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testPipelineBinaryResponse() throws Exception {
		doAnswer((e)->{return new BinaryResponseMessage("dummy data".getBytes());}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new StringAttachment("message", "<dummy-message/>"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		assertEquals("application/json", response.getHeaderString("Content-Type")); //The endpoint always returns JSON
		assertEquals("{\"result\":\"dummy data\",\"state\":null,\"message\":\"<dummy-message/>\"}", response.getEntity().toString());
	}

	@Test
	public void testPipelineUnknownResponse() throws Exception {
		doAnswer((e)->{return new GenericMessage<>(123L);}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new StringAttachment("message", "<dummy-message/>"));

		assertThrows(ApiException.class, ()-> dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments));
	}
}
