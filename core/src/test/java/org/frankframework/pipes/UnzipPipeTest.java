package org.frankframework.pipes;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestFileUtils;

public class UnzipPipeTest extends PipeTestBase<UnzipPipe> {

	@TempDir
	private File folder;
	private final String fileSeparator = File.separator;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	public UnzipPipe createPipe() {
		UnzipPipe pipe = new UnzipPipe();
		pipe.setDirectory(folder.toString());
		return pipe;
	}

	@Test
	public void testConfigureAndStartWithDefaultAttributes() throws ConfigurationException {
		configureAndStartPipe();
		assertFalse(pipe.isSkipOnEmptyInput());
	}

	@Test
	public void testUnzipFromStream() throws Exception {
		pipe.setKeepOriginalFileName(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");

		String expected = 	"<results count=\"2\">"+
				"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>" + folder.toString() + fileSeparator + "fileaa.txt</fileName></result>" +
				"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>" + folder.toString() + fileSeparator + "filebb.log</fileName></result>" +
							"</results>";

		PipeRunResult prr = doPipe(new UrlMessage(zip));

		assertXmlEquals(expected, prr.getResult().asString());
	}


	@Test
	public void testUnzipNoCollectResults() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setCollectResults(false);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");

		String expected = 	"""
							<results count="2">\
							</results>\
							""";

		PipeRunResult prr = doPipe(new UrlMessage(zip));

		assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testUnzipCollectFileContents() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setCollectFileContents(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");

		String expected = 	"<results count=\"2\">" +
				"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>" + folder.toString() + fileSeparator + "fileaa.txt</fileName>" +
					"<fileContent>aaa</fileContent>"+
				"</result>" +
				"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>" + folder.toString() + fileSeparator + "filebb.log</fileName>" +
					"<fileContent>bbb</fileContent>"+
				"</result>" +
			"</results>";

		PipeRunResult prr = doPipe(new UrlMessage(zip));

		assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testUnzipCollectFileContentsBase64() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setCollectFileContentsBase64Encoded(".log");
		pipe.setCollectFileContents(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");

		String expected = 	"<results count=\"2\">" +
				"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>" + folder.toString() + fileSeparator + "fileaa.txt</fileName>" +
					"<fileContent>aaa</fileContent>"+
				"</result>" +
				"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>" + folder.toString() + fileSeparator + "filebb.log</fileName>" +
					"<fileContent>YmJi\n</fileContent>"+
				"</result>" +
			"</results>";

		PipeRunResult prr = doPipe(new UrlMessage(zip));

		assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testCreateSubDirectories() throws Exception {
		pipe.setKeepOriginalFilePath(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/folder.zip");
		doPipe(new UrlMessage(zip));
		String[] files = new File(folder + "/Folder/innerFolder").list();
		assertEquals(1, files.length);
		assertTrue(files[0].contains("innerFile"));
	}

	@Test
	public void testCreateSubDirectoriesInnerItemAsTheFirstEntry() throws Exception {
		pipe.setKeepOriginalFilePath(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/input.zip");
		doPipe(new UrlMessage(zip));
		String[] files = new File(folder + "/MyProjects/").list();
		assertEquals(5, files.length);

		files = new File(folder + "/MyProjects/classes/xml/xsl/").list();
		assertEquals(2, files.length);
	}

	@Test
	public void testExtractAllInTheRoot() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setKeepOriginalFilePath(false);
		pipe.setDeleteOnExit(false);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/input.zip");
		doPipe(new UrlMessage(zip));
		String[] files = new File(folder.getPath()).list();
		assertEquals(6, files.length);
	}

	@Test
	public void testDefaultConfiguration() throws Exception {
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/input.zip");
		doPipe(new UrlMessage(zip));
		String[] files = new File(folder.getPath()).list();
		assertEquals(6, files.length);
	}

	@Test
	public void testCreateSubDirectoriesKeepFilename() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setKeepOriginalFilePath(true);
		pipe.setDeleteOnExit(false);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/folder.zip");
		doPipe(new UrlMessage(zip));
		File toBePresent = new File(folder + "/Folder/innerFolder/innerFile.txt");
		assertTrue(toBePresent.isFile());
	}

	@Test
	public void testCreateSubDirectoriesKeepFilenameDeleteOnExit() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setKeepOriginalFilePath(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/folder.zip");
		doPipe(new UrlMessage(zip));

		File toBePresent = new File(folder + "/Folder/innerFolder/innerFile.txt");
		assertTrue(toBePresent.exists());
	}

	@Test
	public void testNullDirectory() {
		pipe.setDirectory(null);
		pipe.setDirectorySessionKey(null);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.containsString("directory or directorySessionKey must be specified"));
	}

	@Test
	public void testNullDirectoryFakeSessionKey() throws Exception {
		pipe.setDirectory(null);
		pipe.setDirectorySessionKey("dummy");
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/folder.zip");

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(new UrlMessage(zip)));
		assertThat(e.getMessage(), Matchers.containsString("directorySessionKey is empty"));
	}
}
