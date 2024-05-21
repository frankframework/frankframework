package org.frankframework.management.web.spring;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.frankframework.management.bus.message.BinaryMessage;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfig.class, TestPipeline.class})
public class TestPipelineTest extends FrankApiTestBase {

	private static final String DUMMY_MESSAGE = "<dummy-message />";

	private static final String TEST_PIPELINE_ENDPOINT = "/test-pipeline";

	@Test
	public void testPipeLine() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("message", null, DUMMY_MESSAGE.getBytes()))
						.part(getMultiPartParts())
						.characterEncoding("UTF-8")
						.accept(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("{\"topic\":\"TEST_PIPELINE\",\"action\":\"UPLOAD\"}"))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(DUMMY_MESSAGE));
	}

	@Test
	public void testFileMessage() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("file", "my-file.xml", DUMMY_MESSAGE.getBytes()))
						.part(getMultiPartParts())
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("{\"topic\":\"TEST_PIPELINE\",\"action\":\"UPLOAD\"}"))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(DUMMY_MESSAGE));
	}

	@Test
	public void testStoredZipMessage() throws Exception {
		URL zip = org.frankframework.management.web.TestPipelineTest.class.getResource("/TestPipeline/stored.zip");

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
		URL zip = org.frankframework.management.web.TestPipelineTest.class.getResource("/TestPipeline/deflated.zip");

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
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class)))
				.thenAnswer(i -> new BinaryMessage("dummy data".getBytes()));

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.part(getMultiPartParts())
						.file(new MockMultipartFile("message", DUMMY_MESSAGE.getBytes()))
						.accept(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("dummy data"))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(DUMMY_MESSAGE));
	}

	@Test
	void testMessageLineEndingsConversion() throws Exception {
		String input = "\"\n<asdf>asd\rasd\rasd\rasd\rad\rccc\rttt\rbbb\ryyy\ruuu\roooo\r</asdf>\"";
		InputStream message = new ByteArrayInputStream(input.getBytes()); // Stream is needed to test the line endings conversion
		GenericMessage<InputStream> genericMessage = new GenericMessage<>(message, new MessageHeaders(null));

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class)))
				.thenAnswer(i -> genericMessage);

		String expectedValue = "\"\n<asdf>asd\nasd\nasd\nasd\nad\nccc\nttt\nbbb\nyyy\nuuu\noooo\n</asdf>\"";

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.part(getMultiPartParts())
						.file(new MockMultipartFile("message", input.getBytes()))
						.accept(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value(expectedValue))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(expectedValue));

	}

	private static MockPart[] getMultiPartParts() {
		return new MockPart[] {
				new MockPart("configuration", "TestConfiguration".getBytes()),
				new MockPart("adapter", "HelloWorld".getBytes())
		};
	}
}
