package nl.nn.adapterframework.filesystem;

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
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.filesystem.FileSystemActor.FileSystemAction;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.StreamUtil;

public abstract class FileSystemPipeTest<FSP extends FileSystemPipe<F, FS>, F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FSP fileSystemPipe;

	public abstract FSP createFileSystemPipe();

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();

		fileSystemPipe = createFileSystemPipe();
		autowireByName(fileSystemPipe);
		fileSystemPipe.registerForward(new PipeForward("success",null));
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		if (fileSystemPipe!=null) {
			fileSystemPipe.stop();
		}

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

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
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

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
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

		InputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTarget", stream);

		fileSystemPipe.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTarget"));
		fileSystemPipe.setAction(FileSystemAction.UPLOAD);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
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

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result=prr.getResult().asString();

		// test
		assertEquals(contents.trim(), result.trim(), "result should be base64 of file content");
	}

	public void fileSystemPipeMoveActionTest(String folder1, String folder2, boolean folderExists, boolean setCreateFolderAttribute) throws Exception {
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

		Message message = new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result = prr.getResult().asString();

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse(_fileExists(folder1, filename), "file should not exist anymore in original folder ["+folder1+"]");
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
	public void fileSystemPipeMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
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

	public void fileSystemPipeCreateFile(String folder, boolean fileAlreadyExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "create" + FILE1;

		if(_folderExists(folder)) {
			_deleteFolder(folder);
		}
		waitForActionToFinish();

		if(fileAlreadyExists && !_fileExists(folder, filename)) {
			_createFile(folder, filename);
		}

		fileSystemPipe.setAction(FileSystemAction.CREATE);
		if (setCreateFolderAttribute) {
			fileSystemPipe.setCreateFolder(true);
		}
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message message = new Message(folder + "/" + filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result = prr.getResult().asString();

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(folder, filename), "file should exist in destination folder ["+folder+"]");
	}

	@Test
	public void fileSystemPipeCreateFileInFolder() throws Exception {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeCreateFile("folder1", false, false));
		assertEquals(e.getCause().getClass(), FileSystemException.class);
		assertThat(e.getMessage(), containsString("unable to process [CREATE] action for File [folder1/createfile1.txt]"));
	}

	@Test
	public void fileSystemPipeCreateFileAndCreateFolderAttributeEnabled() throws Exception {
		fileSystemPipeCreateFile("folder2", false, true);
	}

	@Test
	public void fileSystemPipeCreatingFileThatAlreadyExists() throws Exception {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeCreateFile("folder3", true, false));
		assertEquals(e.getCause().getClass(), FileSystemException.class);
		assertThat(e.getMessage(), containsString("unable to process [CREATE] action for File [folder3/createfile1.txt]"));
	}

	@Test
	public void fileSystemPipeCreatingFileThatAlreadyExistsAndCreateFolderAttributeEnabled() throws Exception {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeCreateFile("folder4", true, true));
		assertEquals(e.getCause().getClass(), FileSystemException.class);
		assertThat(e.getMessage(), containsString("unable to process [CREATE] action for File [folder4/createfile1.txt]"));
	}

	public void fileSystemPipeWriteFile(String folder, boolean fileAlreadyExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "write" + FILE1;

		if(_folderExists(folder)) {
			_deleteFolder(folder);
		}
		waitForActionToFinish();

		if(fileAlreadyExists && !_fileExists(folder, filename)) {
			_createFile(folder, filename);
		}

		fileSystemPipe.setAction(FileSystemAction.WRITE);
		if (setCreateFolderAttribute) {
			fileSystemPipe.setCreateFolder(true);
		}
		fileSystemPipe.addParameter(ParameterBuilder.create("filename", folder + "/" + filename));
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message message = new Message("dummyText");
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result = prr.getResult().asString();

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(folder, filename), "file should exist in destination folder ["+folder+"]");
		assertEquals("dummyText", StreamUtil.streamToString(_readFile(folder, filename)));
	}
	@Test
	public void fileSystemPipeWriteNewFileInFolder() throws Exception {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeWriteFile("folder1", false, false));
		assertEquals(e.getCause().getClass(), FileSystemException.class);
		assertThat(e.getMessage(), containsString("unable to process [WRITE] action for File [folder1/writefile1.txt]"));
	}

	@Test
	public void fileSystemPipeWritingFileAndCreateFolderAttributeEnabled() throws Exception {
		fileSystemPipeWriteFile("folder2", false, true);
	}

	@Test
	public void fileSystemPipeWritingFileThatAlreadyExists() throws Exception {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeWriteFile("folder3", true, false));
		assertEquals(e.getCause().getClass(), FileSystemException.class);
		assertThat(e.getMessage(), containsString("unable to process [WRITE] action for File [folder3/writefile1.txt]"));
	}

	@Test
	public void fileSystemPipeWritingFileThatAlreadyExistsAndCreateFolderAttributeEnabled() throws Exception {
		PipeRunException e = assertThrows(PipeRunException.class, () -> fileSystemPipeWriteFile("folder3", true, false));
		assertEquals(e.getCause().getClass(), FileSystemException.class);
		assertThat(e.getMessage(), containsString("unable to process [WRITE] action for File [folder3/writefile1.txt]"));
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

		Message message= new Message(folder);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result=prr.getResult().asString();
		waitForActionToFinish();

		// test

		boolean actual = _folderExists(folder);
		// test
		assertEquals(folder, result, "result of pipe should be name of created folder");
		assertTrue(actual, "Expected folder [" + folder + "] to be present");
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

		Message message= new Message(folder);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result=prr.getResult().asString();

		// test
		assertEquals(folder, result, "result of pipe should be name of removed folder");
		waitForActionToFinish();

		boolean actual = _fileExists(folder);
		// test
		assertFalse(actual, "Expected file [" + folder + "] " + "not to be present");
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

		Message message= new Message(folder);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
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

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
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

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
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

		Message message= new Message("");
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result=prr.getResult().asString();

		log.debug(result);

		// TODO test that the fileSystemPipe has returned the an XML with the details of the file
//		Iterator<F> it = result;
//		int count = 0;
//		while (it.hasNext()) {
//			it.next();
//			count++;
//		}

		assertFileCountEquals(result, numberOfFiles);
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
		fileSystemPipe.configure();

		PipeStartException e = assertThrows(PipeStartException.class, fileSystemPipe::start);
		assertThat(e.getMessage(), startsWith("Cannot open fileSystem"));
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

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result=prr.getResult().asString();
		waitForActionToFinish();

		assertFileCountEquals(result, 2);
	}
}
