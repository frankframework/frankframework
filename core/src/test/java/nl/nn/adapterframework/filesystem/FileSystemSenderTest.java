package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IFileSystem;
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
	public void setUp() throws ConfigurationException, IOException, FileSystemException {
		super.setUp();
		fileSystemSender = createFileSystemSender();
	}

	@Test
	public void fileSystemSenderUploadActionTestWithString() throws Exception {
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
	public void fileSystemSenderUploadActionTestWithByteArray() throws Exception {
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
	public void fileSystemSenderUploadActionTestWithInputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(filename);
		}

		InputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
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
	public void fileSystemSenderDownloadActionTest() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(filename, contents);
		waitForActionToFinish();

		fileSystemSender.setAction("download");
		fileSystemSender.configure();
		
		String actual;
		actual = fileSystemSender.sendMessage("<result>ok</result>", filename);
		
		String contentsBase64 = Base64.encodeBase64String(contents.getBytes());
		// test
		assertEquals(contentsBase64.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderMkdirActionTest() throws Exception {
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
	public void fileSystemSenderRmdirActionTest() throws Exception {
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
	public void fileSystemSenderDeleteActionTest() throws Exception {
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
	public void fileSystemSenderRenameActionTest() throws Exception {
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
	public void fileSystemSenderListActionTest() throws Exception {

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
		
		int resultCount = Integer.valueOf(resultArray[Arrays.asList(resultArray).indexOf(" count=")+1]);
		// test
		assertEquals(count, resultCount);
	}
}