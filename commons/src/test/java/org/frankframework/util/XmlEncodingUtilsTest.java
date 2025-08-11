package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class XmlEncodingUtilsTest {

	@Test
	public void encodesSpecialCharacters() {
		String input = "<>&\"'\n";
		String expected = "&lt;&gt;&amp;&quot;&#39;&#10;";
		assertEquals(expected, XmlEncodingUtils.encodeChars(input, true));
	}

	@Test
	public void encodeCharsReturnsNullForNullInput() {
		assertNull(XmlEncodingUtils.encodeChars(null, true));
	}

	@Test
	public void encodeCharsDoesNotEncodeNewLinesWhenFlagIsFalse() {
		String input = "\n";
		String expected = "\n";
		assertEquals(expected, XmlEncodingUtils.encodeChars(input, false));
		assertEquals(expected, XmlEncodingUtils.encodeChars(input));
	}

	@Test
	public void encodeCharsEncodesNewLinesWhenFlagIsTrue() {
		String input = "\n";
		String expected = "&#10;";
		assertEquals(expected, XmlEncodingUtils.encodeChars(input, true));
	}

	private static Stream<Arguments> testFileWithXmlDeclaration() {
		return Stream.of(
				Arguments.of(StandardCharsets.ISO_8859_1, null, "iso-8859-1.xml"),
				Arguments.of(StandardCharsets.ISO_8859_1, StandardCharsets.ISO_8859_1, "iso-8859-1.xml"),
				Arguments.of(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8, "iso-8859-1.xml"),

				Arguments.of(StandardCharsets.UTF_8, null, "utf8-with-bom.xml"),
				Arguments.of(StandardCharsets.UTF_8, StandardCharsets.UTF_8, "utf8-with-bom.xml"),
				Arguments.of(StandardCharsets.UTF_8, null, "utf8-without-bom.xml"),

				Arguments.of(StandardCharsets.UTF_8, null, "file-without-xml-declaration.xml"),
				Arguments.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, "file-without-xml-declaration.xml")
		);
	}

	private static Stream<Arguments> testFileWithXmlDeclarationStream() {
		return Stream.of(
				Arguments.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, "utf8-with-bom.xml"),
				Arguments.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, "utf8-without-bom.xml"),
				Arguments.of(StandardCharsets.UTF_8, null, "utf8-without-bom.xml")
		);
	}

	@ParameterizedTest
	@MethodSource("testFileWithXmlDeclaration")
	public void testFileWithXmlDeclarationBytes(Charset expectedCharset, Charset defaultCharset, String resource) throws Exception {
		// Arrange
		URL url = XmlEncodingUtilsTest.class.getResource("/XmlEncodingUtils/" + resource);
		byte[] bytes = StreamUtil.streamToBytes(url.openStream());

		// Act
		final String charset = defaultCharset != null ? defaultCharset.name() : null;
		String xmlContent = XmlEncodingUtils.readXml(bytes, charset);

		// Assert
		assertEquals(new String(bytes, expectedCharset), xmlContent);
	}

	@ParameterizedTest
	@MethodSource({"testFileWithXmlDeclaration", "testFileWithXmlDeclarationStream"})
	public void testFileWithXmlDeclarationStream(Charset expectedCharset, Charset defaultCharset, String resource) throws Exception {
		// Arrange
		URL url = XmlEncodingUtilsTest.class.getResource("/XmlEncodingUtils/" + resource);

		// Act
		final String charset = defaultCharset != null ? defaultCharset.name() : null;
		String xmlContent = XmlEncodingUtils.readXml(url.openStream(), charset);

		// Assert
		assertEquals(StreamUtil.streamToString(url.openStream(), expectedCharset.name()), xmlContent);
	}

	@Test
	public void testInvalidXml() throws Exception {
		// Arrange
		URL url = XmlEncodingUtilsTest.class.getResource("/XmlEncodingUtils/invalid.xml");
		byte[] bytes = StreamUtil.streamToBytes(url.openStream());

		// Act
		Exception ex = assertThrows(IllegalArgumentException.class, () -> XmlEncodingUtils.readXml(bytes, null));

		// Assert
		assertEquals("no valid xml declaration in string [<?xml \n<file>in root of classpath</file>]", ex.getMessage().replace("\r", ""));
	}
}
