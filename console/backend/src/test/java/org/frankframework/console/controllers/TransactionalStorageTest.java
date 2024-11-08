package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doCallRealMethod;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.message.StringMessage;

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
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("adapterName", in.getHeaders().get("meta-adapter"));

			return new StringMessage("");
		});

		mockMvc.perform(MockMvcRequestBuilders.multipart("/configurations/configuration/adapters/adapterName/pipes/storageSourceName/stores/error/messages/download")
						.part(new MockPart("messageIds", "1,2,3".getBytes())))
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
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapterName/receivers/receiverName/stores/Error/messages/1"))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals("MESSAGE_BROWSER", capturedRequest.getHeaders().get("topic"));
		assertEquals("1", capturedRequest.getHeaders().get("meta-messageId"));
	}

	@Test
	public void testResendReceiverMessages() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.multipart("/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error")
						.part(new MockPart("messageIds", "1,2,3".getBytes())))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals("MESSAGE_BROWSER", capturedRequest.getHeaders().get("topic"));
		// Async call will be done 3 times (for messageId 1,2, and 3). Here we'll only have access to the last call, which should be 3.
		assertEquals("3", capturedRequest.getHeaders().get("meta-messageId"));
	}

	@Test
	public void testResendReceiverMessagesWithErrors() throws Exception {
		Mockito.doThrow(new RuntimeException("Test Exception"))
				.when(outputGateway)
				.sendAsyncMessage(Mockito.any(Message.class));

		mockMvc.perform(MockMvcRequestBuilders.multipart("/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error")
						.part(new MockPart("messageIds", "1,2,3".getBytes())))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("[ \"Test Exception\", \"Test Exception\", \"Test Exception\" ]"));
	}

	@Test
	public void testChangeProcessState() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.multipart("/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error/move/Done")
						.part(new MockPart("messageIds", "1,2,3".getBytes())))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals("MESSAGE_BROWSER", capturedRequest.getHeaders().get("topic"));
		assertEquals("Error", capturedRequest.getHeaders().get("meta-processState"));
		assertEquals("Done", capturedRequest.getHeaders().get("meta-targetState"));
	}

	@Test
	public void testDeleteReceiverMessage() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.delete("/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error/messages/1"))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals("DELETE", capturedRequest.getHeaders().get("action"));
		assertEquals("1", capturedRequest.getHeaders().get("meta-messageId"));
	}

	@Test
	public void testDeleteReceiverMessages() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE, "/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error")
						.part(new MockPart("messageIds", "1,2,3".getBytes())))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals("DELETE", capturedRequest.getHeaders().get("action"));
		assertEquals("3", capturedRequest.getHeaders().get("meta-messageId"));
	}

	@Test
	public void testDeleteReceiverMessagesWithErrors() throws Exception {
		Mockito.doThrow(new RuntimeException("Test Exception"))
				.when(outputGateway).sendAsyncMessage(Mockito.any(Message.class));

		mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE, "/configurations/configuration/adapters/adapterName/receivers/storageSourceName/stores/Error")
						.part(new MockPart("messageIds", "1,2,3".getBytes())))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("[ \"Test Exception\", \"Test Exception\", \"Test Exception\" ]"));
	}
}
