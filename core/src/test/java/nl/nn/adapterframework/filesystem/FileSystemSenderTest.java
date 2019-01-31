package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public abstract class FileSystemSenderTest<F, FS extends IFileSystem<F>> extends
		LocalFileSystemTestBase<F, FS> {

	FileSystemSender fileSystemSender;

	@Before
	public void setup() throws ConfigurationException, IOException {
		super.setup();
		fileSystemSender = new FileSystemSender();
		fileSystemSender.setFileSystem(fileSystem);
	}

	@Test
	public void downloadActionTest() throws IOException, SenderException, TimeOutException {
		String filename = FILE1;
		String contents = "Tekst om te lezen";
		createFile(filename, contents);
		assertTrue(_fileExists(filename));

		fileSystemSender.setAction("download");
		String actual;
		actual = fileSystemSender.sendMessage("message", filename);

		assertEquals(contents.trim(), actual.trim());

	}

	@Test
	public void uploadActionTestWithString() throws SenderException, TimeOutException, IOException {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";
		createFile(filename, contents);
		assertTrue(_fileExists(filename));

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("file", contents);

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		fileSystemSender.setAction("upload");

		String actual;
		actual = fileSystemSender.sendMessage("message", filename, prc);

		actual = readFile(filename);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void uploadActionTestWithByteArray() throws SenderException, TimeOutException,
			IOException {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";
		createFile(filename, contents);
		assertTrue(_fileExists(filename));

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("file", contents.getBytes());

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		fileSystemSender.setAction("upload");

		String actual;
		actual = fileSystemSender.sendMessage("message", filename, prc);

		actual = readFile(filename);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void uploadActionTestWithInputStream() throws SenderException, TimeOutException,
			IOException {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		createFile(filename, contents);
		assertTrue(_fileExists(filename));
		InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("file", stream);

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		fileSystemSender.setAction("upload");

		String actual;
		actual = fileSystemSender.sendMessage("message", filename, prc);

		actual = readFile(filename);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void mkdirActionTest() throws SenderException, TimeOutException {
		String filename = DIR1;

		fileSystemSender.setAction("mkdir");
		String message = "message";
		String actual;
		actual = fileSystemSender.sendMessage(message, filename);

		assertEquals(message.trim(), actual.trim());

		boolean result = _fileExists(filename);
		assertTrue("Expected file[" + filename + "] to be present", result);

	}

	@Test
	public void rmdirActionTest() throws IOException, SenderException, TimeOutException {
		String filename = DIR1;
		if (!_fileExists(DIR1)) {
			_createFolder(filename);
		}

		fileSystemSender.setAction("rmdir");
		String message = "message";
		String actual;
		actual = fileSystemSender.sendMessage(message, filename);
		assertEquals(message.trim(), actual.trim());

		boolean result = _fileExists(filename);
		assertFalse("Expected file [" + filename + "] " + "not to be present", result);
	}

	@Test
	public void deleteActionTest() throws IOException, SenderException, TimeOutException {
		String filename = "tobedeleted" + FILE1;
		if (!_fileExists(filename)) {
			createFile(filename, "is not empty");
		}

		fileSystemSender.setAction("delete");
		String message = "message";
		String actual;
		actual = fileSystemSender.sendMessage(message, filename);
		assertEquals(message.trim(), actual.trim());

		boolean result = _fileExists(filename);
		assertFalse("Expected file [" + filename + "] " + "not to be present", result);
	}

	@Test
	public void renameActionTest() throws IOException, SenderException, TimeOutException {
		String filename = "toberenamed" + FILE1;
		String dest = "renamed" + FILE1;
		if (!_fileExists(filename)) {
			createFile(filename, "is not empty");
		}

		if (_fileExists(dest)) {
			deleteFile(dest);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("destination", dest);

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		fileSystemSender.setAction("rename");
		String message = "message";
		String actual;
		actual = fileSystemSender.sendMessage(message, filename, prc);
		assertEquals(message.trim(), actual.trim());

		boolean result;
		result = _fileExists(filename);
		assertFalse("Expected file [" + filename + "] " + "not to be present", result);

		result = _fileExists(dest);
		assertTrue("Expected file [" + dest + "] " + "to be present", result);
	}

	@Test
	public void listActionTest() throws SenderException, TimeOutException {

		File folder = getFileHandle("");
		File[] listOfFiles = folder.listFiles();
		String actual = "";
		for (int i = 1; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				try {
					actual += IOUtils
							.toString(FileUtils.openInputStream((listOfFiles[i])), "UTF-8");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (listOfFiles[i].isDirectory()) {
				System.out.println("Directory " + listOfFiles[i].getName());
			}
		}

		fileSystemSender.setAction("list");
		//		actual = actual.replaceAll("[ViewState]" + "Mode=" + "Vid=" + "FolderType=Generic", "");
		String result = fileSystemSender.sendMessage("message", "message");
		assertEquals(actual.trim(), result.trim());
	}

	@Override
	@Ignore
	@Test
	public void testExists() throws IOException, FileSystemException {
	}

	@Override
	@Ignore
	@Test
	public void testExistsContinue() throws FileSystemException, IOException {
	}

	@Override
	@Ignore
	@Test
	public void testCreateNewFile() throws IOException, FileSystemException {
	}

	@Override
	@Ignore
	@Test
	public void testCreateOverwriteFile() throws IOException, FileSystemException {
	}

	@Override
	@Ignore
	@Test
	public void testTruncateFile() throws IOException, FileSystemException {
	}

	@Override
	@Ignore
	@Test
	public void testAppendFile() throws IOException, FileSystemException {
	}

	@Override
	@Ignore
	@Test
	public void testDelete() throws IOException, FileSystemException {
	}

	@Override
	@Ignore
	@Test
	public void testRead() throws IOException, FileSystemException {
	}

	@Override
	@Ignore
	public void testFileInfo(F f) throws FileSystemException {
	}

	@Override
	@Ignore
	public void testFileInfo() throws FileSystemException {
	}

	@Override
	@Ignore
	@Test
	public void testListFile() throws IOException, FileSystemException {
	}

}