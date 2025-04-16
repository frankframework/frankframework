package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.Action;

@ContextConfiguration(classes = {WebTestConfiguration.class, ConfigurationStatus.class})
public class ConfigurationStatusTest extends FrankApiTestBase {
	public static Stream<Arguments> updateReceiverOptions() {
		return Stream.of(
				Arguments.of("{\"action\":\"stop\"}", Action.STOPRECEIVER),
				Arguments.of("{\"action\":\"start\"}", Action.STARTRECEIVER),
				Arguments.of("{\"action\":\"incthread\"}", Action.INCTHREADS),
				Arguments.of("{\"action\":\"decthread\"}", Action.DECTHREADS)
		);
	}

	@Test
	public void getAdapters() throws Exception {
		testActionAndTopicHeaders("/adapters", "ADAPTER", "GET");
	}

	@Test
	public void getAdapter() throws Exception {
		testActionAndTopicHeaders("/configurations/configuration/adapters/name", "ADAPTER", "FIND");
	}

	@Test
	public void getAdapterHealth() throws Exception {
		testActionAndTopicHeaders("/configurations/configuration/adapters/name/health", "HEALTH", "GET");
	}

	@Test
	public void updateAdapters() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.put("/adapters")
						.content("{ \"action\": \"start\", \"adapters\": [\"adapter1\", \"adapter2\"] }")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		verify(outputGateway, times(2)).sendAsyncMessage(any());

		assertEquals(Action.STARTADAPTER.name(), capturedRequest.getHeaders().get("meta-action"));
	}

	@Test
	public void updateConfigurationWithAdapters() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.put("/adapters")
						.content("{ \"action\": \"start\", \"adapters\": [\"config1/adapter1\", \"config1/adapter2\"] }")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		verify(outputGateway, times(2)).sendAsyncMessage(any());

		assertEquals("config1", capturedRequest.getHeaders().get("meta-configuration"));
		assertEquals("adapter2", capturedRequest.getHeaders().get("meta-adapter"));
		assertEquals(Action.STARTADAPTER.name(), capturedRequest.getHeaders().get("meta-action"));
	}

	@Test
	public void updateAdaptersWithNoAdapters() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.put("/adapters")
						.content("{ \"action\": \"start\", \"adapters\": [] }")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals(Action.STARTADAPTER.name(), capturedRequest.getHeaders().get("meta-action"));
		assertEquals("*ALL*", capturedRequest.getHeaders().get("meta-adapter"));
	}

	@Test
	public void updateAdaptersWithBadAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/adapters")
						.content("{ \"action\": \"badAction\", \"adapters\": [\"adapter1\", \"adapter2\"] }")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("no or unknown action provided"))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("Bad Request"));
	}

	@Test
	public void updateAdapter() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter")
						.content("{\"action\": \"start\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals(Action.STARTADAPTER.name(), capturedRequest.getHeaders().get("meta-action"));
		assertEquals("adapter", capturedRequest.getHeaders().get("meta-adapter"));

		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter")
						.content("{\"action\": \"stop\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("ok"));

		Message<Object> capturedRequest2 = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals(Action.STOPADAPTER.name(), capturedRequest2.getHeaders().get("meta-action"));
		assertEquals("adapter", capturedRequest2.getHeaders().get("meta-adapter"));
	}

	@Test
	public void updateAdapterWithNonStringAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter")
						.content("{\"action\": 0}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void updateAdapterWithBadAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter")
						.content("{\"action\": \"badAction\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@ParameterizedTest
	@MethodSource("updateReceiverOptions")
	public void updateReceiver(String content, Action expectedBusAction) throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter/receivers/receiver")
						.content(content)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("ok"));

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals(expectedBusAction.name(), capturedRequest.getHeaders().get("meta-action"));
		assertEquals("adapter", capturedRequest.getHeaders().get("meta-adapter"));
		assertEquals("receiver", capturedRequest.getHeaders().get("meta-receiver"));
	}

	@Test
	public void updateReceiverWithNonStringAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter/receivers/receiver")
						.content("{\"action\": 0}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void updateReceiverWithBadAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter/receivers/receiver")
						.content("{\"action\": \"badAction\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void getAdapterFlow() throws Exception {
		testActionAndTopicHeaders("/configurations/configuration/adapters/adapter/flow", "FLOW", "GET");
	}
}
