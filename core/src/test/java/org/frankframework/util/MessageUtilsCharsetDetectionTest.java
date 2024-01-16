package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestFileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runners.Parameterized.Parameters;

public class MessageUtilsCharsetDetectionTest {

	@Parameters(name= "file [{0}] encoding [{2}]")
	public static List<Object[]> data() {
		return Arrays.asList(new Object[][]{
			{"cp1252.txt", "cp1252 test text ’•†™", Charset.forName("windows-1252")},
			{"iso-8859-1.txt", null, Charset.forName("iso-8859-1")},
			{"iso-8859-2.txt", "Naiveté de style, de langage, de pinceau.", Charset.forName("iso-8859-2")},
			{"utf8-with-bom.txt", "testFile with BOM —•˜›", Charset.forName("utf-8")},
			{"utf8-without-bom.txt", "testFile w/o BOM —•˜›", Charset.forName("utf-8")},
		});
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testCharset(String testFile, String fileContent, Charset expectedCharset) throws Exception {
		URL url = TestFileUtils.getTestFileURL("/Util/MessageUtils/"+testFile);
		assertNotNull(url, "cannot find test file ["+testFile+"]");

		Message message = new UrlMessage(url);
		Charset computedCharset = MessageUtils.computeDecodingCharset(message, 40);

		assertEquals(expectedCharset, computedCharset, "charset mismatch");

		if(fileContent != null) {
			String asString = message.asString(computedCharset == null ? null : computedCharset.name());

			assertEquals(fileContent, asString, "fileContent mismatch");
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testDetection(String testFile, String fileContent, Charset expectedCharset) throws Exception {
		assumeTrue(expectedCharset != Charset.forName("windows-1252"));

		URL url = TestFileUtils.getTestFileURL("/Util/MessageUtils/"+testFile);
		assertNotNull(url, "cannot find test file ["+testFile+"]");

		Message message = new UrlMessage(url);
		String result = message.asString("auto"); //calls asReader();

		assertEquals(expectedCharset.name(), message.getCharset(), "charset mismatch");

		if(fileContent != null) {
			assertEquals(fileContent, result, "fileContent mismatch");
		}
	}
}
