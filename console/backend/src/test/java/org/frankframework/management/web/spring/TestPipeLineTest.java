package org.frankframework.management.web.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

public class TestPipeLineTest extends FrankApiTestBase<TestPipeline> {

	@Override
	public TestPipeline createSpringMvcResource() {
		return new TestPipeline();
	}

	@Test
	public void testPipeLine() {
		MockMultipartFile dummyMessage = new MockMultipartFile("message", null, "application/xml", "<dummy-message/>".getBytes());

		ResponseEntity<Map<String, String>> responseEntity = springMvcResource.testPipeLine(new TestPipeline.TestPipeLineModel("TestConfiguration", "HelloWorld", null, null, dummyMessage, null));

		String result = responseEntity.getBody().get("result");

		String expected = "{\"result\":{\"topic\":\"TEST_PIPELINE\",\"action\":\"UPLOAD\"},\"state\":\"SUCCESS\",\"message\":\"<dummy-message/>\"}";
		assertEquals(expected, result);
	}

	@Test
	public void testFileMessage() {
		TestPipeline.TestPipeLineModel model = new TestPipeline.TestPipeLineModel("TestConfiguration", "HelloWorld", null, null, null,
				new MockMultipartFile("file", "my-file.xml", "application/xml", "<dummy-message/>".getBytes())
		);

		ResponseEntity<Map<String, String>> responseEntity = springMvcResource.testPipeLine(model);

		String result = responseEntity.getBody().get("result");

		String expected = "{\"result\":{\"topic\":\"TEST_PIPELINE\",\"action\":\"UPLOAD\"},\"state\":\"SUCCESS\",\"message\":\"<dummy-message/>\"}";
		assertEquals(expected, result);
	}


}
