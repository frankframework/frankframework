package org.frankframework.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.ByteOrderMark;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.StreamUtil;

class JsonUtilTest {

	@Test
	public void testPrettyJson() throws IOException {
		URL input = TestFileUtils.getTestFileURL("/JsonUtil/minified.json");
		String inputString = StreamUtil.resourceToString(input);
		URL expected = TestFileUtils.getTestFileURL("/JsonUtil/prettified.json");
		String expectedString = StreamUtil.resourceToString(expected);
		TestAssertions.assertEqualsIgnoreCRLF(expectedString, JsonUtil.jsonPretty(inputString));
	}

	@Test
	public void testPrettyJsonArray() throws IOException {
		URL input = TestFileUtils.getTestFileURL("/JsonUtil/minifiedJsonArray.json");
		String inputString = StreamUtil.resourceToString(input);
		URL expected = TestFileUtils.getTestFileURL("/JsonUtil/prettifiedJsonArray.json");
		String expectedString = StreamUtil.resourceToString(expected);
		TestAssertions.assertEqualsIgnoreCRLF(expectedString, JsonUtil.jsonPretty(inputString));
	}

	@Test
	public void testJsonPathWithBom() throws Exception {
		// Arrange
		Message input = createInputWithBom();
		JsonPath jsonPath = JsonUtil.compileJsonPath("$.root.a");
		assertNotNull(jsonPath);

		// Act
		Message result = JsonUtil.evaluateJsonPath(jsonPath, input);

		// Assert
		assertEquals("v1", result.asString());
	}

	private static @NonNull Message createInputWithBom() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.writeBytes(ByteOrderMark.UTF_8.getBytes());
		baos.writeBytes("""
				{
				  "root": {
				    "a": "v1"
				  }
				}
				""".getBytes(StandardCharsets.UTF_8));
		return Message.asMessage(baos.toByteArray());
	}

	@Test
	public void testJsonPathSingleValueWithBom() throws Exception {
		// Arrange
		Message input = createInputWithBom();
		JsonPath jsonPath = JsonUtil.compileJsonPath("$.root.a");
		assertNotNull(jsonPath);

		// Act
		String result = JsonUtil.evaluateJsonPathToSingleValue(jsonPath, input);

		// Assert
		assertEquals("v1", result);
	}
}
