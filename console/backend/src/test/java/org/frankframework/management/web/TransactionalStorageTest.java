package org.frankframework.management.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.frankframework.management.bus.message.JsonMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, TransactionalStorage.class})
public class TransactionalStorageTest extends FrankApiTestBase {

	@Test
	public void testBrowseMessage() throws Exception {
		testActionAndTopicHeaders(
				"/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/error/messages/1",
				"MESSAGE_BROWSER",
				"GET"
		);
	}

	@Test
	public void testDownloadMessage() throws Exception {
		testActionAndTopicHeaders(
				"/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/error/messages/1/download",
				"MESSAGE_BROWSER",
				"DOWNLOAD"
		);
	}

	@Test
	public void testDownloadMessages() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.multipart("/configurations/configuration/adapters/adapterName/pipes/storageSourceName/stores/error/messages/download")
						.part(
								new MockPart("messageIds", "1,2,3".getBytes())
						))
				.andExpect(request().asyncStarted())
				.andDo(MvcResult::getAsyncResult)
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_OCTET_STREAM));
	}

	@Test
	public void testBrowseMessages() throws Exception {
		testActionAndTopicHeaders(
				"/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/error",
				"MESSAGE_BROWSER",
				"FIND"
		);
	}

	@Test
	public void testResendReceiverMessage() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			Map<String, Object> headers = new HashMap<>(msg.getHeaders());
			return new JsonMessage(headers);
		});

		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapterName/receivers/receiverName/stores/Error/messages/1"))
				.andExpect(MockMvcResultMatchers.status().isOk());
//				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
//				.andExpect(MockMvcResultMatchers.jsonPath("topic").value("MESSAGE_BROWSER"))
//				.andExpect(MockMvcResultMatchers.jsonPath("action").value("STATUS"));
	}

	@Test
	public void testResendReceiverMessages() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.multipart("/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error")
						.part(
								new MockPart("messageIds", "1,2,3".getBytes())
						))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void testChangeProcessState() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.multipart("/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error/move/Done")
						.part(
								new MockPart("messageIds", "1,2,3".getBytes())
						))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void testDeleteReceiverMessage() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error/messages/1"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void testDeleteReceiverMessages() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE, "/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error")
						.part(
								new MockPart("messageIds", "1,2,3".getBytes())
						))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

}
