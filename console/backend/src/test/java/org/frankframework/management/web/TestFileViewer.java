package org.frankframework.management.web;

import org.apache.commons.io.FilenameUtils;
import org.frankframework.management.bus.ResponseMessageBase;
import org.frankframework.util.StreamUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFileViewer extends FrankApiTestBase<FileViewer> {

	@Override
	public FileViewer createJaxRsResource() {
		return new FileViewer();
	}

	@Test
	public void testRetrievingHtmlProcessedFile() throws IOException {
		URL fileUrl = TestFileViewer.class.getResource("/FileViewer/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);

		doAnswer((i) -> getDefaultAnswer(i, filePath)).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		String requestUrl = "/file-viewer?file=" + fileName;
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, requestUrl, null, IbisRole.IbisTester, Map.of("Accept", MediaType.TEXT_HTML));
		StreamingOutput result = (StreamingOutput) response.getEntity();

		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		result.write(boas);
		Assertions.assertTrue(boas.toString().contains("<br>"));
	}

	@Test
	public void testRetrievingTextFile() throws IOException {
		URL fileUrl = TestFileViewer.class.getResource("/FileViewer/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);

		doAnswer((i) -> getDefaultAnswer(i, filePath)).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		String requestUrl = "/file-viewer?file=" + fileName;
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, requestUrl, null, IbisRole.IbisTester, Map.of("Accept", MediaType.TEXT_PLAIN));
		InputStream result = (InputStream) response.getEntity();

		String resultString = StreamUtil.streamToString(result);
        Assertions.assertFalse(resultString.contains("<br>"));
	}

	private static Message<?> getDefaultAnswer(InvocationOnMock i, String filePath) throws FileNotFoundException {
		RequestMessageBuilder inputMessage = i.getArgument(0);
		inputMessage.addHeader(ResponseMessageBase.STATUS_KEY, 200);
		inputMessage.addHeader(ResponseMessageBase.MIMETYPE_KEY, MediaType.TEXT_HTML);
		inputMessage.setPayload(new FileInputStream(filePath));
		Message<?> msg = inputMessage.build();
		MessageHeaders headers = msg.getHeaders();
		assertEquals("FILE_VIEWER", headers.get("topic"));
		return msg;
	}
}
