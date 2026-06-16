package org.frankframework.console.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import org.frankframework.console.ApiException;

public class RequestUtilsTest {

	@Test
	public void testResolveRequiredProperty() throws Exception {
		assertEquals("value", RequestUtils.resolveRequiredProperty("testKey", "value", "default"));
		assertEquals("", RequestUtils.resolveRequiredProperty("testKey", "", "default"));
		assertEquals("default", RequestUtils.resolveRequiredProperty("testKey", null, "default"));
		ApiException ex = assertThrows(ApiException.class, () -> RequestUtils.resolveRequiredProperty("testKey", null, null));
		assertEquals("Key [testKey] not defined", ex.getMessage());
	}

	@Test
	public void testResolveStringWithISO88591Encoding() throws IOException {
		// Arrange
		URL testFile = RequestUtilsTest.class.getResource("/files-with-charset/iso-8859-1.txt");
		assertNotNull(testFile, "unable to find testfile");

		MultipartFile file = new MockMultipartFile("name", testFile.openStream());

		String expected = readFile(testFile, StandardCharsets.ISO_8859_1);
		// Act + Assert
		assertNotEquals(expected, RequestUtils.resolveStringWithEncoding("testkey-ignored", file, null, false));
		assertNotEquals(expected, RequestUtils.resolveStringWithEncoding("testkey-ignored", file, "", false));
		assertNotEquals(expected, RequestUtils.resolveStringWithEncoding("testkey-ignored", file, "utf-8", false));
		assertEquals(expected, RequestUtils.resolveStringWithEncoding("testkey-ignored", file, "iso-8859-1", false));
	}

	@Test
	public void testResolveStringWithISO88591ContentType() throws IOException {
		// Arrange
		URL testFile = RequestUtilsTest.class.getResource("/files-with-charset/iso-8859-1.txt");
		assertNotNull(testFile, "unable to find testfile");

		MediaType contentType = new MediaType("text", "plain", StandardCharsets.ISO_8859_1);
		MultipartFile file = new MockMultipartFile("name", "filename.txt", contentType.toString(), testFile.openStream());

		String expected = readFile(testFile, StandardCharsets.ISO_8859_1);
		// Act + Assert with file with ContentType
		// 3rd parameter is ignored when a charset is provided.
		assertEquals(expected, RequestUtils.resolveStringWithEncoding("testkey-ignored", file, null, false));
		assertEquals(expected, RequestUtils.resolveStringWithEncoding("testkey-ignored", file, "", false));
		assertEquals(expected, RequestUtils.resolveStringWithEncoding("testkey-ignored", file, "utf-8", false));
		assertEquals(expected, RequestUtils.resolveStringWithEncoding("testkey-ignored", file, "iso-8859-1", false));
	}

	@Test
	public void testResolveEmptyString() throws IOException {
		assertEquals("", RequestUtils.resolveStringWithEncoding("testkey-ignored", new MockMultipartFile("name", new ByteArrayInputStream("".getBytes())), null, false));
		assertNull(RequestUtils.resolveStringWithEncoding("testkey-ignored", new MockMultipartFile("name", new ByteArrayInputStream("".getBytes())), null, true));
		assertNull(RequestUtils.resolveStringWithEncoding("testkey-ignored", null, null, true));
	}

	private static String readFile(URL resource, Charset charset) throws IOException {
		assertNotNull(resource, "unable to find resource");
		try (Reader reader = new InputStreamReader(resource.openStream(), charset)) {
			return IOUtils.toString(reader);
		}
	}
}
