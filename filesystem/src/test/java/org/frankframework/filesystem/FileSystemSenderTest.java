package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderResult;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.UUIDUtil;

public abstract class FileSystemSenderTest<FSS extends AbstractFileSystemSender<F, FS>, F, FS extends IBasicFileSystem<F>> extends HelperedFileSystemTestBase {

	private static final String FOLDER_NAME = "folder";
	protected FSS fileSystemSender;
	private SenderResult senderResult;
	private Message result;

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
	public void tearDown() {
		CloseUtils.closeSilently(senderResult);
		try {
			if (fileSystemSender != null) {
				fileSystemSender.stop();
			}
		} catch (LifecycleException e) {
			log.warn("Failed to close fileSystemSender", e);
		}
		CloseUtils.closeSilently(result);
		super.tearDown();
	}

	@Test
	public void fileSystemSenderTestConfigure() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.configure();
	}

	@Test
	public void fileSystemSenderTestStart() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.configure();
		fileSystemSender.start();
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
		fileSystemSender.start();

		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);
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
		fileSystemSender.start();

		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);
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

		InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTarget", stream);

		fileSystemSender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTarget"));
		fileSystemSender.setAction(FileSystemAction.UPLOAD);
		fileSystemSender.configure();
		fileSystemSender.start();

		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderDownloadActionTest() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		fileSystemSender.setAction(FileSystemAction.DOWNLOAD);
		fileSystemSender.configure();
		fileSystemSender.start();

		PipeLineSession session = new PipeLineSession();
		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		assertEquals(contents.trim(), result.asString().trim(), "result should be base64 of file content");
	}

	public void fileSystemSenderMoveActionTest(String folder1, String folder2, boolean folderShouldExist, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if (folder2 != null) {
			_deleteFolder(folder2);
		}
		if (folder1 != null) {
			_createFolder(folder1);
		}
		if (folderShouldExist && folder2 != null) {
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
		fileSystemSender.start();

		PipeLineSession session = new PipeLineSession();
		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);

		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse(_fileExists(folder1, filename), "file should not exist anymore in original folder [" + folder1 + "]");
	}

	@Test
	public void fileSystemSenderMoveActionTestRootToFolder() throws Exception {
		fileSystemSenderMoveActionTest(null, FOLDER_NAME, true, false);
	}

	@Test
	public void fileSystemSenderMoveActionTestRootToFolderCreateFolder() throws Exception {
		fileSystemSenderMoveActionTest(null, FOLDER_NAME, false, true);
	}

	@Test
	public void fileSystemSenderMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
		Exception e = assertThrows(Exception.class, () -> fileSystemSenderMoveActionTest(null, FOLDER_NAME, false, false));
		assertThat(e.getMessage(), containsString("unable to process [" + FileSystemAction.MOVE + "] action for File [sendermovefile1.txt]: destination folder [folder] does not exist"));
	}

	@Test
	public void moveFileParamPrefixedWithFolderToAnotherFolder() throws Exception {
		// Arrange
		String testFileContents = UUIDUtil.createRandomUUID();
		String inputFolder = "tests";
		String outputFolder = "tests/processed";

		_deleteFolder(inputFolder); // ensure all test folders are empty
		_createFolder(inputFolder);
		createFile(inputFolder, FILE1, testFileContents);

		waitForActionToFinish();

		fileSystemSender.setAction(FileSystemAction.MOVE);
		fileSystemSender.addParameter(ParameterBuilder.create("filename", inputFolder + "/" + FILE1));
		fileSystemSender.addParameter(ParameterBuilder.create("destination", outputFolder));
		fileSystemSender.setCreateFolder(true);

		// Act
		fileSystemSender.configure();
		fileSystemSender.start();

		Message message = new Message("is-not-relevant");
		result = fileSystemSender.sendMessageOrThrow(message, session);

		// Assert
		assertNotNull(result);
		String newFilename = result.asString();
		assertEquals(FILE1, newFilename);

		assertTrue(_fileExists(outputFolder, newFilename), "file should exist in destination folder [" + outputFolder + "]");
		assertFalse(_fileExists(inputFolder, FILE1), "file should not exist anymore in original folder [" + outputFolder + "]");
		String newFileContents = readFile(outputFolder, newFilename);
		assertEquals(testFileContents, newFileContents);
	}

	@Test
	public void fileSystemSenderMkdirActionTest() throws Exception {
		String folder = "mkdir" + DIR1;

		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}

		fileSystemSender.setAction(FileSystemAction.MKDIR);
		fileSystemSender.configure();
		fileSystemSender.start();

		PipeLineSession session = new PipeLineSession();
		Message message = new Message(folder);
		result = fileSystemSender.sendMessageOrThrow(message, session);
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
		fileSystemSender.start();

		PipeLineSession session = new PipeLineSession();
		Message message = new Message(folder);
		result = fileSystemSender.sendMessageOrThrow(message, session);

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
		String innerFolder = DIR1 + "/innerFolder";
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}
		if (!_folderExists(innerFolder)) {
			_createFolder(innerFolder);
		}

		for (int i = 0; i < 3; i++) {
			String filename = "file" + i + FILE1;
			createFile(folder, filename, "is not empty");
			createFile(innerFolder, filename, "is not empty");
		}

		fileSystemSender.setRemoveNonEmptyFolder(true);
		fileSystemSender.setAction(FileSystemAction.RMDIR);
		fileSystemSender.configure();
		fileSystemSender.start();

		PipeLineSession session = new PipeLineSession();
		Message message = new Message(folder);
		result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		assertEquals(folder, result.asString(), "result of sender should be name of deleted folder");
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
		fileSystemSender.start();

		PipeLineSession session = new PipeLineSession();
		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);

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
		fileSystemSender.start();

		deleteFile(null, dest);

		PipeLineSession session = new PipeLineSession();
		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		assertEquals(dest, result.asString(), "result of sender should be new name of file");

		boolean actual = _fileExists(filename);
		// test
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");

		actual = _fileExists(dest);
		// test
		assertTrue(actual, "Expected file [" + dest + "] " + "to be present");
	}

	public void fileSystemSenderListActionTest(String inputFolder, int numberOfItems, TypeFilter typeFilter) throws Exception {
		if (inputFolder != null) {
			_deleteFolder(inputFolder);
			_createFolder(inputFolder);
		}

		for (int i = 0; i < numberOfItems; i++) {
			String fileName = "toBeListedFile" + i;
			String folderName = (inputFolder != null) ? inputFolder + "/" : "";
			folderName += "toBeListedFolder" + i;

			if (typeFilter.includeFiles() && !_fileExists(fileName)) {
				createFile(inputFolder, fileName, "is not empty");
			}
			if (typeFilter.includeFolders() && !_fileExists(folderName)) {
				_createFolder(folderName);
			}
		}

		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.setTypeFilter(typeFilter);
		fileSystemSender.setInputFolder(inputFolder);
		fileSystemSender.configure();
		fileSystemSender.start();

		PipeLineSession session = new PipeLineSession();
		Message message = Message.nullMessage();
		result = fileSystemSender.sendMessageOrThrow(message, session);

		log.debug("Result: {}", result.asString());
		if (typeFilter.includeFiles()) {
			assertFileCountEquals(result, numberOfItems);
		}
		if (typeFilter.includeFolders()) {
			assertFolderCountEquals(result, numberOfItems);
		}
	}

	@Test
	public void fileSystemSenderListActionTestInRootNoFiles() throws Exception {
		fileSystemSenderListActionTest(null, 0, TypeFilter.FILES_ONLY);
	}

	@Test
	public void fileSystemSenderListActionTestInRoot() throws Exception {
		fileSystemSenderListActionTest(null, 2, TypeFilter.FILES_ONLY);
	}

	@Test
	public void fileSystemSenderListActionTestInFolderNoFiles() throws Exception {
		fileSystemSenderListActionTest(FOLDER_NAME, 0, TypeFilter.FILES_ONLY);
	}

	@Test
	public void fileSystemSenderListActionTestInFolder() throws Exception {
		fileSystemSenderListActionTest(FOLDER_NAME, 2, TypeFilter.FILES_ONLY);
	}

	@Test
	public void fileSystemSenderListFilesAndFoldersActionTestInFolder() throws Exception {
		fileSystemSenderListActionTest(FOLDER_NAME, 2, TypeFilter.FILES_AND_FOLDERS);
	}

	@Test
	public void fileSystemSenderListFoldersActionTestInFolder() throws Exception {
		fileSystemSenderListActionTest(null, 3, TypeFilter.FOLDERS_ONLY);
	}

	public SenderResult fileSystemSenderCreateFile(String folder, boolean fileAlreadyExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "create" + FILE1;

		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}
		waitForActionToFinish();

		if (fileAlreadyExists) {
			createFile(folder, filename, "dummy-contents\n");
		}

		fileSystemSender.setAction(FileSystemAction.CREATE); //TODO WRITE
		if (setCreateFolderAttribute) {
			fileSystemSender.setCreateFolder(true);
		}
		fileSystemSender.configure();
		fileSystemSender.start();

		Message input = new Message(folder + "/" + filename);
		senderResult = fileSystemSender.sendMessage(input, session);
		CloseUtils.closeSilently(input);
		if (!senderResult.isSuccess()) {
			return senderResult;
		}
		String result = senderResult.getResult().asString();

		// Result should be the name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(folder, filename), "file should exist in destination folder [" + folder + "]");
		return senderResult;
	}

	@Test
	public void fileSystemSenderCreateFile() throws Exception {
		senderResult = fileSystemSenderCreateFile(FOLDER_NAME, false, false);
		assertFalse(senderResult.isSuccess());
		assertEquals("folderNotFound", senderResult.getForwardName());
		assertThat(senderResult.getErrorMessage(), containsString("unable to process [CREATE] action for File [folder/createfile1.txt]"));
		assertThat(senderResult.getErrorMessage(), containsString("folder] does not exist"));
	}

	@Test
	public void fileSystemSenderCreateFileAlreadyExists() throws Exception {
		senderResult = fileSystemSenderCreateFile(FOLDER_NAME, true, false);
		assertFalse(senderResult.isSuccess());
		assertEquals("fileAlreadyExists", senderResult.getForwardName());
		assertThat(senderResult.getErrorMessage(), containsString("unable to process [CREATE] action for File [folder/createfile1.txt]"));
		assertThat(senderResult.getErrorMessage(), containsString("createfile1.txt] already exists"));
	}

	@Test
	public void fileSystemSenderCreateFileAndCreateFolderAttributeEnabled() throws Exception {
		fileSystemSenderCreateFile(FOLDER_NAME, false, true);
	}

	public SenderResult fileSystemSenderWriteFile(String folder, boolean fileAlreadyExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "write" + FILE1;

		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}
		waitForActionToFinish();

		if (fileAlreadyExists && !_fileExists(folder, filename)) {
			createFile(folder, filename, "dummy-contents\n");
		}

		fileSystemSender.setAction(FileSystemAction.WRITE);
		fileSystemSender.setCreateFolder(setCreateFolderAttribute);
		fileSystemSender.addParameter(ParameterBuilder.create("filename", folder + "/" + filename));
		fileSystemSender.configure();
		fileSystemSender.start();

		Message input = new Message("dummyText");
		senderResult = fileSystemSender.sendMessage(input, session);
		CloseUtils.closeSilently(input);
		if (!senderResult.isSuccess()) {
			return senderResult;
		}
		String result = senderResult.getResult().asString();

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(folder, filename), "file should exist in destination folder [" + folder + "]");
		assertEquals("dummyText", StreamUtil.streamToString(_readFile(folder, filename)));
		return senderResult;
	}

	@Test
	public void fileSystemSenderWriteNewFileInFolder() throws Exception {
		senderResult = fileSystemSenderWriteFile("folder1", false, false);
		assertFalse(senderResult.isSuccess());
		assertEquals("folderNotFound", senderResult.getForwardName());
		assertThat(senderResult.getErrorMessage(), containsString("unable to process [WRITE] action for File [folder1/writefile1.txt]"));
		assertThat(senderResult.getErrorMessage(), containsString("folder1] does not exist"));
	}

	@Test
	public void fileSystemSenderWritingFileAndCreateFolderAttributeEnabled() throws Exception {
		fileSystemSenderWriteFile("folder2", false, true);
	}

	@Test
	public void fileSystemSenderWritingFileThatAlreadyExists() throws Exception {
		senderResult = fileSystemSenderWriteFile("folder3", true, false);
		assertFalse(senderResult.isSuccess());
		assertEquals("fileAlreadyExists", senderResult.getForwardName());
		assertThat(senderResult.getErrorMessage(), containsString("unable to process [WRITE] action for File [folder3/writefile1.txt]"));
		assertThat(senderResult.getErrorMessage(), containsString("writefile1.txt] already exists"));
	}

	@Test
	public void fileSystemSenderWritingFileThatAlreadyExistsAndCreateFolderAttributeEnabled() throws Exception {
		senderResult = fileSystemSenderWriteFile("folder4", true, true);
		assertFalse(senderResult.isSuccess());
		assertEquals("fileAlreadyExists", senderResult.getForwardName());
		assertThat(senderResult.getErrorMessage(), containsString("unable to process [WRITE] action for File [folder4/writefile1.txt]"));
		assertThat(senderResult.getErrorMessage(), containsString("writefile1.txt] already exists"));
	}

	@Test
	public void fileSystemSenderTestForFolderExistenceWithNonExistingFolder() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.setInputFolder("NonExistentFolder");
		fileSystemSender.configure();

		LifecycleException e = assertThrows(LifecycleException.class, fileSystemSender::start);
		assertThat(e.getMessage(), startsWith("Cannot open fileSystem"));
	}

	@Test
	public void fileSystemSenderTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder(FOLDER_NAME);
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.setTypeFilter(TypeFilter.FILES_ONLY);
		fileSystemSender.setInputFolder(FOLDER_NAME);
		fileSystemSender.configure();
		fileSystemSender.start();
	}

	@Test()
	public void fileSystemSenderTestForFolderExistenceWithRoot() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.configure();
		fileSystemSender.start();
	}

	@Test
	public void fileSystemSenderListActionTestWithInputFolderAsParameter() throws Exception {
		// Arrange
		String filename = FILE1;
		String filename2 = FILE2;
		String inputFolder = "directory";

		if (_folderExists(inputFolder)) {
			_deleteFolder(inputFolder);
		}
		_createFolder(inputFolder);

		createFile(inputFolder, filename, "some content");
		createFile(inputFolder, filename2, "some content of the second file");

		_createFolder(inputFolder + "/subfolder");
		createFile(inputFolder + "/subfolder", "dont-list-me.txt", "content of the third file");

		waitForActionToFinish();
		session.put("listWithInputFolderAsParameter", inputFolder);

		fileSystemSender.addParameter(ParameterBuilder.create().withName("inputFolder").withSessionKey("listWithInputFolderAsParameter"));
		fileSystemSender.addParameter(ParameterBuilder.create()
				.withName(FileSystemActor.PARAMETER_TYPEFILTER)
				.withSessionKey(TypeFilter.FILES_ONLY.toString().toLowerCase()));
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.configure();
		fileSystemSender.start();

		// Act
		assertTrue(_fileExists(inputFolder, filename), "File [" + filename + "] expected to be present");
		assertTrue(_fileExists(inputFolder, filename2), "File [" + filename2 + "] expected to be present");

		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		// Assert
		assertFileCountEquals(result, 2); //2 files and 1 folder (which should be omitted from the result)
	}

	@Test
	public void fileSystemSenderTestReadDelete() throws Exception {
		// Arrange
		String filename = FILE1;
		String inputFolder = "read-delete";

		if (_folderExists(inputFolder)) {
			_deleteFolder(inputFolder);
		}
		_createFolder(inputFolder);

		createFile(inputFolder, filename, "some content");

		waitForActionToFinish();

		fileSystemSender.addParameter(ParameterBuilder.create("filename", inputFolder + "/" + filename));
		fileSystemSender.setAction(FileSystemAction.READDELETE);
		fileSystemSender.configure();
		fileSystemSender.start();

		// Act
		assertTrue(_fileExists(inputFolder, filename), "File [" + filename + "] expected to be present");

		Message message = new Message("not-used");
		result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		// Assert
		result.preserve(); //read the stream else close wont be called... sigh
		assertFalse(_fileExists(inputFolder, FILE1), "File [" + FILE1 + "] should have been deleted after READ action");
		assertEquals("some content", result.asString());
	}
}
