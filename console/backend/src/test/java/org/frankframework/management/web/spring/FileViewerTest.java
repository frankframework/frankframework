package org.frankframework.management.web.spring;

import org.apache.commons.io.FilenameUtils;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.management.web.RequestMessageBuilder;
import org.frankframework.management.web.TestFileViewer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = {WebTestConfiguration.class, FileViewer.class})
public class FileViewerTest extends FrankApiTestBase {

	@Test
	@Disabled
	public void testRetrievingHtmlProcessedFile() throws Exception {
		URL fileUrl = TestFileViewer.class.getResource("/FileViewer/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);
		String requestUrl = "/file-viewer?file=" + fileName;

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> getDefaultAnswer(i, filePath));

		MvcResult response = mockMvc.perform(MockMvcRequestBuilders
					.get(requestUrl)
					.accept(MediaType.TEXT_HTML))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_HTML))
				.andReturn();

		OutputStream result = response.getResponse().getOutputStream();
		assertNotNull(result);

		assertTrue(result.toString().contains("<br>"));
	}

	@Test
	public void testRetrievingTextFile() throws Exception {
		URL fileUrl = TestFileViewer.class.getResource("/management/web/FileViewer.txt");
		assert fileUrl != null;
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);
		String requestUrl = "/file-viewer?file=" + fileName;

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> getDefaultAnswer(i, filePath));

		MvcResult response = mockMvc.perform(MockMvcRequestBuilders
						.get(requestUrl)
						.accept(MediaType.TEXT_PLAIN))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_PLAIN))
				.andReturn();
		OutputStream result = response.getResponse().getOutputStream();
		assertNotNull(result);
		assertFalse(result.toString().contains("<br>"));
	}

	@Test
	public void testDownloadFile() throws Exception {
		URL fileUrl = TestFileViewer.class.getResource("/management/web/FileViewer.txt");
		String filePath = fileUrl.getPath();
		String fileName = FilenameUtils.getName(filePath);
		String requestUrl = "/file-viewer?file=" + fileName + "&accept=application/octet-stream"; // ignore accept header & use parameter

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();
			assertEquals("FILE_VIEWER", headers.get("topic"));
			assertEquals("octet-stream", headers.get("meta-resultType"));
			return new Message<>() {
				@Override
				public FileInputStream getPayload() {
					try {
						return new FileInputStream(filePath);
					} catch (FileNotFoundException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public MessageHeaders getHeaders() {
					Map<String, Object> headers = new HashMap<>(msg.getHeaders());
					headers.put("state", "SUCCESS");
					headers.put("meta-state", "SUCCESS");
					headers.put(MessageBase.STATUS_KEY, 200);
					headers.put("meta-" + MessageBase.STATUS_KEY, 200);
					headers.put(MessageBase.MIMETYPE_KEY, "application/" + headers.get("meta-resultType"));
					headers.put("meta-" + MessageBase.MIMETYPE_KEY, "application/" + headers.get("meta-resultType"));
					headers.put(MessageBase.CONTENT_DISPOSITION_KEY, "attachment; filename=\"" + fileName + "\"");
					headers.put("met-" + MessageBase.CONTENT_DISPOSITION_KEY, "attachment; filename=\"" + fileName + "\"");

					return new MessageHeaders(headers);
				}
			};
		});

		MvcResult response = mockMvc.perform(MockMvcRequestBuilders
						.get(requestUrl)
						.accept(MediaType.TEXT_PLAIN))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_OCTET_STREAM))
				.andReturn();
		OutputStream result = response.getResponse().getOutputStream();
		assertNotNull(result);
		assertFalse(result.toString().contains("<br>"));
	}

	private Message<?> getDefaultAnswer(InvocationOnMock i, String filePath) throws FileNotFoundException {
		Message<String> msg = i.getArgument(0);
		MessageHeaders headers = msg.getHeaders();
		assertEquals("FILE_VIEWER", headers.get("topic"));
		return mockResponseMessage(msg, () -> {
			try {
				return new FileInputStream(filePath);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}, 200, MediaType.APPLICATION_OCTET_STREAM);
	}

}
