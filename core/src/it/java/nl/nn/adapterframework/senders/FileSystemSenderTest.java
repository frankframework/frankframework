package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IFileSystem;
import nl.nn.adapterframework.filesystems.FileSystemTest;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.FileSystemSender;

public abstract class FileSystemSenderTest<F, FS extends IFileSystem<F>> extends FileSystemTest<F, FS> {

	private FileSystemSender<F, FS> fileSystemSender;

	public FileSystemSender<F, FS> createFileSystemSender() {
		FileSystemSender<F, FS> fileSystemSender = new FileSystemSender<F, FS>();
		fileSystemSender.setFileSystem(fileSystem);
		return fileSystemSender;
	}

	@Before
	public void setup() throws ConfigurationException, IOException, FileSystemException {
		super.setup();
		fileSystemSender = createFileSystemSender();
	}

	@Test
	public void uploadActionTestWithString() throws Exception {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTargetwString", contents.getBytes());

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTargetwString");

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("upload");
		fileSystemSender.configure();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);
		String actual;
		
		actual = fileSystemSender.sendMessage("<result>ok</result>", filename, prc);
		waitForActionToFinish();
		
		actual = readFile(filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void uploadActionTestWithByteArray() throws Exception {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTargetwByteArray", contents.getBytes());

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTargetwByteArray");

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("upload");
		fileSystemSender.configure();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		String actual;
		actual = fileSystemSender.sendMessage("<result>ok</result>", filename, prc);
		waitForActionToFinish();
		waitForActionToFinish();
		
		actual = readFile(filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void uploadActionTestWithInputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(filename);
		}

		InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTarget", stream);

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTarget");

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("upload");
		fileSystemSender.configure();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		String actual;
		actual = fileSystemSender.sendMessage("<result>ok</result>", filename, prc);
		waitForActionToFinish();

		actual = readFile(filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void downloadActionTest() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(filename, contents);
		waitForActionToFinish();

		fileSystemSender.setAction("download");
		fileSystemSender.configure();
		
		String actual;
		actual = fileSystemSender.sendMessage("<result>ok</result>", filename);
		
		String contentsBase64 = Base64.getEncoder().encodeToString(contents.getBytes());
		// test
		assertEquals(contentsBase64.trim(), actual.trim());
	}

	@Test
	public void mkdirActionTest() throws Exception {
		String filename = "mkdir" + DIR1;
		
		if (_folderExists(filename)) {
			_deleteFolder(filename);
		}

		fileSystemSender.setAction("mkdir");
		fileSystemSender.configure();
		
		String message = "<result>ok</result>";
		String actual;
		
		actual = fileSystemSender.sendMessage(message, filename);
		// test
		assertEquals(message.trim(), actual.trim());
		waitForActionToFinish();
		
		boolean result = _folderExists(filename);
		// test
		assertTrue("Expected file[" + filename + "] to be present", result);
	}

	@Test
	public void rmdirActionTest() throws Exception {
		String filename = DIR1;
		
		if (!_folderExists(DIR1)) {
			_createFolder(filename);
		}

		fileSystemSender.setAction("rmdir");
		fileSystemSender.configure();
		
		String message = "<result>ok</result>";
		String actual;
		
		actual = fileSystemSender.sendMessage(message, filename);
		// test
		assertEquals(message.trim(), actual.trim());
		waitForActionToFinish();
		
		boolean result = _fileExists(filename);
		// test
		assertFalse("Expected file [" + filename + "] " + "not to be present", result);
	}

	@Test
	public void deleteActionTest() throws Exception {
		String filename = "tobedeleted" + FILE1;
		
		if (!_fileExists(filename)) {
			createFile(filename, "is not empty");
		}

		fileSystemSender.setAction("delete");
		fileSystemSender.configure();
		
		String message = "<result>ok</result>";
		String actual;
		actual = fileSystemSender.sendMessage(message, filename);
		// test
		assertEquals(message.trim(), actual.trim());
		waitForActionToFinish();
		
		boolean result = _fileExists(filename);
		// test
		assertFalse("Expected file [" + filename + "] " + "not to be present", result);
	}

	@Test
	public void renameActionTest() throws Exception {
		String filename = "toberenamed" + FILE1;
		String dest = "renamed" + FILE1;
		
		if (!_fileExists(filename)) {
			createFile(filename, "is not empty");
		}

		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(dest);

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("rename");
		fileSystemSender.configure();

		PipeLineSessionBase session = new PipeLineSessionBase();
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		deleteFile(dest);

		String message = "<result>ok</result>";
		String actual;
		actual = fileSystemSender.sendMessage(message, filename, prc);
		// test
		assertEquals(message.trim(), actual.trim());

		boolean result;
		result = _fileExists(filename);
		// test
		assertFalse("Expected file [" + filename + "] " + "not to be present", result);

		result = _fileExists(dest);
		// test
		assertTrue("Expected file [" + dest + "] " + "to be present", result);
	}

	@Test
	public void listActionTest() throws Exception {

		fileSystemSender.setAction("list");
		fileSystemSender.configure();

		String result = fileSystemSender.sendMessage(null, "");

		Iterator<F> it = fileSystem.listFiles();
		int count = 0;
		while (it.hasNext()) {
			it.next();
			count++;
		}

		String[] resultArray = result.split("\"");
		int resultCount = Integer.valueOf(resultArray[3]);
		// test
		assertEquals(resultCount, count);
	}
}