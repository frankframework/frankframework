package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;

public class JsonDirectoryInfoTest {
	private String basePath;

	private static final JsonWriterFactory WRITER_FACTORY;

	static {
		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, false);
		WRITER_FACTORY = Json.createWriterFactory(config);
	}

	@BeforeEach
	public void setup() throws URISyntaxException {
		URL base = JsonDirectoryInfoTest.class.getResource("/ClassLoader/DirectoryClassLoaderRoot");
		assertNotNull(base, "cannot find root directory");
		basePath = Paths.get(base.toURI()).toString();
	}

	@Test
	public void listFiles() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "*", false, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFiles.json", json);
	}

	@Test
	public void listFilesAndFolders() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "*", true, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesAndFolders.json", json);
	}

	@Test
	public void listFilesAndFoldersLimit5() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "*", true, 5);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesAndFoldersLimit5.json", json);
	}

	@Test
	public void listFilesWithWildcard() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "ClassLoader*", false, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesWithWildcard.json", json);
	}

	@Test
	public void listFilesAndFoldersWithWildcard() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "ClassLoader*", true, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesAndFoldersWithWildcard.json", json);
	}

	@Test
	public void listFilesWithDifferentWildcard() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "fileOnly*", false, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesWithDifferentWildcard.json", json);
	}

	@Test
	public void listFilesAndFoldersWithNonExistentWildcard() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "ik-besta-niet", true, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesAndFoldersWithNonExistentWildcard.json", json);
	}

	@Test
	public void listFilesWithSlashExtensionWildcard() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "ClassLoader*.xml", false, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesWithSlashExtensionWildcard.json", json);
	}

	@Test
	public void listFilesAndFoldersWithSlashExtensionWildcard() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "ClassLoader*.xml", true, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesAndFoldersWithSlashExtensionWildcard.json", json);
	}

	@Test
	public void listFilesAndFoldersWithWildcardInSubDirectory() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "NonDefaultConfiguration.xml", true, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesAndFoldersWithWildcardInSubDirectory.json", json);
	}

	@Test
	public void listFilesWithExtensionWildcard() throws IOException {
		JsonDirectoryInfo map = new JsonDirectoryInfo(basePath, "*.xml", true, 100);

		String json = jsonToString(map.toJson());
		assertJsonEquals("/Util/JsonDirectoryInfo/listFilesWithExtensionWildcard.json", json);
	}

	@Test
	public void testFolderDoesNotExist() throws IOException {
		IOException e = assertThrows(IOException.class, () -> new JsonDirectoryInfo(basePath+"ik-besta-niet", "*", true, 100));
		assertTrue(e.getMessage().contains("DirectoryClassLoaderRootik-besta-niet] does not exist or is not a valid directory"));
	}

	private void assertJsonEquals(String expectedFile, String jsonResult) throws IOException {
		String expected = TestFileUtils.getTestFile(expectedFile);
		MatchUtils.assertJsonEquals(applyIgnores(expected), applyIgnores(jsonResult));
	}

	private String applyIgnores(String message) {
		String normalizedPath = FilenameUtils.normalize(basePath, true);
		int i = normalizedPath.indexOf("/core/target/");
		String workDir = normalizedPath.substring(0, i);
		return message.replaceAll(workDir, "IGNORE").replaceAll("\\d{8,}", "\"IGNORE\"");
	}

	private String jsonToString(JsonStructure json) {
		Writer writer = new StringWriter();

		try (JsonWriter jsonWriter = WRITER_FACTORY.createWriter(writer)) {
			jsonWriter.write(json);
		}

		return writer.toString();
	}
}
