package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.nn.adapterframework.filesystem.FileNotFoundException;
import nl.nn.adapterframework.testutil.TestAssertions;

public class FileUtilsTest {

	@TempDir
	public static Path testFolder;

	public static String testFolderPath;

	@BeforeEach
	public void setUp() {
		testFolderPath = testFolder.toString();
	}

	private File getFile(String fileName) throws FileNotFoundException {
		String BASE = "/Util/FileUtils/";
		URL pipes = this.getClass().getResource(BASE);
		assertNotNull(pipes, "unable to find base [" + BASE + "]");

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
	public void testGetFreeFile() throws Exception { //normal file exists but _001 file doesn't
		File res = FileUtils.getFreeFile(getFile("file.txt"));
		assertEquals("file_001.txt", res.getName());
	}

	@Test
	public void testGetFreeFile001() throws Exception {
		File res = FileUtils.getFreeFile(new File(getFile(null), "freeFile.txt")); //_001 file exists but normal file doesn't
		assertEquals("freeFile.txt", res.getName());
	}

	@Test
	public void testMoveFile() throws Exception {
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
	public void testAppendFile() throws Exception {
		File orgFile = getFile("fileToAppend.txt");
		File destFile = getFile("destinationFile.txt");
		String res = FileUtils.appendFile(orgFile, destFile, 5, 5000);
		assertEquals(destFile.getAbsolutePath(), res);
	}

	@Test
	public void testCopyFile() throws Exception {
		File sourceFile = getFile("fileToAppend.txt");
		File destFile = getFile("copyFile.txt");
		boolean b = FileUtils.copyFile(sourceFile, destFile, true);
		assertEquals(true, b);
	}

	@Test
	public void createTempDirectoryTest() throws Exception {
		File f = new File(testFolderPath);
		File file = FileUtils.createTempDirectory(f);
		boolean b = file.exists();
		file.delete();
		assertTrue(b);
	}

	@Test
	public void testRollOver() throws Exception {
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
	public void testRollOverTwice() throws Exception {
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
	public void testGetFilesWithWildcard() throws Exception {
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
	public void testGetFirstFile() throws Exception {
		assumeTrue(TestAssertions.isTestRunningOnWindows());

		long stabilityPeriod = 5000;
		getFile("copyFrom.txt").setLastModified(new Date().getTime()-(stabilityPeriod + 500)); //mark file as stable (add 500ms to the stability period because of HDD latency)
		getFile("copyFile.txt").setLastModified(new Date().getTime()); //update last modified to now, so it fails the stability period

		File directory = getFile(null);
		File file = FileUtils.getFirstFile(directory);
		assertEquals("copyFile.txt", file.getName());

		File file2 = FileUtils.getFirstFile(directory.getPath(), stabilityPeriod); //Run again with 5 second stability period
		assertEquals("copyFrom.txt", file2.getName());
	}

	@Test
	public void testGetFirstFileDirectory() throws Exception {
		Files.createFile(testFolder.resolve("myFile.txt"));
		File file = FileUtils.getFirstFile(testFolder.toString(), 50000000);
		assertNull(file);
	}

	@Test
	public void testGetListFromNamesForNames() {
		List<String> list = FileUtils.getListFromNames("abc.txt,test.txt,jkl.txt", ',');
		assertEquals("[abc.txt, test.txt, jkl.txt]", list.toString());
	}

	@Test
	public void testGetListFromNamesNames() {
		String[] names = {"abc.txt", "test.txt", "jkl.txt"};
		List<String> list = FileUtils.getListFromNames(names);
		assertEquals("[abc.txt, test.txt, jkl.txt]", list.toString());
	}

	@Test
	public void testGetNamesFromArray() {
		String[] names = {"abc.txt", "test.txt", "jkl.txt"};
		String res = FileUtils.getNamesFromArray(names, ',');
		assertEquals("abc.txt,test.txt,jkl.txt", res);
	}

	@Test
	public void testGetNamesFromList() {
		String[] names = {"abc.txt", "test.txt", "jkl.txt"};
		List<String> list = FileUtils.getListFromNames(names);
		String res = FileUtils.getNamesFromList(list, '*');
		assertEquals("abc.txt*test.txt*jkl.txt", res);
	}

	@Test
	public void testAlignForValLengthLeftAlignFillchar() {
		String s = "test";
		String res1 = FileUtils.align(s, 10, true, 'b');
		String res2 = FileUtils.align(s, 2, true, 'b');
		String res3 = FileUtils.align(s, 4, false, 'b');
		assertEquals("testbbbbbb", res1);
		assertEquals("te", res2);
		assertEquals("test", res3);
	}

	@Test
	public void testAlignForValLengthRightAlignFillchar() {
		String s = "test";
		String res1 = FileUtils.align(s, 10, false, 'c');
		String res2 = FileUtils.align(s, 2, false, 'c');
		String res3 = FileUtils.align(s, 4, false, 'c');
		assertEquals("cccccctest", res1);
		assertEquals("te", res2);
		assertEquals("test", res3);
	}

	@Test
	public void testGetFilledArray() {
		char[] arr = FileUtils.getFilledArray(5, 'a');
		assertEquals("aaaaa", new String(arr));
	}

	@Test
	public void testGetFileNameExtension() {
		String ext = FileUtils.getFileNameExtension("file.blaaaathowdiaa");
		assertEquals("blaaaathowdiaa", ext);
	}

	@Test
	public void testGetFileNameWithoutExtension() {
		String ext = FileUtils.getFileNameExtension("file-blaaaathowdiaa");
		assertNull(ext);
	}

	@Test
	public void testGetBaseName() {
		String name = FileUtils.getBaseName("file.blaaaathowdiaa");
		assertEquals("file", name);
	}

	@Test
	public void testGetBaseNameWithoutExtension() {
		String name = FileUtils.getBaseName("file-blaaaathowdiaa");
		assertNull(name);
	}

	@Test
	public void testExtensionEqualsIgnoreCase() {
		assertTrue(FileUtils.extensionEqualsIgnoreCase("a.txT", "txt"));
		assertFalse(FileUtils.extensionEqualsIgnoreCase("b.ABT", "txt"));
	}

	@Test
	public void testCanWrite() throws Exception {
		String file = getFile("copyFile.txt").getPath();
		String directory = getFile(null).getPath();

		assertTrue(FileUtils.canWrite(directory));
		assertFalse(FileUtils.canWrite(file));
	}

	@Test
	public void testEncodeFileName() {
		assertEquals("_ab__5__c.txt", FileUtils.encodeFileName(" ab&@5*(c.txt"));
	}

	@Test
	public void testIsFileBinaryEqual() throws Exception {
		File file1 = getFile("file.txt");
		File file2 = getFile("copyFile.txt");

		assertTrue(FileUtils.isFileBinaryEqual(file1, file1));
		assertFalse(FileUtils.isFileBinaryEqual(file1, file2));
	}

	public File getRollingFile(String dir, int year, int month, int day) {
		Date date = new GregorianCalendar(year, month, day).getTime();
		return FileUtils.getRollingFile(dir, "", FileUtils.WEEKLY_ROLLING_FILENAME_DATE_FORMATTER, "", 0, date);
	}
	// Helper method to verify the filename
	public boolean testWeeklyRollingFilename(String name) {
		String[] nsplit = name.split("W");
		if(Integer.valueOf(nsplit[1]) > 50) { // in case the week number is greater than 50 then the year must be 2020
			return "2020".equals(nsplit[0]);
		}
		return "2021".equals(nsplit[0]);
	}

	@Test
	public void testWeeklyRollingFilenameForTheLastWeekOfOldYear() {
		// The Last week of old year
		assertTrue(testWeeklyRollingFilename(getRollingFile("", 2020, 11, 31).getName()));
	}

	@Test
	public void testWeeklyRollingFilenameForTheWeekBeforeTheLastWeekOfOldYear() {
		// The week before the last week of old year
		assertTrue(testWeeklyRollingFilename(getRollingFile("", 2020, 11, 25).getName()));
	}

	@Test
	public void testWeeklyRollingFilenameForTheLastDayOfTheWeekBeforeTheLastWeekOfOldYear() {
		// The last day of the week before the last week of old year
		assertTrue(testWeeklyRollingFilename(getRollingFile("", 2020, 11, 27).getName()));
	}

	@Test
	public void testWeeklyRollingFilenameForFewDaysOfTheNewYearFromTheLastWeekOfOldYear() {
		// Few days of the new year which are also in the last week of the old year
		assertTrue(testWeeklyRollingFilename(getRollingFile("", 2021, 0, 3).getName()));
	}

	@Test
	public void testWeeklyRollingFilenameForTheFirstDayOfTheNewYear() {
		// The first day of the first week of the new year
		assertTrue(testWeeklyRollingFilename(getRollingFile("", 2021, 0, 4).getName()));
	}

	@Test
	public void testWeeklyRollingFilenameForAdayFromTheSecondWeekOfTheNewYear() {
		// The first day of the first week of the new year
		assertTrue(testWeeklyRollingFilename(getRollingFile("", 2021, 0, 11).getName()));
	}

	@Test
	public void testGetRollingFileDeleteExisting() throws IOException {
		File tempDir = new File(testFolderPath);
		File tempDirRoot = tempDir;
		// get rolling file 2020W52
		File test = getRollingFile(tempDirRoot.getAbsolutePath(), 2020, 11, 25);
		Files.createFile(Paths.get(test.getAbsolutePath()));
		assertEquals(1, tempDirRoot.listFiles().length); // test number of files

		test.setLastModified(new GregorianCalendar(2020, 11, 25).getTime().getTime()); // change the last modified date
																						// for the file to be deleted.
		test = FileUtils.getRollingFile(tempDirRoot.getAbsolutePath(), "", FileUtils.WEEKLY_ROLLING_FILENAME_DATE_FORMATTER,
				"", 7, null);
		Files.createFile(Paths.get(test.getAbsolutePath()));
		assertEquals(1, tempDirRoot.listFiles().length); // test number of files
	}

	@Test
	@Disabled("Wordt gefixt in een volgend PR")
	public void testGetRollingFileTheSamefile() throws IOException {
		File tempDirRoot = new File(testFolderPath);

		// get rolling file 2020W52
		File test = getRollingFile(tempDirRoot.getAbsolutePath(), 2020, 11, 25);
		Files.createFile(Paths.get(test.getAbsolutePath()));
		assertEquals(1, tempDirRoot.listFiles().length); // test number of files

		test = getRollingFile(tempDirRoot.getAbsolutePath(), 2020, 11, 25);
		try {
			Files.createFile(Paths.get(test.getAbsolutePath()));
		} catch (Exception e) {
			assertEquals(FileAlreadyExistsException.class, e.getClass());
		}
		assertEquals(1, tempDirRoot.listFiles().length); // test number of files
	}
}
