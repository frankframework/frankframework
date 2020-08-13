package nl.nn.adapterframework.util;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * FileUtils Tester.
 *
 * @author <Sina Sen>
 */
public class FileUtilsTest {

	@ClassRule
	public static TemporaryFolder testFolderSource = new TemporaryFolder();

	private static String sourceFolderPath;

	private static File f1;


	@BeforeClass
	public static void setUpTest() throws IOException {
		sourceFolderPath = testFolderSource.getRoot().getPath();
		f1 = testFolderSource.newFile("1.txt");
	}

	/**
	 * Method: getFilename(ParameterList definedParameters, IPipeLineSession session, String originalFilename, String filenamePattern)
	 */
	@Test
	public void testGetFilenameForDefinedParametersSessionOriginalFilenameFilenamePattern() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getFilename(ParameterList definedParameters, IPipeLineSession session, File originalFile, String filenamePattern)
	 */
	@Test
	public void testGetFilenameForDefinedParametersSessionOriginalFileFilenamePattern() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: moveFileAfterProcessing(File orgFile, String destDir, boolean delete, boolean overwrite, int numBackups)
	 */
	@Test
	public void testMoveFileAfterProcessing() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: moveFile(String filename, String destDir, boolean overwrite, int numBackups)
	 */
	@Test
	public void testMoveFileForFilenameDestDirOverwriteNumBackups() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: moveFile(File orgFile, String destDir, boolean overwrite, int numBackups)
	 */
	@Test
	public void testMoveFileForOrgFileDestDirOverwriteNumBackups() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: moveFile(File orgFile, File rename2File, boolean overwrite, int numBackups)
	 */
	@Test
	public void testMoveFileForOrgFileRename2FileOverwriteNumBackups() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: moveFile(File orgFile, File rename2File, boolean overwrite, int numBackups, int numberOfAttempts, long waitTime)
	 */
	@Test
	public void testMoveFileForOrgFileRename2FileOverwriteNumBackupsNumberOfAttemptsWaitTime() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: moveFile(File orgFile, File rename2File, int numberOfAttempts, long waitTime)
	 */
	@Test
	public void testMoveFileForOrgFileRename2FileNumberOfAttemptsWaitTime() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getFreeFile(File file)
	 */
	@Test
	public void testGetFreeFile() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: appendFile(File orgFile, File destFile, int nrRetries, long waitTime)
	 */
	@Test
	public void testAppendFile() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: copyFile(File orgFile, File destFile, boolean append)
	 */
	@Test
	public void testCopyFile() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: createTempFile()
	 */
	@Test
	public void testCreateTempFile() throws Exception {
		File a = FileUtils.createTempFile("abc", ".txt");
		String s = a.getPath();
		assertEquals("asd", s);
	}

	/**
	 * Method: createTempFile(String suffix)
	 */
	@Test
	public void testCreateTempFileSuffix() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: createTempFile(String prefix, String suffix)
	 */
	@Test
	public void testCreateTempFileForPrefixSuffix() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: createTempDir()
	 */
	@Test
	public void testCreateTempDir() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: createTempDir(File baseDir)
	 */
	@Test
	public void testCreateTempDirBaseDir() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: createTempDir(File baseDir, String subDir)
	 */
	@Test
	public void testCreateTempDirForBaseDirSubDir() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: createTempDir(File baseDir, String subDir, String prefix, String suffix)
	 */
	@Test
	public void testCreateTempDirForBaseDirSubDirPrefixSuffix() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: makeBackups(File targetFile, int numBackups)
	 */
	@Test
	public void testMakeBackups() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getWeeklyRollingFile(String directory, String filenamePrefix, String filenameSuffix, int retentionDays)
	 */
	@Test
	public void testGetWeeklyRollingFile() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getDailyRollingFile(String directory, String filenamePrefix, String filenameSuffix, int retentionDays)
	 */
	@Test
	public void testGetDailyRollingFile() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getRollingFile(String directory, String filenamePrefix, String dateformat, String filenameSuffix, int retentionDays)
	 */
	@Test
	public void testGetRollingFile() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getFiles(String directory, String wildcard, String excludeWildcard, long minStability)
	 */
	@Test
	public void testGetFiles() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getFirstFile(File directory)
	 */
	@Test
	public void testGetFirstFileDirectory() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getFirstFile(String directory, long minStability)
	 */
	@Test
	public void testGetFirstFileForDirectoryMinStability() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getListFromNames(String names, char seperator)
	 */
	@Test
	public void testGetListFromNamesForNamesSeperator() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getListFromNames(String[] names)
	 */
	@Test
	public void testGetListFromNamesNames() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getNamesFromArray(String[] names, char seperator)
	 */
	@Test
	public void testGetNamesFromArray() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getNamesFromList(List<String> filenames, char seperator)
	 */
	@Test
	public void testGetNamesFromList() throws Exception {
//TODO: Test goes here...
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
//TODO: Test goes here...
	}

	/**
	 * Method: canWrite(String directory)
	 */
	@Test
	public void testCanWrite() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: encodeFileName(String fileName)
	 */
	@Test
	public void testEncodeFileName() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: unzipStream(InputStream inputStream, File dir)
	 */
	@Test
	public void testUnzipStream() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: readAllowed(String rules, HttpServletRequest request, String fileName)
	 */
	@Test
	public void testReadAllowed() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: getLastModifiedDelta(File file)
	 */
	@Test
	public void testGetLastModifiedDelta() throws Exception {
//TODO: Test goes here...
	}

	/**
	 * Method: isFileBinaryEqual(File first, File second)
	 */
	@Test
	public void testIsFileBinaryEqual() throws Exception {
//TODO: Test goes here...
	}


}
