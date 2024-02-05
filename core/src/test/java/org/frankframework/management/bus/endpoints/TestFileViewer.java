package org.frankframework.management.bus.endpoints;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.InputStream;

public class TestFileViewer extends BusTestBase {

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void getFileContent() throws Exception {
		String testFilePath = "/Management/FileViewer.txt";
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.FILE_VIEWER, BusAction.GET);
		request.setHeader("resultType", "text/plain");
		request.setHeader("fileName", TestFileUtils.getTestFilePath(testFilePath));

		Message<?> response = callSyncGateway(request);
		String expectedTxt = TestFileUtils.getTestFile(testFilePath);
		String result = new String(((InputStream) response.getPayload()).readAllBytes());
		Assertions.assertEquals(expectedTxt, result);
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void getFileWithContentType(){

	}

}
