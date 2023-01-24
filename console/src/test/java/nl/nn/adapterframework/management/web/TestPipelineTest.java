package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class TestPipelineTest extends FrankApiTestBase<TestPipeline>{

	@Override
	public TestPipeline createJaxRsResource() {
		return new TestPipeline() {
			@Override
			protected org.springframework.messaging.Message<?> sendSyncMessage(RequestMessageBuilder input) {
				return ResponseMessage.Builder.create().withPayload(input).setHeader(TestPipeline.RESULT_STATE_HEADER, "SUCCESS").toJson();
			}
		};
	}

	@Test
	public void testMessage() throws ConfigurationException, IOException {
		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new StringAttachment("message", "<dummy-message/>"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		String expected="{\"result\":\"{\\\"topic\\\":\\\"TEST_PIPELINE\\\",\\\"action\\\":\\\"UPLOAD\\\"}\",\"state\":\"SUCCESS\",\"message\":\"<dummy-message/>\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testFileMessage() throws ConfigurationException, IOException {
		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("<dummy-message/>".getBytes()), "my-file.xml"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		String expected="{\"result\":\"{\\\"topic\\\":\\\"TEST_PIPELINE\\\",\\\"action\\\":\\\"UPLOAD\\\"}\",\"state\":\"SUCCESS\",\"message\":\"<dummy-message/>\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testZipMessage() throws ConfigurationException, IOException {
		URL zip = TestFileUtils.getTestFileURL("/Webcontrol.api/temp.zip");

		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new StringAttachment("adapter", "HelloWorld"));
		attachments.add(new FileAttachment("file", zip.openStream(), "archive.zip"));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		String expected="{\"result\":\"Test1.txt: SUCCESS\\nTest2.txt: SUCCESS\\n\",\"state\":\"SUCCESS\"}";
		assertEquals(expected, response.getEntity().toString());
	}
}
