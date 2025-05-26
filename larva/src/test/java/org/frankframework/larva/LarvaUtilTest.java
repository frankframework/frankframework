package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;

import org.frankframework.stream.Message;

public class LarvaUtilTest {

	private String getTestFile(String filename) {
		URL url = LarvaUtilTest.class.getResource("/testFiles/"+filename);
		assertNotNull(url);
		return FilenameUtils.normalize(url.toExternalForm(), true).replaceAll("file:", "");
	}

	@Test
	public void testISOFilename() throws IOException {
		Message message = LarvaUtil.readFile(getTestFile("iso-8859-1.ISO-8859-1"));

		message.asString(); // computes Charset
		assertEquals("ISO-8859-1", message.getCharset());
	}

	@Test
	public void testXmlEncoding() throws IOException {
		Message message = LarvaUtil.readFile(getTestFile("iso-8859-1.xml"));

		message.asString(); // computes Charset
		assertEquals("ISO-8859-1", message.getCharset());
	}

	@Test
	public void testUTF8WithBom() throws IOException {
		Message message = LarvaUtil.readFile(getTestFile("utf8-with-bom.txt"));

		message.asString(); // computes Charset
		assertEquals("UTF-8", message.getCharset());
	}

	@Test
	public void testUTF8WithoutBom() throws IOException {
		Message message = LarvaUtil.readFile(getTestFile("utf8-without-bom.xml"));

		message.asString(); // computes Charset
		assertEquals("UTF-8", message.getCharset());
	}
}
