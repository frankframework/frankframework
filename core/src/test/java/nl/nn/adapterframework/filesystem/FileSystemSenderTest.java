package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

public abstract class FileSystemSenderTest<F, FS extends IWritableFileSystem<F>> extends FileSystemTest<F, FS> {

	private FileSystemSender<F, FS> fileSystemSender;

	public FileSystemSender<F, FS> createFileSystemSender() {
		FileSystemSender<F, FS> fileSystemSender = new FileSystemSender<F, FS>();
		fileSystemSender.setFileSystem(fileSystem);
		return fileSystemSender;
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fileSystemSender = createFileSystemSender();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		if (fileSystemSender!=null) {
			fileSystemSender.close();
		};
	}

	@Test
	public void fileSenderTestConfigure() throws Exception {
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
	}

	@Test
	public void fileSenderTestOpen() throws Exception {
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
		fileSystemSender.open();
	}

	@Test
	public void fileSystemSenderUploadActionTestWithString() throws Exception {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTargetwString", contents.getBytes());

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTargetwString");

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("upload");
		fileSystemSender.configure();
		fileSystemSender.open();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);
		String actual;
		
		actual = fileSystemSender.sendMessage("fakecorrelationid", filename, prc);
		waitForActionToFinish();
		
		actual = readFile(null, filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderUploadActionTestWithByteArray() throws Exception {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTargetwByteArray", contents.getBytes());

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTargetwByteArray");

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("upload");
		fileSystemSender.configure();
		fileSystemSender.open();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		String actual;
		actual = fileSystemSender.sendMessage("fakecorrelationid", filename, prc);
		waitForActionToFinish();
		waitForActionToFinish();
		
		actual = readFile(null, filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderUploadActionTestWithInputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
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
		fileSystemSender.open();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		String actual;
		actual = fileSystemSender.sendMessage("fakecorrelationid", filename, prc);
		waitForActionToFinish();

		actual = readFile(null, filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderDownloadActionTest() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(null, filename, contents);
		waitForActionToFinish();

		fileSystemSender.setAction("download");
		fileSystemSender.configure();
		fileSystemSender.open();
		
		String actual;
		actual = fileSystemSender.sendMessage("fakecorrelationid", filename);
		
		String contentsBase64 = Base64.encodeBase64String(contents.getBytes());
		// test
		assertEquals(contentsBase64.trim(), actual.trim());
	}

	public void fileSystemSenderMoveActionTest(String folder1, String folder2) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";
		
		if (folder1!=null) {
			_createFolder(folder1);
		}
		if (folder2!=null) {
			_createFolder(folder2);
		}
		createFile(folder1, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		fileSystemSender.setAction("move");
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(folder2);
		fileSystemSender.addParameter(p);
		fileSystemSender.configure();
		fileSystemSender.open();
		
		String actual;
		ParameterResolutionContext prc = new ParameterResolutionContext(filename,new PipeLineSessionBase());
		actual = fileSystemSender.sendMessage("fakecorrelationid", filename,prc);
		
		assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename));
		assertFalse("file should not exist anymore in original folder ["+folder1+"]", _fileExists(folder1, filename));
//		String contentsBase64 = Base64.encodeBase64String(contents.getBytes());
//		// test
//		assertEquals(contentsBase64.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderMoveActionTestRootToFolder() throws Exception {
		fileSystemSenderMoveActionTest(null,"folder");
	}
//	@Test
//	public void fileSystemSenderMoveActionTestFolderToRoot() throws Exception {
//		fileSystemSenderMoveActionTest("folder",null);
//	}
//	@Test
//	public void fileSystemSenderMoveActionTestFolderToFolder() throws Exception {
//		fileSystemSenderMoveActionTest("folder1","folder2");
//	}

	@Test
	public void fileSystemSenderMkdirActionTest() throws Exception {
		String filename = "mkdir" + DIR1;
		
		if (_folderExists(filename)) {
			_deleteFolder(filename);
		}

		fileSystemSender.setAction("mkdir");
		fileSystemSender.configure();
		fileSystemSender.open();
		
		String message = "fakecorrelationid";
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
		fileSystemSender.open();
		
		String message = "fakecorrelationid";
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
			createFile(null, filename, "is not empty");
		}

		fileSystemSender.setAction("delete");
		fileSystemSender.configure();
		fileSystemSender.open();
		
		String message = "fakecorrelationid";
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
			createFile(null, filename, "is not empty");
		}

		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(dest);

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("rename");
		fileSystemSender.configure();
		fileSystemSender.open();

		PipeLineSessionBase session = new PipeLineSessionBase();
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);

		deleteFile(null, dest);

		String message = "huh?";
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

	public void fileSystemSenderListActionTest(String inputFolder, int numberOfFiles) throws Exception {

		
		for (int i=0; i<numberOfFiles; i++) {
			String filename = "tobelisted"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(inputFolder, filename, "is not empty");
			}
		}
		
		fileSystemSender.setAction("list");
		if (inputFolder!=null) {
			fileSystemSender.setInputFolder(inputFolder);
		}
		fileSystemSender.configure();
		fileSystemSender.open();

		String result = fileSystemSender.sendMessage(null, "");

		log.debug(result);
		
		Iterator<F> it = fileSystem.listFiles(inputFolder);
		int count = 0;
		while (it.hasNext()) {
			it.next();
			count++;
		}
		
		String anchor=" count=\"";
		int posCount=result.indexOf(anchor);
		if (posCount<0) {
			fail("result does not contain anchor ["+anchor+"]");
		}
		int posQuote=result.indexOf('"',posCount+anchor.length());
		
		int resultCount = Integer.valueOf(result.substring(posCount+anchor.length(), posQuote));
		// test
		assertEquals("count mismatch",numberOfFiles, resultCount);
		assertEquals("mismatch in number of files",numberOfFiles, resultCount);
	}

	@Test
	public void fileSystemSenderListActionTestInRootNoFiles() throws Exception {
		fileSystemSenderListActionTest(null,0);
	}
	@Test
	public void fileSystemSenderListActionTestInRoot() throws Exception {
		fileSystemSenderListActionTest(null,2);
	}

	@Test
	public void fileSystemSenderListActionTestInFolderNoFiles() throws Exception {
		_createFolder("folder");
		fileSystemSenderListActionTest("folder",0);
	}

	@Test
	public void fileSystemSenderListActionTestInFolder() throws Exception {
		_createFolder("folder");
		fileSystemSenderListActionTest("folder",2);
	}
	
	@Test(expected = SenderException.class)
	public void fileSystemSenderTestForFolderExistenceWithNonExistingFolder() throws Exception {
		fileSystemSender.setAction("list");
		fileSystemSender.setInputFolder("NonExistentFolder");
		fileSystemSender.configure();
		fileSystemSender.open();
	}

	@Test
	public void fileSystemSenderTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder("folder");
		fileSystemSender.setAction("list");
		fileSystemSender.setInputFolder("folder");
		fileSystemSender.configure();
		fileSystemSender.open();
	}

	@Test()
	public void fileSystemSenderTestForFolderExistenceWithRoot() throws Exception {
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
		fileSystemSender.open();
	}
	
}