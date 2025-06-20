package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.Parameter;
import org.frankframework.processors.PipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

public abstract class FileSystemPipeTest<FSP extends AbstractFileSystemPipe<F, FS>, F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FSP fileSystemPipe;
	protected PipeRunResult prr;

	public abstract FSP createFileSystemPipe();

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();

		fileSystemPipe = createFileSystemPipe();
		fileSystemPipe.setName(getClass().getSimpleName());
		PipeLine pipeLine = createBeanInAdapter(PipeLine.class);
		fileSystemPipe.setPipeLine(pipeLine);
		pipeLine.addPipe(fileSystemPipe);

		autowireBeanByNameInAdapter(fileSystemPipe);
		fileSystemPipe.addForward(new PipeForward("success",null));
	}

	@Override
	@AfterEach
	public void tearDown() {
		if (fileSystemPipe!=null) {
			fileSystemPipe.stop();
		}
		CloseUtils.closeSilently(prr);

		super.tearDown();
	}

	@Test
	public void fileSystemPipeTestConfigure() throws Exception {
		fileSystemPipe.setAction(FileSystemAction.LIST);
		fileSystemPipe.configure();
	}

	@Test
	public void fileSystemPipeTestOpen() throws Exception {
		fileSystemPipe.setAction(FileSystemAction.LIST);
		fileSystemPipe.configure();
		fileSystemPipe.start();
	}

	@Test
	public void fileSystemPipeUploadActionTestWithString() throws Exception {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwString", contents.getBytes());

		fileSystemPipe.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTargetwString"));
		fileSystemPipe.setAction(FileSystemAction.UPLOAD);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();
		waitForActionToFinish();

		TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemPipeUploadActionTestWithByteArray() throws Exception {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwByteArray", contents.getBytes());

		fileSystemPipe.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTargetwByteArray"));
		fileSystemPipe.setAction(FileSystemAction.UPLOAD);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();
		TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
		waitForActionToFinish();


		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemPipeUploadActionTestWithInputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTarget", stream);

		fileSystemPipe.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTarget"));
		fileSystemPipe.setAction(FileSystemAction.UPLOAD);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();
		TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemPipeDownloadActionTest() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		fileSystemPipe.setAction(FileSystemAction.DOWNLOAD);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();

		// test
		assertEquals(contents.trim(), result.trim(), "result should be base64 of file content");
	}

	public PipeRunResult fileSystemPipeMoveActionTest(String folder1, String folder2, boolean folderExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if(folder1 != null) {
			_createFolder(folder1);
		}
		if(folderExists && folder2 != null) {
			_createFolder(folder2);
		}
		createFile(folder1, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		fileSystemPipe.setAction(FileSystemAction.MOVE);
		fileSystemPipe.addParameter(new Parameter("destination", folder2));
		if (setCreateFolderAttribute) {
			fileSystemPipe.setCreateFolder(true);
		}
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input = new Message(filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		if (prr == null || !prr.isSuccessful()) {
			return prr;
		}
		String result = prr.getResult().asString();

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse(_fileExists(folder1, filename), "file should not exist anymore in original folder ["+folder1+"]");

		return prr;
	}

	@Test
	public void fileSystemPipeMoveActionTestRootToFolder() throws Exception {
		fileSystemPipeMoveActionTest(null,"folder",true,false);
	}
	@Test
	public void fileSystemPipeMoveActionTestRootToFolderCreateFolder() throws Exception {
		fileSystemPipeMoveActionTest(null,"folder",false,true);
	}
	@Test
	public void fileSystemPipeMoveActionTestRootToFolderFailIfolderDoesNotExist() {
		Exception e = assertThrows(Exception.class, () -> fileSystemPipeMoveActionTest(null,"folder",false,false));
		assertThat(e.getMessage(), containsString("unable to process ["+FileSystemAction.MOVE+"] action for File [sendermovefile1.txt]: destination folder [folder] does not exist"));
	}

//	@Test
//	public void fileSystemPipeMoveActionTestFolderToRoot() throws Exception {
//		fileSystemPipeMoveActionTest("folder",null);
//	}
//	@Test
//	public void fileSystemPipeMoveActionTestFolderToFolder() throws Exception {
//		fileSystemPipeMoveActionTest("folder1","folder2");
//	}

	public PipeRunResult fileSystemPipeCreateFile(String folder, boolean fileAlreadyExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "create" + FILE1;

		if(_folderExists(folder)) {
			_deleteFolder(folder);
		}
		waitForActionToFinish();

		if(fileAlreadyExists && !_fileExists(folder, filename)) {
			createFile(folder, filename, "dummy-contents\n");
		}

		fileSystemPipe.setAction(FileSystemAction.CREATE);
		if (setCreateFolderAttribute) {
			fileSystemPipe.setCreateFolder(true);
		}
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input = new Message(folder + "/" + filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		if (prr == null || !prr.isSuccessful()) {
			return prr;
		}
		String result = prr.getResult().asString();

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(folder, filename), "file should exist in destination folder ["+folder+"]");

		return prr;
	}

	@Test
	public void fileSystemPipeCreateFileInFolder() {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeCreateFile("folder1", false, false));
		assertEquals(FileSystemException.class, e.getCause().getClass());
		assertThat(e.getMessage(), containsString("unable to process [CREATE] action for File [folder1/createfile1.txt]"));
	}

	@Test
	public void fileSystemPipeCreateFileAndCreateFolderAttributeEnabled() throws Exception {
		fileSystemPipeCreateFile("folder2", false, true);
	}

	@Test
	public void fileSystemPipeCreatingFileThatAlreadyExists() {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeCreateFile("folder3", true, false));
		assertEquals(FileSystemException.class, e.getCause().getClass());
		assertThat(e.getMessage(), containsString("unable to process [CREATE] action for File [folder3/createfile1.txt]"));
	}

	@Test
	public void fileSystemPipeCreatingFileThatAlreadyExistsAndCreateFolderAttributeEnabled() {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeCreateFile("folder4", true, true));
		assertEquals(FileSystemException.class, e.getCause().getClass());
		assertThat(e.getMessage(), containsString("unable to process [CREATE] action for File [folder4/createfile1.txt]"));
	}

	public PipeRunResult fileSystemPipeWriteFile(String folder, boolean fileAlreadyExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "write" + FILE1;

		if(_folderExists(folder)) {
			_deleteFolder(folder);
		}
		waitForActionToFinish();

		if(fileAlreadyExists && !_fileExists(folder, filename)) {
			createFile(folder, filename, "dummy-contents\n");
		}

		fileSystemPipe.setAction(FileSystemAction.WRITE);
		if (setCreateFolderAttribute) {
			fileSystemPipe.setCreateFolder(true);
		}
		fileSystemPipe.addParameter(ParameterBuilder.create("filename", folder + "/" + filename));
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input = new Message("dummyText");
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		if (prr == null || !prr.isSuccessful()) {
			return prr;
		}
		String result = prr.getResult().asString();

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(folder, filename), "file should exist in destination folder ["+folder+"]");
		assertEquals("dummyText", StreamUtil.streamToString(_readFile(folder, filename)));

		return prr;
	}

	@Test
	public void fileSystemPipeWriteNewFileInFolder() {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeWriteFile("folder1", false, false));
		assertEquals(FileSystemException.class, e.getCause().getClass());
		assertThat(e.getMessage(), containsString("unable to process [WRITE] action for File [folder1/writefile1.txt]"));
	}

	@Test
	public void fileSystemPipeWritingFileAndCreateFolderAttributeEnabled() throws Exception {
		fileSystemPipeWriteFile("folder2", false, true);
	}

	@Test
	public void fileSystemPipeWritingFileThatAlreadyExistsNoForwardsConfiguration() {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeWriteFile("folder3", true, false));
		assertEquals(FileSystemException.class, e.getCause().getClass());
		assertThat(e.getMessage(), containsString("unable to process [WRITE] action for File [folder3/writefile1.txt]"));
	}

	@Test
	public void fileSystemPipeWritingFileThatAlreadyExistsAndCreateFolderAttributeEnabledNoForwardsConfiguration() {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeWriteFile("folder3", true, false));
		assertEquals(FileSystemException.class, e.getCause().getClass());
		assertThat(e.getMessage(), containsString("unable to process [WRITE] action for File [folder3/writefile1.txt]"));
	}

	@Test
	public void fileSystemPipeWritingFileThatAlreadyExists() throws Exception {
		fileSystemPipe.addForward(new PipeForward("fileAlreadyExists", "fileAlreadyExists"));
		prr = fileSystemPipeWriteFile("folder3", true, false);

		assertNotNull(prr);
		assertEquals("fileAlreadyExists", prr.getPipeForward().getName());
	}

	@Test
	public void fileSystemPipeMkdirActionTest() throws Exception {
		String folder = "mkdir" + DIR1;

		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}

		fileSystemPipe.setAction(FileSystemAction.MKDIR);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(folder);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();
		waitForActionToFinish();

		// test

		boolean actual = _folderExists(folder);
		// test
		assertEquals(folder, result, "result of pipe should be name of created folder");
		assertTrue(actual, "Expected folder [" + folder + "] to be present");
	}

	@Test
	public void fileSystemPipeMkdirActionFolderAlreadyExistsForwardConfiguredTest() throws Exception {
		// Arrange
		String folder = "mkdir" + DIR1;

		if (!_folderExists(folder)) {
			_createFolder(folder);
		}

		fileSystemPipe.setAction(FileSystemAction.MKDIR);
		fileSystemPipe.addForward(new PipeForward(FileSystemException.Forward.FOLDER_ALREADY_EXISTS.getForwardName(), "x"));
		fileSystemPipe.configure();
		fileSystemPipe.start();

		// Act
		Message input= new Message(folder);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);

		// Assert
		assertNotNull(prr);
		assertFalse(prr.isSuccessful());
		assertEquals(FileSystemException.Forward.FOLDER_ALREADY_EXISTS.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	public void fileSystemPipeMkdirActionFolderAlreadyExistsSpecificForwardNotConfiguredTest() throws Exception {
		// Arrange
		String folder = "mkdir" + DIR1;

		if (!_folderExists(folder)) {
			_createFolder(folder);
		}

		fileSystemPipe.setAction(FileSystemAction.MKDIR);
		fileSystemPipe.addForward(new PipeForward(FileSystemException.Forward.EXCEPTION.getForwardName(), "x"));
		fileSystemPipe.configure();
		fileSystemPipe.start();

		PipeProcessor pipeProcessor = (PipeProcessor) getConfiguration().getBean("pipeProcessor");

		// Act
		Message input= new Message(folder);
		prr = pipeProcessor.processPipe(fileSystemPipe.getPipeLine(), fileSystemPipe, input, session);
		CloseUtils.closeSilently(input);

		// Assert
		assertNotNull(prr);
		assertFalse(prr.isSuccessful());
		assertEquals(FileSystemException.Forward.EXCEPTION.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	public void fileSystemPipeMkdirActionFolderAlreadyExistsNoForwardsConfiguredTest() throws Exception {
		// Arrange
		String folder = "mkdir" + DIR1;

		if (!_folderExists(folder)) {
			_createFolder(folder);
		}

		fileSystemPipe.setAction(FileSystemAction.MKDIR);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		// Act
		Message input= new Message(folder);
		PipeRunException pre = assertThrows(PipeRunException.class, ()->fileSystemPipe.doPipe(input, session));
		CloseUtils.closeSilently(input);

		// Assert
		assertInstanceOf(FileSystemException.class, pre.getCause());
		assertEquals(FileSystemException.Forward.FOLDER_ALREADY_EXISTS.getForwardName(), ((FileSystemException) pre.getCause()).getForward().getForwardName());
	}

	@Test
	public void fileSystemPipeRmdirActionTest() throws Exception {
		String folder = DIR1;

		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}

		fileSystemPipe.setAction(FileSystemAction.RMDIR);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(folder);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();

		// test
		assertEquals(folder, result, "result of pipe should be name of removed folder");
		waitForActionToFinish();

		boolean actual = _fileExists(folder);
		// test
		assertFalse(actual, "Expected file [" + folder + "] " + "not to be present");
	}

	@Test
	public void fileSystemPipeRmdirActionFolderDoesNotExistForwardConfiguredTest() throws Exception {
		String folder = DIR1;

		if (_folderExists(DIR1)) {
			_deleteFolder(folder);
		}

		fileSystemPipe.setAction(FileSystemAction.RMDIR);
		fileSystemPipe.addForward(new PipeForward(FileSystemException.Forward.FOLDER_NOT_FOUND.getForwardName(), "x"));
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(folder);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);

		// Assert
		assertNotNull(prr);
		assertFalse(prr.isSuccessful());
		assertEquals(FileSystemException.Forward.FOLDER_NOT_FOUND.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	public void fileSystemPipeRmdirActionFolderDoesNotExistSpecificForwardNotConfiguredTest() throws Exception {
		String folder = DIR1;

		if (_folderExists(DIR1)) {
			_deleteFolder(folder);
		}

		fileSystemPipe.setAction(FileSystemAction.RMDIR);
		fileSystemPipe.addForward(new PipeForward(FileSystemException.Forward.EXCEPTION.getForwardName(), "x"));
		fileSystemPipe.configure();
		fileSystemPipe.start();

		PipeProcessor pipeProcessor = (PipeProcessor) getConfiguration().getBean("pipeProcessor");

		Message input= new Message(folder);
		prr = pipeProcessor.processPipe(fileSystemPipe.getPipeLine(), fileSystemPipe, input, session);
		CloseUtils.closeSilently(input);

		// Assert
		assertNotNull(prr);
		assertFalse(prr.isSuccessful());
		assertEquals(FileSystemException.Forward.EXCEPTION.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	public void fileSystemPipeRmdirActionFolderDoesNotExistNoForwardsConfiguredTest() throws Exception {
		String folder = DIR1;

		if (_folderExists(DIR1)) {
			_deleteFolder(folder);
		}

		fileSystemPipe.setAction(FileSystemAction.RMDIR);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(folder);
		PipeRunException pre = assertThrows(PipeRunException.class, ()->fileSystemPipe.doPipe(input, session));
		CloseUtils.closeSilently(input);

		// Assert
		assertInstanceOf(FileSystemException.class, pre.getCause());
		assertEquals(FileSystemException.Forward.FOLDER_NOT_FOUND.getForwardName(), ((FileSystemException) pre.getCause()).getForward().getForwardName());
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

		fileSystemPipe.setRemoveNonEmptyFolder(true);
		fileSystemPipe.setAction(FileSystemAction.RMDIR);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(folder);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();

		// test
		assertEquals(folder, result, "result of pipe should be name of removed folder");
		waitForActionToFinish();

		boolean actual = _fileExists(folder);
		// test
		assertFalse(actual, "Expected file [" + folder + "] " + "not to be present");
	}

	@Test
	public void fileSystemPipeDeleteActionTest() throws Exception {
		String filename = "tobedeleted" + FILE1;

		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		fileSystemPipe.setAction(FileSystemAction.DELETE);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message(filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();

		waitForActionToFinish();

		boolean actual = _fileExists(filename);
		// test
		assertEquals(filename, result, "result of pipe should be name of deleted file");
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");
	}

	@Test
	public void fileSystemPipeRenameActionTest() throws Exception {
		String filename = "toberenamed" + FILE1;
		String dest = "renamed" + FILE1;

		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		fileSystemPipe.addParameter(new Parameter("destination", dest));
		fileSystemPipe.setAction(FileSystemAction.RENAME);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		deleteFile(null, dest);

		Message input= new Message(filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		String result=prr.getResult().asString();

		// test
		assertEquals(dest, result, "result of pipe should be name of new file");

		boolean actual = _fileExists(filename);
		// test
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");

		actual = _fileExists(dest);
		// test
		assertTrue(actual, "Expected file [" + dest + "] " + "to be present");
	}

	public void fileSystemPipeListActionTest(String inputFolder, int numberOfFiles) throws Exception {


		for (int i=0; i<numberOfFiles; i++) {
			String filename = "tobelisted"+i + FILE1;

			if (!_fileExists(filename)) {
				createFile(inputFolder, filename, "is not empty");
			}
		}

		fileSystemPipe.setAction(FileSystemAction.LIST);
		if (inputFolder!=null) {
			fileSystemPipe.setInputFolder(inputFolder);
		}
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message input= new Message("");
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		Message result = prr.getResult();

		log.debug(result.asString());

		// TODO test that the fileSystemPipe has returned the an XML with the details of the file
//		Iterator<F> it = result;
//		int count = 0;
//		while (it.hasNext()) {
//			it.next();
//			count++;
//		}

		assertFileCountEquals(result, numberOfFiles);
		result.close();
	}

	@Test
	public void fileSystemPipeListActionTestInRootNoFiles() throws Exception {
		fileSystemPipeListActionTest(null,0);
	}
	@Test
	public void fileSystemPipeListActionTestInRoot() throws Exception {
		fileSystemPipeListActionTest(null,2);
	}

	@Test
	public void fileSystemPipeListActionTestInFolderNoFiles() throws Exception {
		_createFolder("folder");
		fileSystemPipeListActionTest("folder",0);
	}

	@Test
	public void fileSystemPipeListActionTestInFolder() throws Exception {
		_createFolder("folder");
		fileSystemPipeListActionTest("folder",2);
	}

	@Test
	public void fileSystemPipeTestForFolderExistenceWithNonExistingFolder() throws Exception {
		fileSystemPipe.setAction(FileSystemAction.LIST);
		fileSystemPipe.setInputFolder("NonExistentFolder");
		fileSystemPipe.setName("FSP");
		fileSystemPipe.configure();

		LifecycleException e = assertThrows(LifecycleException.class, fileSystemPipe::start);
		assertThat(e.getMessage(), startsWith("Pipe [FSP]: Cannot open fileSystem"));
	}

	@Test
	public void fileSystemPipeTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder("folder");
		fileSystemPipe.setAction(FileSystemAction.LIST);
		fileSystemPipe.setInputFolder("folder");
		fileSystemPipe.configure();
		fileSystemPipe.start();
	}

	@Test()
	public void fileSystemPipeTestForFolderExistenceWithRoot() throws Exception {
		fileSystemPipe.setAction(FileSystemAction.LIST);
		fileSystemPipe.configure();
		fileSystemPipe.start();
	}

	@Test
	public void fileSystemPipeListActionTestWithInputFolderAsParameter() throws Exception {
		String filename = FILE1;
		String filename2 = FILE2;
		String inputFolder = "directory";

		if (_fileExists(inputFolder, filename)) {
			_deleteFile(inputFolder, filename);
		}

		if (_fileExists(inputFolder, filename2)) {
			_deleteFile(inputFolder, filename2);
		}
		_createFolder(inputFolder);

		fileSystemPipe.addParameter(new Parameter("inputFolder", inputFolder));
		fileSystemPipe.setAction(FileSystemAction.LIST);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		createFile(inputFolder, filename, "some content");
		waitForActionToFinish();
		assertTrue(_fileExists(inputFolder, filename), "File ["+filename+"]expected to be present");

		createFile(inputFolder, filename2, "some content of second file");
		waitForActionToFinish();
		assertTrue(_fileExists(inputFolder, filename2), "File ["+filename2+"]expected to be present");

		Message input= new Message(filename);
		prr = fileSystemPipe.doPipe(input, session);
		CloseUtils.closeSilently(input);
		assertNotNull(prr);
		Message result = prr.getResult();
		waitForActionToFinish();

		assertFileCountEquals(result, 2);
		result.close();
	}
}
