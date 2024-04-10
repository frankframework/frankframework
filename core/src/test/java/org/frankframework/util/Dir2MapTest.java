package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//Rename to JsonDirectoryInfoTest
public class Dir2MapTest {
	private String basePath;

	@BeforeEach
	public void setup() throws URISyntaxException {
		URL base = Dir2MapTest.class.getResource("/ClassLoader/DirectoryClassLoaderRoot");
		assertNotNull(base, "cannot find root directory");
		basePath = Paths.get(base.toURI()).toString();
	}

	@Test
	public void listFiles() {
		Dir2Map map = new Dir2Map(basePath, "*", false, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[ClassLoaderTestFile, ClassLoaderTestFile.txt, ClassLoaderTestFile.xml, Configuration.xml, fileOnlyOnDirectoryClassPath.txt, fileOnlyOnDirectoryClassPath.xml]", readList(map.getList()));
		assertEquals(8, map.size());
		assertEquals(6, map.getList().size());
	}

	@Test
	public void listFilesAndFolders() {
		Dir2Map map = new Dir2Map(basePath, "*", true, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[.., ClassLoader, ClassLoaderTestFile, ClassLoaderTestFile.txt, ClassLoaderTestFile.xml, Config, Configuration.xml, fileOnlyOnDirectoryClassPath.txt, fileOnlyOnDirectoryClassPath.xml]", readList(map.getList()));
		assertEquals(8, map.size());
		assertEquals(9, map.getList().size());
	}

	@Test
	public void listFilesAndFoldersLimit5() {
		Dir2Map map = new Dir2Map(basePath, "*", true, 5);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[.., ClassLoader, ClassLoaderTestFile, ClassLoaderTestFile.txt, ClassLoaderTestFile.xml, Config]", readList(map.getList()));
		assertEquals(8, map.size());
		assertEquals(6, map.getList().size());
	}

	@Test
	public void listFilesWithWildcard() {
		Dir2Map map = new Dir2Map(basePath, "ClassLoader*", false, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[ClassLoaderTestFile, ClassLoaderTestFile.txt, ClassLoaderTestFile.xml]", readList(map.getList()));
		assertEquals(5, map.size());
		assertEquals(3, map.getList().size());
	}

	@Test
	public void listFilesAndFoldersWithWildcard() {
		Dir2Map map = new Dir2Map(basePath, "ClassLoader*", true, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[.., ClassLoader, ClassLoaderTestFile, ClassLoaderTestFile.txt, ClassLoaderTestFile.xml, Config]", readList(map.getList()));
		assertEquals(5, map.size());
		assertEquals(6, map.getList().size()); //Seems to display everything because the base contains the wildcard
	}

	@Test
	public void listFilesWithDifferentWildcard() {
		Dir2Map map = new Dir2Map(basePath, "fileOnly*", false, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[fileOnlyOnDirectoryClassPath.txt, fileOnlyOnDirectoryClassPath.xml]", readList(map.getList()));
		assertEquals(4, map.size());
		assertEquals(2, map.getList().size());
	}

	@Test
	public void listFilesAndFoldersWithNonExistentWildcard() {
		Dir2Map map = new Dir2Map(basePath, "ik-besta-niet", true, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[.., ClassLoader, Config]", readList(map.getList()));
		assertEquals(2, map.size());
		assertEquals(3, map.getList().size()); //Seems to display everything because the base contains the wildcard
	}

	@Test
	public void listFilesWithSlashExtensionWildcard() {
		Dir2Map map = new Dir2Map(basePath, "ClassLoader*.xml", false, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[ClassLoaderTestFile.xml]", readList(map.getList()));
		assertEquals(3, map.size());
		assertEquals(1, map.getList().size());
	}

	@Test
	public void listFilesAndFoldersWithSlashExtensionWildcard() {
		Dir2Map map = new Dir2Map(basePath, "ClassLoader*.xml", true, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[.., ClassLoader, ClassLoaderTestFile.xml, Config]", readList(map.getList()));
		assertEquals(3, map.size());
		assertEquals(4, map.getList().size());
	}

	@Test
	public void listFilesAndFoldersWithWildcardInSubDirectory() {
		Dir2Map map = new Dir2Map(basePath, "NonDefaultConfiguration.xml", true, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[.., ClassLoader, Config]", readList(map.getList()));
		assertEquals(2, map.size());
		assertEquals(3, map.getList().size());
	}

	@Test
	public void listFilesWithExtensionWildcard() {
		Dir2Map map = new Dir2Map(basePath, "*.xml", true, 100);
		assertTrue(map.getDirectory().contains("ClassLoader/DirectoryClassLoaderRoot"));
		assertEquals("[.., ClassLoader, ClassLoaderTestFile.xml, Config, Configuration.xml, fileOnlyOnDirectoryClassPath.xml]", readList(map.getList()));
		assertEquals(5, map.size());
		assertEquals(6, map.getList().size());
	}

	//Converts a strange map to a sorted string
	private String readList(List<Map<String, Object>> list) {
		return list.stream().map(i -> i.get("name")).sorted().toList().toString();
	}
}
