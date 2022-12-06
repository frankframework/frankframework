package nl.nn.adapterframework.webcontrol.api;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;
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

	private class CustomAttachment extends Attachment {

		private Object data;

		@Override
		public <T> T getObject(Class<T> cls) {
			return (T) data;
		}

		public CustomAttachment(String id, InputStream openStream, ContentDisposition contentDisposition) {
			super(id, openStream, contentDisposition);
			data = openStream;
		}

		public void setObject(Object o) {
			data=o;
		}
	}

	@Test
	public void testArchiveNotClosedDuringProcessing() throws ConfigurationException, IOException {
		URL zip = TestFileUtils.getTestFileURL("/Webcontrol.api/temp.zip");

		CustomAttachment attachmentFile = new CustomAttachment("file", zip.openStream(), new ContentDisposition("attachment;filename=temp.zip"));
		attachmentFile.setObject(zip.openStream());

		Attachment adapter = new Attachment("adapter", "text/plain", new ByteArrayInputStream("HelloWorld".getBytes())) {
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getObject(Class<T> cls) {
				return (T) getObject();
			}
		};
		Attachment configuration = new Attachment("configuration", "text/plain", new ByteArrayInputStream("TestConfiguration".getBytes())) {
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getObject(Class<T> cls) {
				return (T) getObject();
			}
		};

		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(attachmentFile);
		attachments.add(adapter);
		attachments.add(configuration);

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		String expected="{\"result\":\"Test1.txt: SUCCESS\\nTest2.txt: SUCCESS\\n\",\"state\":\"SUCCESS\"}";
		assertEquals(expected, response.getEntity().toString());
	}
}
