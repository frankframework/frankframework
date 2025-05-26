package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.MimeType;

import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestFileUtils;

class MessageUtilsCharsetDetectionTest {

	private static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of("cp1252.txt", "cp1252 test text ’•†™", Charset.forName("windows-1252")),
				Arguments.of("iso-8859-1.txt", null, StandardCharsets.ISO_8859_1),
				Arguments.of("iso-8859-2.txt", "Naiveté de style, de langage, de pinceau.", Charset.forName("iso-8859-2")),
				Arguments.of("utf8-with-bom.txt", "testFile with BOM —•˜›", StandardCharsets.UTF_8),
				Arguments.of("utf8-without-bom.txt", "testFile w/o BOM —•˜›", StandardCharsets.UTF_8
				));
	}

	@ParameterizedTest
	@MethodSource("data")
	void testCharset(String testFile, String fileContent, Charset expectedCharset) throws Exception {
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
	void testDetection(String testFile, String fileContent, Charset expectedCharset) throws Exception {
		assumeTrue(expectedCharset != Charset.forName("windows-1252"));

		URL url = TestFileUtils.getTestFileURL("/Util/MessageUtils/"+testFile);
		assertNotNull(url, "cannot find test file ["+testFile+"]");

		Message message = spy(new UrlMessage(url));
		String result = message.asString("auto"); // calls asReader();

		verify(message, times(1)).asReader("auto");
		assertEquals(expectedCharset.name(), message.getCharset(), "charset mismatch");

		if(fileContent != null) {
			assertEquals(fileContent, result, "fileContent mismatch");
		}
	}

	// Prove Charset can be read from XML header.
	@Test
	public void testComputeMimeTypeWithEncodingFromXml() {
		URL url = ClassLoaderUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.xml");
		Message message = new UrlMessage(url);
		message.getContext().put(MessageContext.METADATA_CHARSET, "auto");

		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertEquals("ISO-8859-1", message.getCharset());
		assertTrue(mimeType.toString().contains("application/xml"), "Content-Type header ["+mimeType.toString()+"]");
		assertTrue(mimeType.toString().contains("charset=ISO-8859-1"), "Content-Type header ["+mimeType.toString()+"]");
	}
}
