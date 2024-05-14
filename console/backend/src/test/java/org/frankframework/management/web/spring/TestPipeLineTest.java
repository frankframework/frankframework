package org.frankframework.management.web.spring;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.frankframework.management.web.TestPipelineTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebAppConfiguration
@ContextConfiguration(classes = {WebTestConfig.class, TestPipeline.class})
@ExtendWith(SpringExtension.class)
public class TestPipeLineTest {

	private static final String DUMMY_MESSAGE = "<dummy-message />";

	private static final String RESULT_EXPRESSION = "result";

	private static final String TEST_PIPELINE_ENDPOINT = "/test-pipeline";

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void testPipeLine() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("message", null, DUMMY_MESSAGE.getBytes()))
						.content(asJsonString(getTestPipeLineModel()))
						.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath(RESULT_EXPRESSION).value("{\"topic\":\"TEST_PIPELINE\",\"action\":\"UPLOAD\"}"))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(DUMMY_MESSAGE))
				.andReturn();
	}

	@Test
	public void testFileMessage() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("file", "my-file.xml", DUMMY_MESSAGE.getBytes()))
						.content(asJsonString(getTestPipeLineModel()))
						.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath(RESULT_EXPRESSION).value("{\"topic\":\"TEST_PIPELINE\",\"action\":\"UPLOAD\"}"))
				.andExpect(MockMvcResultMatchers.jsonPath("message").value(DUMMY_MESSAGE))
				.andReturn();
	}

	@Test
	public void testStoredZipMessage() throws Exception {
		URL zip = TestPipelineTest.class.getResource("/TestPipeline/stored.zip");

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("file", "archive2.zip", zip.openStream().readAllBytes()))
						.content(asJsonString(getTestPipeLineModel()))
						.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath(RESULT_EXPRESSION).value("msg-7.xml: SUCCESS\n"))
				.andReturn();
	}

	@Test
	public void testDeflatedZipMessage() throws Exception {
		URL zip = TestPipelineTest.class.getResource("/TestPipeline/deflated.zip");

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_PIPELINE_ENDPOINT)
						.file(createMockMultipartFile("file", "archive3.zip", zip.openStream().readAllBytes()))
						.content(asJsonString(getTestPipeLineModel()))
						.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath(RESULT_EXPRESSION).value("msg-2.xml: SUCCESS\n"))
				.andReturn();
	}

	private String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private MockMultipartFile createMockMultipartFile(final String name, final String originalFilename, final byte[] content) {
		return new MockMultipartFile(name, originalFilename, MediaType.MULTIPART_FORM_DATA_VALUE, content);
	}

	private static TestPipeline.TestPipeLineModel getTestPipeLineModel() {
		return new TestPipeline.TestPipeLineModel("TestConfiguration", "HelloWorld", null, null, null, null);
	}
}
