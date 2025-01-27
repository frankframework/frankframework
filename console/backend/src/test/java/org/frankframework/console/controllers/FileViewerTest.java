package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.io.FileInputStream;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.message.BinaryMessage;

@ContextConfiguration(classes = {WebTestConfiguration.class, FileViewer.class})
public class FileViewerTest extends FrankApiTestBase {

	@Test
	public void testRetrievingTextFile() throws Exception {
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
	public void testDownloadFile() throws Exception {
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

}
