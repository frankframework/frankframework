package org.frankframework.console.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.frankframework.management.bus.message.MessageBase;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
public abstract class FrankApiTestBase {
	protected MockMvc mockMvc;

	@Autowired
	protected WebApplicationContext webApplicationContext;

	@Autowired
	protected SpringUnitTestLocalGateway outputGateway;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@AfterEach
	public void afterEach() {
		Mockito.reset(outputGateway);
	}

	protected MockMultipartFile createMockMultipartFile(final String name, final String originalFilename, final byte[] content) {
		return new MockMultipartFile(name, originalFilename, MediaType.MULTIPART_FORM_DATA_VALUE, content);
	}

	protected void testActionAndTopicHeaders(String url, String topic, String action) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get(url))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("topic").value(topic))
				.andExpect(MockMvcResultMatchers.jsonPath("action").value(action));
	}

	protected void testActionAndTopicHeaders(String url, String topic, String action, Object... urlParams) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get(url, urlParams))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("topic").value(topic))
				.andExpect(MockMvcResultMatchers.jsonPath("action").value(action));
	}

	protected <T, U> Message<U> mockResponseMessage(Message<T> in, Supplier<U> payload, int status, @Nullable MediaType mediaType){
		return new Message<>() {
			@Override
			public U getPayload() {
				return payload.get();
			}

			@Override
			public MessageHeaders getHeaders() {
				Map<String, Object> headers = new HashMap<>(in.getHeaders());
				headers.put("state", "SUCCESS");
				headers.put("meta-state", "SUCCESS");
				headers.put(MessageBase.STATUS_KEY, status);
				headers.put("meta-" + MessageBase.STATUS_KEY, status);
				if (mediaType != null) {
					headers.put(MessageBase.MIMETYPE_KEY, mediaType);
					headers.put("meta-" + MessageBase.MIMETYPE_KEY, mediaType);
				}

				return new MessageHeaders(headers);
			}
		};
	}

	protected class DefaultSuccessAnswer implements Answer<Message<String>> {
		@Override
		public Message<String> answer(InvocationOnMock invocation) {
			Message<String> in = invocation.getArgument(0);
			return mockResponseMessage(in, in::getPayload, 200, MediaType.APPLICATION_JSON);
		}
	}
}
