package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.message.StringMessage;

@ContextConfiguration(classes = {WebTestConfiguration.class, UpdateLoggingConfig.class})
public class UpdateLoggingConfigTest extends FrankApiTestBase {

	public static Stream<Arguments> updateLogDefinitionSource() {
		return Stream.of(
				Arguments.of("{\"level\":\"WARN\"}", "meta-level", "WARN"),
				Arguments.of("{\"logger\":\"package\"}", "meta-logPackage", "package"),
				Arguments.of("{\"reconfigure\":\"true\"}", "meta-reconfigure", Boolean.TRUE));
	}

	public static Stream<Arguments> updateLogSource() {
		return Stream.of(
				Arguments.of("{\"logIntermediaryResults\":\"true\"}", "meta-level", Boolean.TRUE),
				Arguments.of("{\"maxMessageLength\":50}", "meta-maxMessageLength", 50),
				Arguments.of("{\"enableDebugger\":\"true\"}", "meta-enableDebugger", Boolean.TRUE),
				Arguments.of("{\"logLevel\":\"dummy\"}", "meta-logLevel", "dummy"));
	}

	public static Stream<Arguments> createLogDefinitionSource() {
		return Stream.of(
				Arguments.of(new MockPart[]{
					new MockPart("level", "WARN".getBytes()),
					new MockPart("logger", "package".getBytes())
				}, "meta-level", "WARN", MockMvcResultMatchers.status().isOk()),
				Arguments.of(new MockPart[]{
						new MockPart("level", "WARN".getBytes()),
						new MockPart("logger", "package".getBytes())
				}, "meta-logPackage", "package", MockMvcResultMatchers.status().isOk()),
				Arguments.of(new MockPart[]{
						new MockPart("level", "WARN".getBytes())
				}, null, null, MockMvcResultMatchers.status().isBadRequest()),
				Arguments.of(new MockPart[]{
						new MockPart("logger", "package".getBytes())
				}, null, null, MockMvcResultMatchers.status().isBadRequest())
		);
	}

	@Test
	void testGetLogConfiguration() throws Exception {
		this.testActionAndTopicHeaders("/server/logging", "LOG_CONFIGURATION", "GET");
	}

	@Test
	void testGetLogDefinitions() throws Exception {
		this.testActionAndTopicHeaders("/server/logging/settings", "LOG_DEFINITIONS", "GET");
	}

	@ParameterizedTest
	@MethodSource("updateLogDefinitionSource")
	void testUpdateLogDefinition(String content, String expectedProperty, Object expectedValue) throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(expectedValue, in.getHeaders().get(expectedProperty));

			return new StringMessage("");
		});

		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging/settings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(content))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@ParameterizedTest
	@MethodSource("updateLogSource")
	public void updateLogIntermediaryResults(String content, String expectedProperty, Object expectedValue) throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(expectedValue, in.getHeaders().get(expectedProperty));

			return new StringMessage("");
		});

		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content(content))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@ParameterizedTest
	@MethodSource("createLogDefinitionSource")
	void testCreateLogDefinition(MockPart[] content, String expectedProperty, Object expectedValue, ResultMatcher expectedStatus) throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(expectedValue, in.getHeaders().get(expectedProperty));

			return new StringMessage("");
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart("/server/logging/settings")
						.part(content)
						.contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
				.andExpect(expectedStatus);
	}
}
