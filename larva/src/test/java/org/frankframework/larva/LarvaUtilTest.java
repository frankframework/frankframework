package org.frankframework.larva;

import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

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
		assertEquals(MediaType.APPLICATION_XML, message.getContext().getMimeType());
	}

	@Test
	public void testUTF8WithBom() throws IOException {
		Message message = LarvaUtil.readFile(getTestFile("utf8-with-bom.txt"));

		message.asString(); // computes Charset
		assertEquals("UTF-8", message.getCharset());
		assertNull(message.getContext().getMimeType());
	}

	@Test
	public void testUTF8WithoutBom() throws IOException {
		Message message = LarvaUtil.readFile(getTestFile("utf8-without-bom.xml"));

		message.asString(); // computes Charset
		assertEquals("UTF-8", message.getCharset());
	}

	@Test
	public void testJsonFile() throws IOException {
		Message message = LarvaUtil.readFile(getTestFile("dummy.json"));

		assertEqualsIgnoreCRLF("""
				{
					"json": true
				}""", message.asString());

		assertNull(message.getCharset());
		assertEquals(MediaType.APPLICATION_JSON, message.getContext().getMimeType());
	}

	@Test
	public void testPathInsideDirectory() {
		String directory = new File("scenarios").getAbsolutePath();

		assertTrue(LarvaUtil.isPathInsideDirectory(directory, directory));
		assertTrue(LarvaUtil.isPathInsideDirectory(directory + File.separator, directory));
		assertTrue(LarvaUtil.isPathInsideDirectory(directory + File.separator + "sub" + File.separator + "step1.txt", directory));
		assertTrue(LarvaUtil.isPathInsideDirectory("scenarios" + File.separator + "step1.txt", directory));
	}

	@Test
	public void testPathOutsideDirectory() {
		String directory = new File("scenarios").getAbsolutePath();

		assertFalse(LarvaUtil.isPathInsideDirectory(new File("other-scenarios").getAbsolutePath(), directory));
		assertFalse(LarvaUtil.isPathInsideDirectory(directory + "-sibling", directory));
		assertFalse(LarvaUtil.isPathInsideDirectory(directory + File.separator + ".." + File.separator + "step1.txt", directory));
		assertFalse(LarvaUtil.isPathInsideDirectory(new File(directory).getParent(), directory));
	}

	@Test
	public void testPathInsideDirectoryWithoutValues() {
		String directory = new File("scenarios").getAbsolutePath();

		assertFalse(LarvaUtil.isPathInsideDirectory(null, directory));
		assertFalse(LarvaUtil.isPathInsideDirectory("", directory));
		assertFalse(LarvaUtil.isPathInsideDirectory(" ", directory));
		assertFalse(LarvaUtil.isPathInsideDirectory(directory, null));
		assertFalse(LarvaUtil.isPathInsideDirectory(directory, ""));
	}
}
