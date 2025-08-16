package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilsTest {

	@TempDir
	public static Path testFolder;

	public static String testFolderPath;

	@BeforeAll
	public static void setUpTest() {
		testFolderPath = testFolder.toString();
	}

	private File getFile(String fileName) throws FileNotFoundException {
		String base = "/Util/FileUtils/";
		URL pipes = this.getClass().getResource(base);
		assertNotNull(pipes, "unable to find base ["+ base +"]");

		File root = new File(pipes.getPath());
		if(fileName == null) {
			return root;
		}
		File child = new File(root, fileName);
		if(!child.exists()) {
			throw new FileNotFoundException("unable to find file ["+fileName+"] in folder ["+root.toPath()+"]");
		}
		return child;
	}

	@Test
	void testGetFreeFile() throws Exception { // Normal file exists but _001 file doesn't
		File res = FileUtils.getFreeFile(getFile("file.txt"));
		assertEquals("file_001.txt", res.getName());
	}

	@Test
	void testGetFreeFile001() throws Exception {
		File res = FileUtils.getFreeFile(new File(getFile(null), "freeFile.txt")); // _001 file exists but normal file doesn't
		assertEquals("freeFile.txt", res.getName());
	}

	@Test
	void testMoveFile() throws Exception {
		File root = getFile(null);
		File toBeMoved = new File(root, "movingFile.txt");
		toBeMoved.createNewFile(); // Make sure it exists
		assertTrue(toBeMoved.exists());

		File dstFile = new File(testFolderPath, toBeMoved.getName());
		String result = FileUtils.moveFile(toBeMoved, dstFile, true, 0, 5, 500);
		File file = new File(testFolderPath, "movingFile.txt");
		assertTrue(file.exists());

		assertEquals(file.getPath(), result);
	}

	@Test
	void testCopyFile() throws Exception {
		File sourceFile = getFile("fileToAppend.txt");
		File destFile = getFile("copyFile.txt");
		assertTrue(FileUtils.copyFile(sourceFile, destFile, true));
	}

	@Test
	void testRollOver() throws Exception {
		File f1 = Files.createFile(testFolder.resolve("testfile.txt")).toFile();
		FileUtils.makeBackups(f1, 1);
		File rolloverOne = new File(testFolder.toString(), "testfile.txt.1");
		assertTrue(rolloverOne.exists());

		File f2 = Files.createFile(testFolder.resolve("testfile2.txt")).toFile();
		FileUtils.makeBackups(f2, 1);
		File rolloverTwo = new File(testFolder.toString(), "testfile2.txt.1");
		assertTrue(rolloverTwo.exists());
	}

	@Test
	void testRollOverTwice() throws Exception {
		File f1 = Files.createFile(testFolder.resolve("testfile2.txt")).toFile();
		FileUtils.makeBackups(f1, 2);
		File rolloverOne = new File(testFolder.toString(), "testfile2.txt.1");
		assertTrue(rolloverOne.exists());

		File f2 = Files.createFile(testFolder.resolve("testfile2.txt")).toFile();
		FileUtils.makeBackups(f2, 2);
		File rolloverTwo = new File(testFolder.toString(), "testfile2.txt.2");
		assertTrue(rolloverTwo.exists());

		File f3 = Files.createFile(testFolder.resolve("testfile2.txt")).toFile();
		FileUtils.makeBackups(f3, 2);
		File rolloverThree = new File(testFolder.toString(), "testfile2.txt.3");
		assertFalse(rolloverThree.exists());
	}

	@Test
	void testGetFilesWithWildcard() throws Exception {
		String directory = getFile(null).getPath();
		File[] files = FileUtils.getFiles(directory, "file*", null, 5);
		assertEquals(2, files.length); // Check if there are 2 files persent

		int containsBothFiles = 0; // Stupid way to check file names ... sigh
		for(File file : files) {
			if("file.txt".equals(file.getName()) || "fileToAppend.txt".equals(file.getName())) {
				containsBothFiles++;
			}
		}

		assertEquals(2, containsBothFiles);
	}

	@Test
	void testGetFileNameExtension() {
		String ext = FileUtils.getFileNameExtension("file.blaaaathowdiaa");
		assertEquals("blaaaathowdiaa", ext);
	}

	@Test
	void testGetFileNameWithoutExtension() {
		String ext = FileUtils.getFileNameExtension("file-blaaaathowdiaa");
		assertNull(ext);
	}

	@Test
	void testGetBaseName() {
		String name = FileUtils.getBaseName("file.blaaaathowdiaa");
		assertEquals("file", name);
	}

	@Test
	void testGetBaseNameWithoutExtension() {
		String name = FileUtils.getBaseName("file-blaaaathowdiaa");
		assertNull(name);
	}

}
