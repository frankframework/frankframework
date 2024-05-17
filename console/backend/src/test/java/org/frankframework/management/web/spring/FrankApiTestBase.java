package org.frankframework.management.web.spring;

import org.frankframework.management.bus.message.MessageBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
public abstract class FrankApiTestBase {
	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	protected String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected MockMultipartFile createMockMultipartFile(final String name, final String originalFilename, final byte[] content) {
		return new MockMultipartFile(name, originalFilename, MediaType.MULTIPART_FORM_DATA_VALUE, content);
	}

	protected void testBasicRequest(String url, String topic, String action) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get(url))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("topic").value(topic))
				.andExpect(MockMvcResultMatchers.jsonPath("action").value(action));
	}

	protected <T> Message<T> mockResponseMessage(Message<T> in, Supplier<T> payload, int status, @Nullable MediaType mediaType){
		return new Message<>() {
			@Override
			public T getPayload() {
				return payload.get();
			}

			@Override
			public MessageHeaders getHeaders() {
				Map<String, Object> headers = new HashMap<>(in.getHeaders());
				headers.put("state", "SUCCESS");
				headers.put("meta-state", "SUCCESS");
				headers.put(MessageBase.STATUS_KEY, status);
				headers.put("meta-" + MessageBase.STATUS_KEY, status);
				if(mediaType != null){
					headers.put(MessageBase.MIMETYPE_KEY, mediaType);
					headers.put("meta-" + MessageBase.MIMETYPE_KEY, mediaType);
				} else {
					headers.remove("meta-" + MessageBase.MIMETYPE_KEY);
				}

				return new MessageHeaders(headers);
			}
		};
	}
}
