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
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class TestPipelineTest extends ApiTestBase<TestPipeline>{

	@Override
	public TestPipeline createJaxRsResource() {
		return new TestPipeline();
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

	@Override
	protected void registerAdapter(TestConfiguration configuration) throws Exception {
		Adapter adapter = spy(Adapter.class);
		adapter.setName("HelloWorld");
		getConfiguration().autowireByName(adapter);

		PipeLineResult plr = new PipeLineResult("", ExitState.SUCCESS, Message.asMessage("Success"));

		doReturn(plr).when(adapter).processMessage(anyString(), any(Message.class), any(PipeLineSession.class));

		getConfiguration().registerAdapter(adapter);

	}

	@Test
	public void testArchiveNotClosedDuringProcessing() throws ConfigurationException, IOException {
		URL zip = TestFileUtils.getTestFileURL("/Webcontrol.api/temp.zip");

		CustomAttachment attachmentFile = new CustomAttachment("file", zip.openStream(), new ContentDisposition("attachment;filename=temp.zip"));
		attachmentFile.setObject(zip.openStream());

		Attachment attachmentAdapter = new Attachment("adapter", "application/text", new ByteArrayInputStream("HelloWorld".getBytes())) {
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getObject(Class<T> cls) {
				return (T) getObject();
			}
		};

		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(attachmentFile);
		attachments.add(attachmentAdapter);

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-pipeline", attachments);
		String expected="{\"result\":\"Test1.txt:SUCCESS\\nTest2.txt:SUCCESS\",\"state\":\"SUCCESS\"}";
		assertEquals(expected, response.getEntity().toString());
	}

	@Test
	public void testGetIbisContext() throws Exception {
		TestPipeline testPipeline = createJaxRsResource();
		String input = "<?ibiscontext key1=value1 key2=value2 ?> <test/>";
		assertEquals("{key1=value1 key2=value2}", testPipeline.getIbisContext(input).toString());
	}
}
