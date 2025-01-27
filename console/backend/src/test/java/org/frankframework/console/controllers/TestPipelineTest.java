package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.StringMessage;

@ContextConfiguration(classes = {WebTestConfiguration.class, TestPipeline.class})
public class TestPipelineTest extends FrankApiTestBase {

	private static final String DUMMY_MESSAGE = "<dummy-message />";

	private static final String TEST_PIPELINE_ENDPOINT = "/test-pipeline";

	private static MockPart[] getMultiPartParts() {
		return new MockPart[] {
				new MockPart("configuration", "TestConfiguration".getBytes()),
				new MockPart("adapter", "HelloWorld".getBytes())
		};
	}

	@Test
	public void testPipeLine() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(DUMMY_MESSAGE, in.getPayload());
			assertEquals("HelloWorld", in.getHeaders().get("meta-adapter"));

			return in;
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("message", null, DUMMY_MESSAGE.getBytes()))
						.part(getMultiPartParts())
						.characterEncoding("UTF-8")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(DUMMY_MESSAGE));
	}

	@Test
	public void testPipeLineNullMessage() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("", in.getPayload());
			assertEquals("HelloWorld", in.getHeaders().get("meta-adapter"));

			return in;
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("message", null, "".getBytes()))
						.part(getMultiPartParts())
						.characterEncoding("UTF-8")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(""));
	}

	@Test
	public void testWithSessionKeys() throws Exception {
		String sessionKeys = "[{\"key\":\"value\", \"key2\":\"value2\"}]";

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			String sessionKeysHeader = (String) in.getHeaders().get("meta-sessionKeys");

			assertEquals(sessionKeys, sessionKeysHeader);
			return mockResponseMessage(in, () -> "{\"sessionKeys\": " + sessionKeysHeader + "}", 200, MediaType.TEXT_PLAIN);
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("message", null, "".getBytes()))
						.part(new MockPart[]{
								new MockPart("configuration", "TestConfiguration".getBytes()),
								new MockPart("adapter", "HelloWorld".getBytes()),
								new MockPart("sessionKeys", sessionKeys.getBytes()),
						})
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("{\"sessionKeys\": [{\"key\":\"value\", \"key2\":\"value2\"}]}"));
	}

	@Test
	public void testFileMessage() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(DUMMY_MESSAGE, in.getPayload());
			assertEquals("HelloWorld", in.getHeaders().get("meta-adapter"));

			return in;
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("file", "my-file.xml", DUMMY_MESSAGE.getBytes()))
						.part(getMultiPartParts())
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void testStoredZipMessage() throws Exception {
		URL zip = TestPipelineTest.class.getResource("/management/web/TestPipeline/stored.zip");

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("<request uri=\"http://localhost:8080/iaf-test/api/exception\"/>", in.getPayload());
			assertEquals("HelloWorld", in.getHeaders().get("meta-adapter"));

			StringMessage stringMessage = new StringMessage(in.getPayload());
			stringMessage.setHeader("state", "SUCCESS");

			return stringMessage;
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("file", "archive2.zip", zip.openStream().readAllBytes()))
						.part(getMultiPartParts())
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("msg-7.xml: SUCCESS\n"));
	}

	@Test
	public void testDeflatedZipMessage() throws Exception {
		URL zip = TestPipelineTest.class.getResource("/management/web/TestPipeline/deflated.zip");

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("<request uri=\"http://localhost:8080/iaf-test/api/exception\"/>", in.getPayload());
			assertEquals("HelloWorld", in.getHeaders().get("meta-adapter"));

			StringMessage stringMessage = new StringMessage(in.getPayload());
			stringMessage.setHeader("state", "SUCCESS");

			return stringMessage;
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("file", "archive3.zip", zip.openStream().readAllBytes()))
						.part(getMultiPartParts())
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("msg-2.xml: SUCCESS\n"));
	}

	@Test
	public void testPipelineBinaryResponse() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(DUMMY_MESSAGE, in.getPayload());
			assertEquals("HelloWorld", in.getHeaders().get("meta-adapter"));

			return new BinaryMessage("dummy data".getBytes());
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.part(getMultiPartParts())
						.file(new MockMultipartFile("message", DUMMY_MESSAGE.getBytes()))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("dummy data"))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(DUMMY_MESSAGE));
	}

	@Test
	void testMessageLineEndingsConversion() throws Exception {
		String input = "\"\n<asdf>asd\rasd\rasd\rasd\rad\rccc\rttt\rbbb\ryyy\ruuu\roooo\r</asdf>\"";
		String expectedValue = "\"\n<asdf>asd\nasd\nasd\nasd\nad\nccc\nttt\nbbb\nyyy\nuuu\noooo\n</asdf>\"";

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(expectedValue, in.getPayload());
			assertEquals("HelloWorld", in.getHeaders().get("meta-adapter"));

			InputStream message = new ByteArrayInputStream(input.getBytes()); // Stream is needed to test the line endings conversion
			return new GenericMessage<>(message, new MessageHeaders(null));
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.part(getMultiPartParts())
						.file(new MockMultipartFile("message", input.getBytes()))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value(expectedValue))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(expectedValue));
	}
}
