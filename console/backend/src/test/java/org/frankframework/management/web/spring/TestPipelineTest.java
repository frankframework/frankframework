package org.frankframework.management.web.spring;

import java.net.URL;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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
						.content(asJsonString(getTestPipeLineModel()))
						.accept(MediaType.APPLICATION_JSON))
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
						.content(asJsonString(getTestPipeLineModel()))
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
						.content(asJsonString(getTestPipeLineModel()))
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
						.content(asJsonString(getTestPipeLineModel()))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("msg-2.xml: SUCCESS\n"));
	}

	private static TestPipeline.TestPipeLineModel getTestPipeLineModel() {
		return new TestPipeline.TestPipeLineModel("TestConfiguration", "HelloWorld", null, null, null, null);
	}
}
