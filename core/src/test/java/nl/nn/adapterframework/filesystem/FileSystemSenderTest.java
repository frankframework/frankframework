package nl.nn.adapterframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.filesystem.FileSystemActor.FileSystemAction;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.Base64Pipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestAssertions;

public abstract class FileSystemSenderTest<FSS extends FileSystemSender<F, FS>, F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FSS fileSystemSender;

	public abstract FSS createFileSystemSender();

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();

		fileSystemSender = createFileSystemSender();
		autowireByName(fileSystemSender);
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		if (fileSystemSender!=null) {
			fileSystemSender.close();
		}

		super.tearDown();
	}

	@Test
	public void fileSystemSenderTestConfigure() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.configure();
	}

	@Test
	public void fileSystemSenderTestOpen() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
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

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwString", contents.getBytes());

		fileSystemSender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTargetwString"));
		fileSystemSender.setAction(FileSystemAction.UPLOAD);
		fileSystemSender.configure();
		fileSystemSender.open();

		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderUploadActionTestWithByteArray() throws Exception {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwByteArray", contents.getBytes());

		fileSystemSender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTargetwByteArray"));
		fileSystemSender.setAction(FileSystemAction.UPLOAD);
		fileSystemSender.configure();
		fileSystemSender.open();

		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();


		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
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
		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTarget", stream);

		fileSystemSender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTarget"));
		fileSystemSender.setAction(FileSystemAction.UPLOAD);
		fileSystemSender.configure();
		fileSystemSender.open();

		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderUploadActionTestWithOutputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();

		fileSystemSender.addParameter(new Parameter("filename", filename));

		fileSystemSender.setAction(FileSystemAction.UPLOAD);
		fileSystemSender.configure();
		fileSystemSender.open();

		//assertTrue(fileSystemSender.canProvideOutputStream());

		MessageOutputStream target = fileSystemSender.provideOutputStream(session, null);
		assertNotNull(target);

		// stream the contents
		try (Writer writer = target.asWriter()) {
			writer.write(contents);
		}

		// verify the filename is properly returned
		String stringResult=target.getPipeRunResult().getResult().asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");

		// verify the file contents
		waitForActionToFinish();
		String actualContents = readFile(null, filename);
		assertEquals(contents,actualContents);
	}

	@Test
	public void fileSystemSenderDownloadActionTest() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		fileSystemSender.setAction(FileSystemAction.DOWNLOAD);
		fileSystemSender.configure();
		fileSystemSender.open();

		PipeLineSession session = new PipeLineSession();
		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		assertEquals(contents.trim(), result.asString().trim(), "result should be base64 of file content");
	}

	@Test
	public void fileSystemSenderDownloadActionBase64Test() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		fileSystemSender.setAction(FileSystemAction.DOWNLOAD);
		fileSystemSender.configure();
		fileSystemSender.setBase64(Base64Pipe.Direction.ENCODE);
		fileSystemSender.open();

		PipeLineSession session = new PipeLineSession();
		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);

		String contentsBase64 = Base64.encodeBase64String(contents.getBytes());
		// test
		assertEquals(contentsBase64.trim(), result.asString().trim(), "result should be base64 of file content");
	}

	public void fileSystemSenderMoveActionTest(String folder1, String folder2, boolean folderShouldExist, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if (folder2!=null) {
			_deleteFolder(folder2);
		}
		if (folder1!=null) {
			_createFolder(folder1);
		}
		if (folderShouldExist && folder2!=null) {
			_createFolder(folder2);
		}
		createFile(folder1, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		fileSystemSender.setAction(FileSystemAction.MOVE);
		fileSystemSender.addParameter(new Parameter("destination", folder2));
		if (setCreateFolderAttribute) {
			fileSystemSender.setCreateFolder(true);
		}
		fileSystemSender.configure();
		fileSystemSender.open();

		PipeLineSession session = new PipeLineSession();
		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse(_fileExists(folder1, filename), "file should not exist anymore in original folder ["+folder1+"]");
	}

	@Test
	public void fileSystemSenderMoveActionTestRootToFolder() throws Exception {
		fileSystemSenderMoveActionTest(null,"folder",true,false);
	}
	@Test
	public void fileSystemSenderMoveActionTestRootToFolderCreateFolder() throws Exception {
		fileSystemSenderMoveActionTest(null,"folder",false,true);
	}
	@Test
	public void fileSystemSenderMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
		Exception e = assertThrows(Exception.class, () -> fileSystemSenderMoveActionTest(null,"folder",false,false));
		assertThat(e.getMessage(), containsString("unable to process ["+FileSystemAction.MOVE+"] action for File [sendermovefile1.txt]: destination folder [folder] does not exist"));
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
		String folder = "mkdir" + DIR1;

		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}

		fileSystemSender.setAction(FileSystemAction.MKDIR);
		fileSystemSender.configure();
		fileSystemSender.open();

		PipeLineSession session = new PipeLineSession();
		Message message=new Message(folder);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		// test

		boolean actual = _folderExists(folder);
		// test
		assertEquals(folder, result.asString(), "result of sender should be name of created folder");
		assertTrue(actual, "Expected folder [" + folder + "] to be present");
	}

	@Test
	public void fileSystemSenderRmdirActionTest() throws Exception {
		String folder = DIR1;

		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}

		fileSystemSender.setAction(FileSystemAction.RMDIR);
		fileSystemSender.configure();
		fileSystemSender.open();

		PipeLineSession session = new PipeLineSession();
		Message message=new Message(folder);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		assertEquals(folder, result.asString(), "result of sender should be name of deleted folder");
		waitForActionToFinish();

		boolean actual = _folderExists(folder);
		// test
		assertFalse(actual, "Expected folder [" + folder + "] " + "not to be present");
	}

	@Test
	public void fileSystemPipeRmNonEmptyDirActionTest() throws Exception {
		String folder = DIR1;
		String innerFolder = DIR1+"/innerFolder";
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}
		if (!_folderExists(innerFolder)) {
			_createFolder(innerFolder);
		}

		for (int i=0; i < 3; i++) {
			String filename = "file"+i + FILE1;
			createFile(folder, filename, "is not empty");
			createFile(innerFolder, filename, "is not empty");
		}

		fileSystemSender.setRemoveNonEmptyFolder(true);
		fileSystemSender.setAction(FileSystemAction.RMDIR);
		fileSystemSender.configure();
		fileSystemSender.open();

		PipeLineSession session = new PipeLineSession();
		Message message=new Message(folder);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		assertEquals(folder,result.asString(), "result of sender should be name of deleted folder");
		waitForActionToFinish();

		boolean actual = _folderExists(folder);
		// test
		assertFalse(actual, "Expected folder [" + folder + "] " + "not to be present");
	}
	@Test
	public void fileSystemSenderDeleteActionTest() throws Exception {
		String filename = "tobedeleted" + FILE1;

		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		fileSystemSender.setAction(FileSystemAction.DELETE);
		fileSystemSender.configure();
		fileSystemSender.open();

		PipeLineSession session = new PipeLineSession();
		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);

		waitForActionToFinish();

		boolean actual = _fileExists(filename);
		// test
		assertEquals(filename, result.asString(), "result of sender should be name of deleted file");
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");
	}

	@Test
	public void fileSystemSenderRenameActionTest() throws Exception {
		String filename = "toberenamed" + FILE1;
		String dest = "renamed" + FILE1;

		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		fileSystemSender.addParameter(new Parameter("destination", dest));
		fileSystemSender.setAction(FileSystemAction.RENAME);
		fileSystemSender.configure();
		fileSystemSender.open();

		deleteFile(null, dest);

		PipeLineSession session = new PipeLineSession();
		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		assertEquals(dest, result.asString(), "result of sender should be new name of file");

		boolean actual = _fileExists(filename);
		// test
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");

		actual = _fileExists(dest);
		// test
		assertTrue(actual, "Expected file [" + dest + "] " + "to be present");
	}

	public void fileSystemSenderListActionTest(String inputFolder, int numberOfFiles) throws Exception {
		_deleteFolder(inputFolder);
		if(inputFolder != null) {
			_createFolder("folder");
		}

		for (int i=0; i<numberOfFiles; i++) {
			String filename = "tobelisted"+i + FILE1;

			if (!_fileExists(filename)) {
				createFile(inputFolder, filename, "is not empty");
			}
		}

		fileSystemSender.setAction(FileSystemAction.LIST);
		if (inputFolder!=null) {
			fileSystemSender.setInputFolder(inputFolder);
		}
		fileSystemSender.configure();
		fileSystemSender.open();

		PipeLineSession session = new PipeLineSession();
		Message message=new Message("");
		Message result = fileSystemSender.sendMessageOrThrow(message, session);

		log.debug(result);
		assertFileCountEquals(result, numberOfFiles);
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
		fileSystemSenderListActionTest("folder",0);
	}

	@Test
	public void fileSystemSenderListActionTestInFolder() throws Exception {
		fileSystemSenderListActionTest("folder",2);
	}

	@Test
	public void fileSystemSenderTestForFolderExistenceWithNonExistingFolder() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.setInputFolder("NonExistentFolder");
		fileSystemSender.configure();

		SenderException e = assertThrows(SenderException.class, fileSystemSender::open);
		assertThat(e.getMessage(), startsWith("Cannot open fileSystem"));
	}

	@Test
	public void fileSystemSenderTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder("folder");
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.setInputFolder("folder");
		fileSystemSender.configure();
		fileSystemSender.open();
	}

	@Test()
	public void fileSystemSenderTestForFolderExistenceWithRoot() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.configure();
		fileSystemSender.open();
	}

	@Test
	public void fileSystemSenderListActionTestWithInputFolderAsParameter() throws Exception {
		String filename = FILE1;
		String filename2 = FILE2;
		String inputFolder = "directory";

		if (_fileExists(inputFolder, filename)) {
			_deleteFile(inputFolder, filename);
		}

		if (_fileExists(inputFolder, filename2)) {
			_deleteFile(inputFolder, filename2);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("listWithInputFolderAsParameter", inputFolder);

		fileSystemSender.addParameter(ParameterBuilder.create().withName("inputFolder").withSessionKey("listWithInputFolderAsParameter"));
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.configure();
		fileSystemSender.open();

		_createFolder(inputFolder);
		OutputStream out = _createFile(inputFolder, filename);
		out.write("some content".getBytes());
		out.close();
		waitForActionToFinish();
		assertTrue(_fileExists(inputFolder, filename), "File ["+filename+"]expected to be present");

		OutputStream out2 = _createFile(inputFolder, filename2);
		out2.write("some content of second file".getBytes());
		out2.close();
		waitForActionToFinish();
		assertTrue(_fileExists(inputFolder, filename2), "File ["+filename2+"]expected to be present");

		Message message=new Message(filename);
		Message result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		assertFileCountEquals(result, 2);
	}
}