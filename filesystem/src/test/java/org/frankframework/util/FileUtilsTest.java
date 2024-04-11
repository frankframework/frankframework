package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.filesystem.FileNotFoundException;
import org.frankframework.testutil.TestAssertions;

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
	void testGetFreeFile() throws Exception { //normal file exists but _001 file doesn't
		File res = FileUtils.getFreeFile(getFile("file.txt"));
		assertEquals("file_001.txt", res.getName());
	}

	@Test
	void testGetFreeFile001() throws Exception {
		File res = FileUtils.getFreeFile(new File(getFile(null), "freeFile.txt")); //_001 file exists but normal file doesn't
		assertEquals("freeFile.txt", res.getName());
	}

	@Test
	void testMoveFile() throws Exception {
		File root = getFile(null);
		File toBeMoved = new File(root, "movingFile.txt");
		toBeMoved.createNewFile(); //Make sure it exists
		assertTrue(toBeMoved.exists());

		String result = FileUtils.moveFile(toBeMoved, testFolderPath, true, 0);
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
	void createTempDirectoryTest() throws Exception {
		File f = new File(testFolderPath);
		File file = FileUtils.createTempDirectory(f);
		boolean b = file.exists();
		file.delete();
		assertTrue(b);
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
		assertEquals(2, files.length); // check if there are 2 files persent

		int containsBothFiles = 0; //Stupid way to check file names ... sigh
		for(File file : files) {
			if("file.txt".equals(file.getName()) || "fileToAppend.txt".equals(file.getName())) {
				containsBothFiles++;
			}
		}

		assertEquals(2, containsBothFiles);
	}

	@Test //retrieve the first file from a directory. Alphabetically it should first return 'copyFile'. Add a stability period, and check if it skips the first file
	void testGetFirstFile() throws Exception {
		assumeTrue(TestAssertions.isTestRunningOnWindows());

		long stabilityPeriod = 5000;
		getFile("copyFrom.txt").setLastModified(new Date().getTime()-(stabilityPeriod + 500)); //mark file as stable (add 500ms to the stability period because of HDD latency)
		getFile("copyFile.txt").setLastModified(new Date().getTime()); //update last modified to now, so it fails the stability period

		File directory = getFile(null);
		File file = FileUtils.getFirstFile(directory);
		assertEquals("copyFile.txt", file.getName());
	}

	@Test
	void testAlignForValLengthLeftAlignFillchar() {
		String s = "test";
		String res1 = FileUtils.align(s, 10, true, 'b');
		String res2 = FileUtils.align(s, 2, true, 'b');
		String res3 = FileUtils.align(s, 4, false, 'b');
		assertEquals("testbbbbbb", res1);
		assertEquals("te", res2);
		assertEquals("test", res3);
	}

	@Test
	void testAlignForValLengthRightAlignFillchar() {
		String s = "test";
		String res1 = FileUtils.align(s, 10, false, 'c');
		String res2 = FileUtils.align(s, 2, false, 'c');
		String res3 = FileUtils.align(s, 4, false, 'c');
		assertEquals("cccccctest", res1);
		assertEquals("te", res2);
		assertEquals("test", res3);
	}

	@Test
	void testGetFilledArray() {
		char[] arr = FileUtils.getFilledArray(5, 'a');
		assertEquals("aaaaa", new String(arr));
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

	@Test
	void testEncodeFileName() {
		assertEquals("_ab__5__c.txt", FileUtils.encodeFileName(" ab&@5*(c.txt"));
	}

}
