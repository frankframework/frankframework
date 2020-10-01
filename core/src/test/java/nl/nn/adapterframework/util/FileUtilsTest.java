package nl.nn.adapterframework.util;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * FileUtils Tester.
 *
 * @author <Sina Sen>
 */
public class FileUtilsTest {

	@ClassRule
	public static TemporaryFolder testFolderSource = new TemporaryFolder();

	@ClassRule
	public static TemporaryFolder testFolderDest = new TemporaryFolder();

	public static String sourceFolderPath;

	public static String destFolderPath;

	private static File f1;

	public String sep = File.separator;

	@BeforeClass
	public static void setUpTest() throws IOException {
		sourceFolderPath = testFolderSource.getRoot().getPath();
		f1 = testFolderSource.newFile("1.txt");
		destFolderPath = testFolderDest.getRoot().getPath();
	}

	@Test
	public void testGetFreeFile() throws Exception {
		File f = new File(".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes");
		File res = FileUtils.getFreeFile(f);
		assertEquals("Pipes", res.getName());
	}


	@Test
	public void testMoveFile() throws Exception {
		String s = FileUtils.moveFile(f1, destFolderPath, true, 2);
		assertEquals(destFolderPath + sep + "1.txt", s);
	}


	/**
	 * Method: appendFile(File orgFile, File destFile, int nrRetries, long waitTime)
	 */
	@Test
	public void testAppendFile() throws Exception {
		File orgFile = new File(".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes" + sep + "fileToAppend.txt");
		File destFile = new File(".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes" + sep + "destinationFile.txt");
		String res = FileUtils.appendFile(orgFile, destFile, 5, 5000);
		assertEquals(destFile.getAbsolutePath(), res);
	}

	@Test
	public void testCopyFile() throws Exception {
		File sourceFile = new File(".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes" + sep + "fileToAppend.txt");
		File destFile = new File(".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes" + sep + "copyFile.txt");
		boolean b = FileUtils.copyFile(sourceFile, destFile, true);
		assertEquals(true, b);
	}


	/**
	 * Method: createTempDir(File baseDir)
	 */
	@Test
	public void testCreateTempDirBaseDir() throws Exception {
		String path = ".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes";
		File f = new File(path);
		File file = FileUtils.createTempDir(f);
		assertTrue(file.exists());
	}

	/**
	 * Method: makeBackups(File targetFile, int numBackups)
	 */
	@Test
	public void testMakeBackups() throws Exception {
		FileUtils.makeBackups(f1, 5);
		assertEquals(3, 3);
	}

	/**
	 * Method: getFiles(String directory, String wildcard, String excludeWildcard, long minStability)
	 */

	@Test
	public void testGetFiles() throws Exception {
		String path = ".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes";
		File[] files = FileUtils.getFiles(path, "*", null, 5);
		assertEquals("2.txt", files[0].getName());
		assertEquals(8, files.length);
	}


	/**
	 * Method: getFirstFile(File directory)
	 */
	@Test
	public void testGetFirstFileDirectory() throws Exception {
		String path = ".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes";
		File f = new File(path);
		File file = FileUtils.getFirstFile(f);
		assertEquals("2.txt", file.getName());
	}

	/**
	 * Method: getListFromNames(String names, char seperator)
	 */
	@Test
	public void testGetListFromNamesForNamesSeperator() throws Exception {
		List<String> list = FileUtils.getListFromNames("abc.txt,test.txt,jkl.txt", ',');
		assertEquals("abc.txt", list.get(0));
		assertEquals("test.txt", list.get(1));
		assertEquals("jkl.txt", list.get(2));
	}

	/**
	 * Method: getListFromNames(String[] names)
	 */
	@Test
	public void testGetListFromNamesNames() throws Exception {
		String[] names = {"abc.txt", "test.txt", "jkl.txt"};
		List<String> list = FileUtils.getListFromNames(names);
		assertEquals("abc.txt", list.get(0));
		assertEquals("test.txt", list.get(1));
		assertEquals("jkl.txt", list.get(2));

	}

	/**
	 * Method: getNamesFromArray(String[] names, char seperator)
	 */
	@Test
	public void testGetNamesFromArray() throws Exception {
		String[] names = {"abc.txt", "test.txt", "jkl.txt"};
		String res = FileUtils.getNamesFromArray(names, ',');
		assertEquals("abc.txt,test.txt,jkl.txt", res);
	}

	/**
	 * Method: getNamesFromList(List<String> filenames, char seperator)
	 */
	@Test
	public void testGetNamesFromList() throws Exception {
		String[] names = {"abc.txt", "test.txt", "jkl.txt"};
		List<String> list = FileUtils.getListFromNames(names);
		String res = FileUtils.getNamesFromList(list, ',');
		assertEquals("abc.txt,test.txt,jkl.txt", res);
	}

	/**
	 * Method: align(String val, int length, boolean leftAlign, char fillchar)
	 */
	@Test
	public void testAlignForValLengthLeftAlignFillchar() throws Exception {
		String s = "test";
		String res = FileUtils.align(s, 10, true, 'b');
		String res2 = FileUtils.align(s, 2, true, 'b');
		assertEquals("testbbbbbb", res);
		assertEquals("te", res2);
	}

	/**
	 * Method: getFilledArray(int length, char fillchar)
	 */
	@Test
	public void testGetFilledArray() throws Exception {
		char[] arr = FileUtils.getFilledArray(5, 'a');
		assertEquals('a', arr[2]);
	}

	/**
	 * Method: getFileNameExtension(String fileName)
	 */
	@Test
	public void testGetFileNameExtension() throws Exception {
		String s = FileUtils.getFileNameExtension("file.txt");
		assertEquals("txt", s);
	}

	/**
	 * Method: getBaseName(String fileName)
	 */
	@Test
	public void testGetBaseName() throws Exception {
		String s = FileUtils.getBaseName("file.txt");
		assertEquals("file", s);
	}

	/**
	 * Method: extensionEqualsIgnoreCase(String fileName, String extension)
	 */
	@Test
	public void testExtensionEqualsIgnoreCase() throws Exception {
		String x1 = "a.txt";
		String x2 = "a.tXt";
		boolean b = FileUtils.extensionEqualsIgnoreCase(x2, "txt");
		boolean b2 = FileUtils.extensionEqualsIgnoreCase(x1, "txt");
		assertTrue(b);
		assertTrue(b2);
	}

	/**
	 * Method: canWrite(String directory)
	 */
	@Test
	public void testCanWrite() throws Exception {
		String p1 = ".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Utils";
		String p2 = ".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes" + sep + "2.txt";
		boolean b = FileUtils.canWrite(p1);
		boolean b2 = FileUtils.canWrite(p2);
		assertTrue(b);
		assertFalse(b2);
	}

	@Test
	public void testGetFirstFile() {
		String p2 = ".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes";
		File f = new File(p2);
		File res = FileUtils.getFirstFile(f);
		assertEquals("2.txt", res.getName());
	}
	/**
	 * Method: encodeFileName(String fileName)
	 */
	@Test
	public void testEncodeFileName() throws Exception {
		String s = " abc.txt";
		String encoded = FileUtils.encodeFileName(s);
		assertEquals("_abc.txt", encoded);
	}

	/**
	 * Method: isFileBinaryEqual(File first, File second)
	 */
	@Test
	public void testIsFileBinaryEqual() throws Exception {
		String p1 = ".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes" + sep + "2.txt";
		String p2 = ".." + sep + "core" + sep + "src" + sep + "test" + sep + "resources" + sep + "Pipes" + sep + "books.xml";
		File f1 = new File(p1);
		File f2 = new File(p2);
		boolean b = FileUtils.isFileBinaryEqual(f1, f2);
		boolean b2 = FileUtils.isFileBinaryEqual(f1, f1);
		assertTrue(b2);
		assertFalse(b);
	}

}
