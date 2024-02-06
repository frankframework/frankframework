package org.frankframework.management.web;

import org.apache.commons.io.FilenameUtils;
import org.frankframework.management.bus.ResponseMessageBase;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFileViewer extends FrankApiTestBase<FileViewer> {

	@Override
	public FileViewer createJaxRsResource() {
		return new FileViewer();
	}

//	@Test
	public void testRetrievingFile() {
		URL fileUrl = TestFileViewer.class.getResource("/FileViewer/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);

		String requestUrl = "/file-viewer?file=" + filePath;
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, requestUrl, null, IbisRole.IbisTester);

		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, response.getMediaType());
		assertEquals("attachment; filename=\""+fileName+"\"", response.getHeaderString("Content-Disposition"));
	}

//	@Test
	public void testRetrievingHtmlProcessedFile(){
		URL fileUrl = TestFileViewer.class.getResource("/FileViewer/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);

		String requestUrl = "/file-viewer?file=" + filePath;
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, requestUrl, null, IbisRole.IbisTester);

		assertEquals(200, response.getStatus());
		assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
		assertEquals("inline; filename=\""+fileName+"\"", response.getHeaderString("Content-Disposition"));

		// TODO test if lines have <br> at the end
	}
}
