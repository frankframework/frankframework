package org.frankframework.json;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

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

}
