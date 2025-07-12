package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.message.BinaryMessage;

@ContextConfiguration(classes = { WebTestConfiguration.class, FileViewer.class })
class FileViewerTest extends FrankApiTestBase {

	@Test
	void testRetrievingTextFile() throws Exception {
		URL fileUrl = FileViewerTest.class.getResource("/management/web/FileViewer.txt");
		assert fileUrl != null;
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);
		String requestUrl = "/file-viewer?file=" + fileName;

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();
			assertEquals("FILE_VIEWER", headers.get("topic"));

			BinaryMessage responseMsg = new BinaryMessage(new FileInputStream(filePath), MediaType.valueOf("text/" + headers.get("meta-resultType")));
			responseMsg.setFilename("inline", fileName);
			return responseMsg;
		});

		mockMvc.perform(MockMvcRequestBuilders
						.get(requestUrl)
						.accept(MediaType.TEXT_PLAIN))
				.andExpect(request().asyncStarted())
				.andDo(MvcResult::getAsyncResult)
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_PLAIN))
				.andExpect(MockMvcResultMatchers.header().string("Content-Disposition", "inline; filename=\"FileViewer.txt\""))
				.andReturn();
	}

	@Test
	void testDownloadFile() throws Exception {
		URL fileUrl = FileViewerTest.class.getResource("/management/web/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);
		String requestUrl = "/file-viewer?file=" + fileName + "&accept=application/octet-stream"; // ignore accept header & use parameter

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();
			assertEquals("FILE_VIEWER", headers.get("topic"));
			assertEquals("octet-stream", headers.get("meta-resultType"));

			BinaryMessage responseMsg = new BinaryMessage(new FileInputStream(filePath), MediaType.valueOf("application/" + headers.get("meta-resultType")));
			responseMsg.setFilename(fileName);
			return responseMsg;
		});

		mockMvc.perform(MockMvcRequestBuilders
						.get(requestUrl)
						.accept(MediaType.TEXT_PLAIN))
				.andExpect(request().asyncStarted())
				.andDo(MvcResult::getAsyncResult)
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_OCTET_STREAM))
				.andExpect(MockMvcResultMatchers.header().string("Content-Disposition", "attachment; filename=\"FileViewer.txt\""))
				.andReturn();
	}

	@Test
	@DisplayName("When getInputStreamMessage is called with String payload, Then Message is recreated into a Stream payload")
	void testGetInputStreamMessage_stringPayload() throws Exception {
		String text = "this is a test string payload \n testing123";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");
		GenericMessage<String> stringMsg = new GenericMessage<>(text, headers);

		Message<InputStream> result = FileViewer.getInputStreamMessage(stringMsg);

		InputStream is = result.getPayload();
		byte[] bytes = is.readAllBytes();
		assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), bytes);

		assertEquals("bar", result.getHeaders().get("foo"));
	}

}
