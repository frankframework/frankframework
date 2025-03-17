package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.net.URL;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class UnzipPipeTest extends PipeTestBase<UnzipPipe> {

	private TemporaryFolder folder;
	private String fileSeparator = File.separator;

	@Override
	public void setUp() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		super.setUp();
	}

	@Override
	public UnzipPipe createPipe() {
		UnzipPipe pipe = new UnzipPipe();
		pipe.setDirectory(folder.getRoot().toString());
		return pipe;
	}

	@Test
	public void testConfigureAndStartWithDefaultAttributes() throws ConfigurationException, PipeStartException {
		configureAndStartPipe();
		assertFalse(pipe.isSkipOnEmptyInput());
	}

	@Test
	public void testUnzipFromStream() throws Exception {
		pipe.setKeepOriginalFileName(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");

		String expected = 	"<results count=\"2\">"+
								"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"fileaa.txt</fileName></result>" +
								"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"filebb.log</fileName></result>" +
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

		String expected = 	"<results count=\"2\">"+
							"</results>";

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
				"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"fileaa.txt</fileName>" +
					"<fileContent>aaa</fileContent>"+
				"</result>" +
				"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"filebb.log</fileName>" +
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
				"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"fileaa.txt</fileName>" +
					"<fileContent>aaa</fileContent>"+
				"</result>" +
				"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"filebb.log</fileName>" +
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
		String[] files = new File(folder.getRoot()+"/Folder/innerFolder").list();
		assertEquals(1, files.length);
		assertTrue(files[0].contains("innerFile"));
	}

	@Test
	public void testCreateSubDirectoriesInnerItemAsTheFirstEntry() throws Exception {
		pipe.setKeepOriginalFilePath(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/input.zip");
		doPipe(new UrlMessage(zip));
		String[] files = new File(folder.getRoot()+"/MyProjects/").list();
		assertEquals(5, files.length);

		files = new File(folder.getRoot()+"/MyProjects/classes/xml/xsl/").list();
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
		String[] files = new File(folder.getRoot().getPath()).list();
		assertEquals(6, files.length);
	}

	@Test
	public void testDefaultConfiguration() throws Exception {
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/input.zip");
		doPipe(new UrlMessage(zip));
		String[] files = new File(folder.getRoot().getPath()).list();
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
		File toBePresent = new File(folder.getRoot()+"/Folder/innerFolder/innerFile.txt");
		assertTrue(toBePresent.isFile());
	}

	@Test
	public void testUnzipToDirectory() throws Exception {
		String path = folder.getRoot().getCanonicalPath();
		log.debug("unzip folder [{}]", path);
		session.put("UnzipFolderName", path);

		pipe.setCollectResults(false);
		pipe.setDirectorySessionKey("UnzipFolderName");
		pipe.setKeepOriginalFileName(true);
		pipe.setProcessFile(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/folder.zip");
		assertNotNull(zip);
		doPipe(zip.getPath());

		File toBePresent = new File(folder.getRoot()+"/innerFile.txt");
		assertTrue(toBePresent.isFile());

		File toBePresent2 = new File(folder.getRoot()+"/file.txt");
		assertTrue(toBePresent2.isFile());
	}

	@Test
	public void testCreateSubDirectoriesKeepFilenameDeleteOnExit() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setKeepOriginalFilePath(true);
		configureAndStartPipe();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/folder.zip");
		doPipe(new UrlMessage(zip));

		File toBePresent = new File(folder.getRoot()+"/Folder/innerFolder/innerFile.txt");
		assertTrue(toBePresent.exists());
	}

	@Test
	public void testNullDirectory() throws Exception {
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
