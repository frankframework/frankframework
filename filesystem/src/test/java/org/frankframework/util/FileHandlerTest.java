package org.frankframework.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

/**
 * This class is due to be removed! Do not add more tests
 */
@SuppressWarnings("removal")
public class FileHandlerTest {

	public static String BASEDIR = "/FileHandler/";
	private final Logger log = LogUtil.getLogger(this);

	@TempDir public File tempFolder;

	private FileHandler handler;
	private final PipeLineSession session = new PipeLineSession();
	public String charset = StandardCharsets.UTF_8.name();

	@BeforeEach
	public void setUp() {
		handler = new FileHandler();
	}

	public URL getURL(String file) {
		return FileHandlerTest.class.getResource(BASEDIR + file);
	}

	public String removeNewlines(String contents) {
		return contents.replaceAll("[\n\r]", "");
	}

	private String testIllegalAction(String action) {
		handler.setActions(action);
		return assertThrows(ConfigurationException.class, handler::configure).getMessage();
	}

	public void testRead(String filename, String charset, boolean decode, boolean encode) throws Exception {
		testRead(filename, charset, decode, encode, false, "String");
	}

	public void testRead(String filename, String charset, boolean decode, boolean encode, boolean skipBom, String outputType) throws Exception {
		URL fileURL = getURL(filename);
		String inFilepath = fileURL.getPath();

		String compareFile = filename;
		String actions = "read";

		if(decode) {
			inFilepath += ".b64";
			actions += ",decode";
		}
		if(encode) {
			actions += ",encode";
			compareFile += ".b64";
		}
		handler.setActions(actions);
		handler.setCharset(charset);
		handler.setFileName(inFilepath);
		handler.setSkipBOM(skipBom);
		handler.setOutputType(outputType);
		handler.configure();

		String expectedContents = TestFileUtils.getTestFile(BASEDIR + compareFile, charset);
		assertNotNull(expectedContents);
		if(outputType == null || "string".equalsIgnoreCase(outputType)) {
			String actualContents = (String) handler.handle(null, session, null);
			assertEquals(removeNewlines(expectedContents), removeNewlines(actualContents), "file contents");
		} else {
			byte[] actualContents = (byte[]) handler.handle(null, session, null);
			assertEquals(expectedContents, new String(actualContents, StandardCharsets.UTF_8), "file contents");
			assertEquals(expectedContents.getBytes().length, actualContents.length);
		}
	}

	public void testWrite(String filename, String charset, boolean decode, boolean encode, String baseAction, String fileContentsAtStart, boolean truncate, boolean write, boolean writeSeparator) throws Exception {
		testWrite(null, filename, null, false, filename, charset, decode, encode, baseAction, fileContentsAtStart, truncate, write, writeSeparator);
	}

	public void testWrite(String directory, String filename, String suffix, boolean suffixViaParam, String contentFile, String charset, boolean decode, boolean encode, String baseAction, String fileContentsAtStart, boolean truncate, boolean write, boolean writeSeparator) throws Exception {
		ParameterList paramList = null;
		String compareFile = directory == null ? contentFile : directory + "/" + contentFile;
		String filepath = null;
		if(filename != null) {
			URL fileURL = getURL(filename);
			filepath = fileURL.getPath() + ".tmp";

			try {
				Path path = Path.of(filepath);
				Files.deleteIfExists(path);
			} catch (Exception e) {} //Ignored
			if(fileContentsAtStart != null) {
				FileWriter fw = new FileWriter(filepath);
				fw.write(fileContentsAtStart);
				fw.close();
			}
		}

		String actions = baseAction;
		if(decode) {
			contentFile += ".b64";
			actions = "decode," + actions;
		}
		if(encode) {
			actions = "encode," + actions;
			compareFile += ".b64";
		}
		handler.setActions(actions);
		handler.setCharset(charset);
		if(directory != null) {
			handler.setDirectory(directory);
		}
		if(suffix != null) {
			if(suffixViaParam) {
				paramList = new ParameterList();
				paramList.add(new Parameter("writeSuffix", suffix));
				paramList.configure();
				handler.setWriteSuffix(".wsx");
			} else {
				handler.setWriteSuffix(suffix);
				session.remove("writeSuffix");
			}
		}
		handler.setFileName(filepath);
		handler.setWriteLineSeparator(writeSeparator);
		handler.configure();

		String contents = MessageTestUtils.getMessage(BASEDIR + contentFile).asString();
		assertNotNull(contents);
		String stringContent = contents.replace("\r", ""); //Remove carriage return
		String actFilename = (String) handler.handle(new Message(stringContent), session, paramList);
		if(filename == null) {
			assertNotNull(actFilename);
		} else {
			File f = new File(filepath);
			assertEquals(f.getAbsolutePath(), actFilename);
		}

		String expectedContents;
		if(fileContentsAtStart != null && !truncate) {
			expectedContents = fileContentsAtStart;
		} else {
			expectedContents = "";
		}
		if(write) {
			expectedContents += TestFileUtils.getTestFile(BASEDIR + compareFile, charset);
			if(writeSeparator) {
				expectedContents += System.getProperty("line.separator");
			}
		}

		log.debug("act filename [{}] suffix [{}]", actFilename, suffix);
		if(suffix != null) {
			assertThat(actFilename, endsWith(suffix));
		}
		File fa = new File(actFilename);

		String actualContents = TestFileUtils.getTestFile(fa.toURI().toURL(), charset);
		assertEquals(expectedContents.trim(), actualContents, "appended file contents");
	}

	public void testList(String filename, String charset) throws Exception {

		URL fileURL = getURL(filename);
		String filePath = fileURL.getPath();
		String directoryPath = filePath.substring(0, filePath.length() - filename.length());

		String actions = "list";

		handler.setActions(actions);
		handler.setDirectory(directoryPath);
		handler.setCharset(charset);
		handler.configure();

		handler.handle(null, session, null);

		String actualContents = (String) handler.handle(null, session, null);
		assertThat(actualContents, startsWith("<directory name"));
		assertThat(actualContents, containsString(filename));
	}

	public void testInfo(String filename, String charset) throws Exception {
		URL fileURL = getURL(filename);
		String filePath = fileURL.getPath();
		String actions = "info";

		handler.setActions(actions);
		handler.setFileName(filePath);
		handler.setCharset(charset);
		handler.configure();
		handler.handle(null, session, null);

		String actualContents = (String) handler.handle(null, session, null);
		log.debug("actual [{}]", actualContents);
		assertThat(actualContents, startsWith("<file>"));
		assertThat(actualContents, containsString(filename + "</fullName>"));
		assertThat(actualContents, containsString("<name>" + filename + "</name>"));
		assertThat(actualContents, containsString("<modificationDate>"));
		assertThat(actualContents, containsString("<modificationTime>"));
	}

	public void testDelete(String filename, boolean readDelete) throws Exception {
		String expectedContents = "contents of readDelete delete file";

		URL fileURL = getURL(filename);
		String filepath = fileURL.getPath() + ".tmp";

		FileOutputStream fout = new FileOutputStream(filepath);
		fout.write(expectedContents.getBytes());
		fout.close();
		File file = new File(filepath);
		assertTrue(file.exists());

		String actions = readDelete ? "read_delete" : "delete";
		handler.setActions(actions);
		handler.setFileName(filepath);
		handler.setCharset(charset);
		handler.configure();

		// String contents=TestFileUtils.getTestFile(contentFile, charset);

		String actualContents = (String) handler.handle(null, session, null);
		if(readDelete) assertEquals(expectedContents, actualContents, "file contents");
		assertFalse(Files.exists(file.toPath()), "file [" + filepath + "] should have been deleted");
	}

	@Test
	public void testIllegalAction1() throws Exception {
		testIllegalAction("lees");
	}

	@Test
	public void testIllegalAction2() throws Exception {
		testIllegalAction("write,schrijf");
	}

	@Test
	public void testNullAction() throws Exception {
		String exceptionMessage = testIllegalAction(null);
		assertThat(exceptionMessage, endsWith("should at least define one action"));
	}

	@Test
	public void testEmptyAction1() throws Exception {
		String exceptionMessage = testIllegalAction("");
		assertThat(exceptionMessage, endsWith("should at least define one action"));
	}

	@Test
	public void testEmptyAction2() throws Exception {
		String exceptionMessage = testIllegalAction(",");
		assertThat(exceptionMessage, endsWith("should at least define one action"));
	}

	@Test
	public void testReadXml() throws Exception {
		testRead("smiley.xml", charset, false, false);
		// TODO: fix the below tests. On Azure, filesize is based on CRLF line endings, instead of LF
//		testRead("smiley.xml",charset,false,false,true,"bytes");
//		testRead("smiley.xml",charset,false,false,false,"bytes");
	}

	@Test
	public void testReadJson() throws Exception {
		testRead("smiley.json", charset, false, false);
	}

	@Test
	public void testReadTxt() throws Exception {
		testRead("smiley.txt", charset, false, false);
		// TODO: fix the below tests. On Azure, filesize is based on CRLF line endings, instead of LF
//		testRead("smiley.txt",charset,false,false,true,"bytes");
//		testRead("smiley.txt",charset,false,false,false,"bytes");
	}

	@Test
	public void testReadJsonDecode() throws Exception {
		testRead("smiley.json", charset, true, false);
	}

	@Test
	@Disabled //Doesn't seem to work properly
	public void testWriteJsonEncodeCreateFresh() throws Exception {
		testWrite("smiley.json", charset, false, false, "create", null, true, false, false);
		testWrite("smiley.json", charset, false, false, "create", null, true, false, true);
		testWrite(null, null, null, false, "smiley.json", charset, false, false, "create", null, true, false, false);
		testWrite(null, null, null, false, "smiley.json", charset, false, false, "create", null, true, false, true);

		String testRootDir = tempFolder.getAbsolutePath();

		testWrite(testRootDir + "/sub", null, null, false, "smiley.json", charset, false, false, "create", null, true, false, false);
		testWrite(testRootDir + "/sub", null, null, false, "smiley.json", charset, false, false, "create", null, true, false, true);

		testWrite(testRootDir + "/sub1/sub2", null, null, false, "smiley.json", charset, false, false, "create", null, true, false, false);
		testWrite(testRootDir + "/sub1/sub2", null, null, false, "smiley.json", charset, false, false, "create", null, true, false, true);

		testWrite(testRootDir + "/sub", null, ".sfx", false, "smiley.json", charset, false, false, "create", null, true, false, false);
		testWrite(testRootDir + "/sub", null, ".sfx", true, "smiley.json", charset, false, false, "create", null, true, false, true);
	}

	@Test
	public void testWriteJsonEncodeCreateTruncate() throws Exception {
		testWrite("smiley.json", charset, false, false, "create", "content at start1", true, false, false);
		testWrite("smiley.json", charset, false, false, "create", "content at start1", true, false, true);
		testWrite(null, null, null, false, "smiley.json", charset, false, false, "create", "content at start1", true, false, false);
		testWrite(null, null, null, false, "smiley.json", charset, false, false, "create", "content at start1", true, false, true);
	}

	@Test
	public void testWriteJsonEncodeWriteFresh() throws Exception {
		testWrite("smiley.json", charset, false, false, "write", null, true, true, false);
		testWrite("smiley.json", charset, true, false, "write", null, true, true, true);
		testWrite("smiley.json", charset, false, true, "write", null, true, true, false);
		testWrite("smiley.json", charset, false, false, "write", "content at start2", true, true, false);
		testWrite("smiley.json", charset, true, false, "write", "content at start2", true, true, true);
		testWrite("smiley.json", charset, false, true, "write", "content at start2", true, true, false);
		testWrite(null, null, null, false, "smiley.json", charset, false, false, "write", null, true, true, false);
		testWrite(null, null, null, false, "smiley.json", charset, true, false, "write", null, true, true, true);
		testWrite(null, null, null, false, "smiley.json", charset, false, true, "write", null, true, true, false);
	}

	@Test
	public void testWriteJsonEncodeWriteAppend() throws Exception {
//		testWrite("smiley.json",charset,false,false,"write_append",null,false,true);
//		testWrite("smiley.json",charset,true,false,"write_append",null,false,true);
//		testWrite("smiley.json",charset,false,true,"write_append",null,false,true);
		testWrite("smiley.json", charset, false, false, "write_append", "content at start2", false, true, false);
		testWrite("smiley.json", charset, true, false, "write_append", "content at start3", false, true, true);
		testWrite("smiley.json", charset, false, true, "write_append", "content at start4", false, true, true);
	}

	@Test
	public void testList() throws Exception {
		testList("smiley.json", charset);
	}

	@Test
	public void testInfo() throws Exception {
		testInfo("smiley.json", charset);
	}

	@Test
	public void testReadDelete() throws Exception {
		testDelete("smiley.xml", true);
	}

	@Test
	public void testDelete() throws Exception {
		testDelete("smiley.txt", false);
	}

}
