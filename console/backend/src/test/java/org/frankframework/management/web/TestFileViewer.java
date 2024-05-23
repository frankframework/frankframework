package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FilenameUtils;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.StreamUtil;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

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

		doAnswer(i -> getDefaultAnswer(i, filePath)).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		String requestUrl = "/file-viewer?file=" + fileName;
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, requestUrl, null, IbisRole.IbisTester, Map.of("Accept", MediaType.TEXT_HTML));
		StreamingOutput result = (StreamingOutput) response.getEntity();
		assertNotNull(result);

		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		result.write(boas);
		assertTrue(boas.toString().contains("<br>"));
	}

	@Test
	public void testRetrievingTextFile() throws IOException {
		URL fileUrl = TestFileViewer.class.getResource("/FileViewer/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);

		doAnswer(i -> getDefaultAnswer(i, filePath)).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		String requestUrl = "/file-viewer?file=" + fileName;
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, requestUrl, null, IbisRole.IbisTester, Map.of("Accept", MediaType.TEXT_PLAIN));
		InputStream result = (InputStream) response.getEntity();
		assertNotNull(result);

		String resultString = StreamUtil.streamToString(result);
		assertFalse(resultString.contains("<br>"));
	}

	@Test
	public void testDownloadFile() throws IOException {
		URL fileUrl = TestFileViewer.class.getResource("/FileViewer/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);

		doAnswer(i -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			Message<?> assessMsg = inputMessage.build();
			MessageHeaders headers = assessMsg.getHeaders();
			assertEquals("FILE_VIEWER", headers.get("topic"));
			assertEquals("octet-stream", headers.get("meta-resultType"));

			inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
			inputMessage.addHeader(MessageBase.CONTENT_DISPOSITION_KEY, "attachment; filename=\"" + fileName + "\"");
			inputMessage.addHeader(MessageBase.MIMETYPE_KEY, "application/" + headers.get("meta-resultType"));
			inputMessage.setPayload(new FileInputStream(filePath));
			return inputMessage.build();
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		String requestUrl = "/file-viewer?file=" + fileName + "&accept=application/octet-stream"; // ignore accept header & use parameter
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, requestUrl, null, IbisRole.IbisTester, Map.of("Accept", MediaType.TEXT_PLAIN));
		assertEquals("attachment; filename=\"" + fileName + "\"", response.getHeaderString("Content-Disposition"));
		assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, response.getMediaType());
		InputStream result = (InputStream) response.getEntity();
		assertNotNull(result);

		String resultString = StreamUtil.streamToString(result);
		assertFalse(resultString.contains("<br>"));
	}

	private static Message<?> getDefaultAnswer(InvocationOnMock i, String filePath) throws FileNotFoundException {
		RequestMessageBuilder inputMessage = i.getArgument(0);
		inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
		inputMessage.setPayload(new FileInputStream(filePath));
		Message<?> msg = inputMessage.build();
		MessageHeaders headers = msg.getHeaders();
		assertEquals("FILE_VIEWER", headers.get("topic"));
		return msg;
	}
}
