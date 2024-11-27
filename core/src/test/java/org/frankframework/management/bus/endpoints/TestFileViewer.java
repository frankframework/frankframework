package org.frankframework.management.bus.endpoints;

import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.StreamUtil;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestFileViewer extends BusTestBase {

	private static final String TestFileName = "FileViewer.txt";
	private static final String TestFilePath = "/Management/" + TestFileName;

	@Test
	public void getFileContent() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("resultType", "plain");
		request.setHeader("fileName", TestFileUtils.getTestFilePath(TestFilePath));

		Message<?> response = callSyncGateway(request);
		String expectedTxt = TestFileUtils.getTestFile(TestFilePath);
		String result = StreamUtil.streamToString((InputStream) response.getPayload());
		Assertions.assertEquals(expectedTxt, result);
	}

	@Test
	public void getFileWithHtmlContentType(){
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("resultType", "html");
		request.setHeader("fileName", TestFileUtils.getTestFilePath(TestFilePath));

		Message<?> response = callSyncGateway(request);
		String contentType = BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY, null);
		String contentDisposition = BusMessageUtils.getHeader(response, MessageBase.CONTENT_DISPOSITION_KEY, null);
		Assertions.assertEquals(MediaType.TEXT_HTML_VALUE, contentType);
		Assertions.assertEquals("inline; filename=\""+TestFileName+"\"", contentDisposition);
	}

	@Test
	public void getFileWithXmlContentType(){
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("resultType", "xml");
		request.setHeader("fileName", TestFileUtils.getTestFilePath(TestFilePath));

		Message<?> response = callSyncGateway(request);
		String contentType = BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY, null);
		String contentDisposition = BusMessageUtils.getHeader(response, MessageBase.CONTENT_DISPOSITION_KEY, null);
		Assertions.assertEquals(MediaType.APPLICATION_XML_VALUE, contentType);
		Assertions.assertEquals("inline; filename=\""+TestFileName+"\"", contentDisposition);
	}

	@Test
	public void getFileWithTextContentType(){
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("resultType", "plain");
		request.setHeader("fileName", TestFileUtils.getTestFilePath(TestFilePath));

		Message<?> response = callSyncGateway(request);
		String contentType = BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY, null);
		String contentDisposition = BusMessageUtils.getHeader(response, MessageBase.CONTENT_DISPOSITION_KEY, null);
		Assertions.assertEquals(MediaType.TEXT_PLAIN_VALUE, contentType);
		Assertions.assertEquals("inline; filename=\""+TestFileName+"\"", contentDisposition);
	}

	@Test
	public void getFileWithZipContentType(){
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("resultType", "zip");
		request.setHeader("fileName", TestFileUtils.getTestFilePath(TestFilePath));

		Message<?> response = callSyncGateway(request);
		String contentType = BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY, null);
		String contentDisposition = BusMessageUtils.getHeader(response, MessageBase.CONTENT_DISPOSITION_KEY, null);
		Assertions.assertEquals("application/zip", contentType);
		Assertions.assertEquals("attachment; filename=\""+TestFileName+"\"", contentDisposition);
	}

	@Test
	public void getFileWithAnyContentType(){
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("resultType", "*");
		request.setHeader("fileName", TestFileUtils.getTestFilePath(TestFilePath));

		Message<?> response = callSyncGateway(request);
		String contentType = BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY, null);
		String contentDisposition = BusMessageUtils.getHeader(response, MessageBase.CONTENT_DISPOSITION_KEY, null);
		Assertions.assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, contentType);
		Assertions.assertEquals("attachment; filename=\""+TestFileName+"\"", contentDisposition);
	}

	@Test
	public void getFileWithoutFilename(){
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("resultType", "*");

		Exception thrown = Assertions.assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
		Throwable exceptionCause = thrown.getCause();
        Assertions.assertInstanceOf(BusException.class, exceptionCause);
		Assertions.assertTrue(exceptionCause.getMessage().contains("fileName or type not specified"));
	}

	@Test
	public void getFileWithoutType(){
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("fileName", TestFileUtils.getTestFilePath(TestFilePath));

		Exception thrown = Assertions.assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
		Throwable exceptionCause = thrown.getCause();
		Assertions.assertInstanceOf(BusException.class, exceptionCause);
		Assertions.assertTrue(exceptionCause.getMessage().contains("fileName or type not specified"));
	}

}
