package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.TimeProvider;

@TestMethodOrder(MethodName.class)
public abstract class FileSystemActorTest<F, FS extends IBasicFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FileSystemActor<F, FS> actor;
	protected FS fileSystem;
	protected Message result;
	protected ParameterList parameters;

	protected abstract FS createFileSystem();

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		fileSystem = createFileSystem();
		autowireBeanByNameInAdapter(fileSystem);
		fileSystem.configure();
		fileSystem.open();
		actor = new FileSystemActor<>();
		parameters = new ParameterList();
		result = null;
	}

	@Override
	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(result, fileSystem);

		super.tearDown();
	}

	@Test
	public void fileSystemActorTestConfigureBasic() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, parameters, adapter);
	}

	@Test
	public void fileSystemActorTestConfigureNoAction() throws Exception {
		ConfigurationException e = assertThrows(ConfigurationException.class, () -> actor.configure(fileSystem, parameters, adapter));
		assertThat(e.getMessage(), containsString("either attribute [action] or parameter [action] must be specified"));
		assertThat(e.getMessage(), containsString("TestAdapter of " + this.getClass().getSimpleName()));
	}

	@Test
	public void fileSystemActorEmptyParameterAction() throws Exception {
		String filename = "emptyParameterAction" + FILE1;
		String contents = "Tekst om te lezen";

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		parameters.add(new Parameter("action", ""));
		parameters.configure();

		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(id);
		ParameterValueList pvl = parameters.getValues(new Message(""), session);

		FileSystemException e = assertThrows(FileSystemException.class, () -> actor.doAction(message, pvl, session));
		assertThat(e.getMessage(), containsString("unable to resolve the value of parameter"));
	}

	@Test
	public void fileSystemActorEmptyParameterActionWillBeOverridenByConfiguredAction() throws Exception {
		String filename = "overwriteEmptyParameter" + FILE1;
		String contents = "Tekst om te lezen";

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		parameters.add(ParameterBuilder.create().withName("action"));
		parameters.configure();
		actor.setAction(FileSystemAction.READ);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(id);
		ParameterValueList pvl = parameters.getValues(null, session);

		result = actor.doAction(message, pvl, session);

		assertEquals(contents, result.asString());
	}

	@Test
	public void fileSystemActorTestBasicOpen() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();
	}

	@Test
	public void fileSystemActorTestConfigureInputDirectoryForListActionDoesNotExist() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.setInputFolder("xxx");
		actor.configure(fileSystem, parameters, adapter);

		FolderNotFoundException e = assertThrows(FolderNotFoundException.class, actor::open);
		assertThat(e.getMessage(), containsString("inputFolder [xxx], canonical name ["));
		assertThat(e.getMessage(), containsString("does not exist"));
	}

	@Test
	public void fileSystemActorTestConfigureInputDirectoryForListActionDoesNotExistButAllowCreate() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.setCreateFolder(true);
		actor.setInputFolder("xxx");
		actor.configure(fileSystem, parameters, adapter);
		actor.open();
	}


	@Test
	public void fileSystemActorListActionTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder("folder");
		actor.setAction(FileSystemAction.LIST);
		actor.setInputFolder("folder");
		actor.configure(fileSystem, parameters, adapter);
		actor.open();
	}

	@Test()
	public void fileSystemActorListActionTestForFolderExistenceWithRoot() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();
	}

	@Test()
	public void fileSystemActorListActionWhenDuplicateConfigurationAttributeHasPreference() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.setInputFolder("folder1");
		parameters.add(new Parameter("inputFolder", "folder2"));
		actor.configure(fileSystem, parameters, adapter);

		FolderNotFoundException e = assertThrows(FolderNotFoundException.class, actor::open);
		assertThat(e.getMessage(), containsString("inputFolder [folder1], canonical name ["));
		assertThat(e.getMessage(), containsString("does not exist"));
	}

	public void fileSystemActorListActionTest(String inputFolder, int numberOfFiles, int expectedNumberOfFiles) throws Exception {
		for (int i = 0; i < numberOfFiles; i++) {
			String filename = "tobelisted" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(inputFolder, filename, "is not empty");
			}
		}

		actor.setAction(FileSystemAction.LIST);
		if (inputFolder != null) {
			actor.setInputFolder(inputFolder);
		}
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		log.debug(result);

		// TODO test that the fileSystemSender has returned the an XML with the details of the file
//		Iterator<F> it = result;
//		int count = 0;
//		while (it.hasNext()) {
//			it.next();
//			count++;
//		}

		assertFileCountEquals(result, expectedNumberOfFiles);
		message.close();
	}

	@Test
	public void fileSystemActorListActionTestInRootNoFiles() throws Exception {
		fileSystemActorListActionTest(null, 0, 0);
	}

	@Test
	public void fileSystemActorListActionTestInRoot() throws Exception {
		fileSystemActorListActionTest(null, 2, 2);
	}

	@Test
	public void fileSystemActorListActionTestInFolderNoFiles() throws Exception {
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 0, 0);
	}

	@Test
	public void fileSystemActorListActionTestInFolder() throws Exception {
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 2, 2);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithWildCard() throws Exception {
		actor.setWildcard("*d0*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 5, 1);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithExcludeWildCard() throws Exception {
		actor.setExcludeWildcard("*d0*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 5, 4);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithBothWildCardAndExcludeWildCard() throws Exception {
		actor.setWildcard("*.txt");
		actor.setExcludeWildcard("*ted1*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 5, 4);
	}

	@Test
	public void migrated_localFileSystemTestListWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename + ".bak";
		String filename2 = filename + ".xml";
		String contents = "regeltje tekst";

		actor.setWildcard("*.xml");
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();
		assertTrue(stringResult.contains(filename2));
		assertFalse(stringResult.contains(filename1));
	}

	@Test
	public void migrated_localFileSystemTestListExcludeWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename + ".bak";
		String filename2 = filename + ".xml";
		String contents = "regeltje tekst";

		actor.setExcludeWildcard("*.bak");
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();

		assertTrue(stringResult.contains(filename2));
		assertFalse(stringResult.contains(filename1));
	}


	@Test
	public void migrated_localFileSystemTestListIncludeExcludeWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename + ".oud.xml";
		String filename2 = filename + ".xml";
		String contents = "regeltje tekst";

		actor.setWildcard("*.xml");
		actor.setExcludeWildcard("*.oud.xml");
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();

		assertTrue(stringResult.contains(filename2));
		assertFalse(stringResult.contains(filename1));
	}


	@Test
	public void fileSystemActorListActionTestWithInputFolderAsParameter() throws Exception {
		String filename = FILE1;
		String filename2 = FILE2;
		String inputFolder = "directory";

		if (_fileExists(inputFolder, filename)) {
			_deleteFile(inputFolder, filename);
		}

		if (_fileExists(inputFolder, filename2)) {
			_deleteFile(inputFolder, filename2);
		}

		actor.setAction(FileSystemAction.LIST);
		parameters.add(new Parameter("inputFolder", inputFolder));
		parameters.configure();
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		_createFolder(inputFolder);
		String id1 = createFile(inputFolder, filename, "some content");
		waitForActionToFinish();
		assertTrue(_fileExists(inputFolder, id1), "File [" + filename + "] expected to be present");

		String id2 = createFile(inputFolder, filename2, "some content of second file");
		waitForActionToFinish();
		assertTrue(_fileExists(inputFolder, id2), "File [" + filename2 + "] expected to be present");

		Message message = new Message(id1);
		ParameterValueList pvl = parameters.getValues(message, session);

		result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		assertFileCountEquals(result, 2);
	}

	public void fileSystemActorInfoActionTest(boolean fileViaAttribute) throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.INFO);
		if (fileViaAttribute) {
			actor.setFilename(id);
		}
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(fileViaAttribute ? null : id);
		ParameterValueList pvl = null;

		// Act
		result = actor.doAction(message, pvl, session);
		String resultStr = result.asString();

		assertThat(resultStr, containsString("<file name=\"%s\"".formatted(id)));
		assertThat(resultStr, containsString("size=\"17\""));
		assertThat(resultStr, containsString("canonicalName="));
		assertThat(resultStr, containsString("modificationDate="));
		assertThat(resultStr, containsString("modificationTime="));
	}

	@Test
	public void fileSystemActorInfoActionTest() throws Exception {
		fileSystemActorInfoActionTest(false);
	}

	@Test
	public void fileSystemActorInfoActionTestFilenameViaAttribute() throws Exception {
		fileSystemActorInfoActionTest(true);
	}

	@Test
	public void fileSystemActorReadActionFromParameterTest() throws Exception {
		String filename = "parameterAction" + FILE1;
		String contents = "Tekst om te lezen";

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		parameters.add(new Parameter("action", "read"));
		parameters.configure();

		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(id);
		ParameterValueList pvl = parameters.getValues(message, session);

		result = actor.doAction(message, pvl, session);

		assertEquals(contents, result.asString());
		assertTrue(_fileExists(id));
	}

	public void fileSystemActorReadActionTest(FileSystemAction action, boolean fileViaAttribute, boolean fileShouldStillExistAfterwards) throws Exception {
		fileSystemActorReadActionTest(action, fileViaAttribute, fileShouldStillExistAfterwards, "Tekst om te lezen");
	}

	public void fileSystemActorReadActionTest(FileSystemAction action, boolean fileViaAttribute, boolean fileShouldStillExistAfterwards, String contents) throws Exception {
		String filename = "sender" + FILE1;

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(action);
		if (fileViaAttribute) {
			actor.setFilename(id);
		}
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(fileViaAttribute ? null : id);
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		if(contents == null) {
			assertTrue(Message.isEmpty(result));
		} else {
			assertEquals(contents, result.asString());
		}
		assertEquals(fileShouldStillExistAfterwards, _fileExists(id));
	}

	@Test
	public void testReadEmptyFile() throws Exception {
		// Test to prove that we work around the bug in AWS v2 api that handles empty files incorrectly
		// in org.frankframework.filesystem.AmazonS3FileSystem.readFile
		fileSystemActorReadActionTest(FileSystemAction.READ, false, true, null);
	}

	@Test
	public void fileSystemActorReadActionTest() throws Exception {
		fileSystemActorReadActionTest(FileSystemAction.READ, false, true);
	}

	@Test
	public void fileSystemActorReadActionTestFilenameViaAttribute() throws Exception {
		fileSystemActorReadActionTest(FileSystemAction.READ, true, true);
	}

	@Test
	public void fileSystemActorReadActionTestCompatiblity() throws Exception {
		fileSystemActorReadActionTest(FileSystemAction.DOWNLOAD, false, true);
	}

	@Test
	public void fileSystemActorReadDeleteActionTest() throws Exception {
		fileSystemActorReadActionTest(FileSystemAction.READDELETE, false, false);
	}

	@Test
	public void fileSystemActorReadDeleteActionWithDeleteEmptyFolderTest() throws Exception {
		String filename = "presender" + FILE1;
		String contents = "Tekst om te lezen";
		String folder = "inner";

		_createFolder(folder);
		String id = createFile(folder, filename, contents);
		waitForActionToFinish();

		actor.setDeleteEmptyFolder(true);
		actor.setAction(FileSystemAction.READDELETE);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(folder + "/" + id);
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		assertEquals(contents, result.asString());
		assertFalse(_fileExists(id), "Expected file [" + filename + "] not to be present");
		assertFalse(_fileExists(id), "Expected file [" + filename + "] not to be present");

		assertFalse(_folderExists(folder), "Expected parent folder not to be present");
	}

	@Test
	public void fileSystemActorReadWithCharsetUseDefault() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "€ $ & ^ % @ < é ë ó ú à è";

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.READ);
		actor.setFilename(id);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(id);
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);
		assertEquals(contents, result.asString());
	}

	@Test
	public void fileSystemActorReadWithCharsetUseIncompatible() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "€ è";
		String expected = "â¬ Ã¨";

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.READ);
		actor.setFilename(id);
		actor.setCharset("ISO-8859-1");
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(id);
		ParameterValueList pvl = null;

		Message result = actor.doAction(message, pvl, session);
		assertEquals(expected, result.asString());
	}

	@Test
	public void fileSystemActorMoveActionTestWithWildCard() throws Exception {
		String srcFolderName = "src" + TimeProvider.nowAsMillis();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + TimeProvider.nowAsMillis();
		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();

		actor.setAction(FileSystemAction.MOVE);
		actor.setWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);
		parameters.add(new Parameter("destination", destFolderName));
		parameters.configure();
		actor.setCreateFolder(true);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message m = new Message("");
		ParameterValueList pvl = parameters.getValues(m, session);
		result = actor.doAction(m, pvl, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertFalse(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorMoveActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + TimeProvider.nowAsMillis();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + TimeProvider.nowAsMillis();
		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();

		actor.setAction(FileSystemAction.MOVE);
		actor.setExcludeWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);
		parameters.add(new Parameter("destination", destFolderName));
		parameters.configure();
		actor.setCreateFolder(true);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message m = new Message("");
		ParameterValueList pvl = parameters.getValues(m, session);
		result = actor.doAction(m, pvl, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertFalse(_fileExists(srcFolderName, filename));
		}
	}

	@Test()
	public void fileSystemActorMoveActionTestForDestinationParameter() throws Exception {
		actor.setAction(FileSystemAction.MOVE);

		ConfigurationException e = assertThrows(ConfigurationException.class, () -> actor.configure(fileSystem, parameters, adapter));
		assertThat(e.getMessage(), endsWith("the [MOVE] action requires the parameter [destination] or the attribute [destination] to be present"));
	}

	public void fileSystemActorMoveActionTest(String srcFolder, String destFolder, boolean createDestFolder, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if (srcFolder != null) {
			_createFolder(srcFolder);
		}
		if (createDestFolder && destFolder != null) {
			_createFolder(destFolder);
		}
		String id = createFile(srcFolder, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.MOVE);
		parameters.add(new Parameter("destination", destFolder));
		parameters.configure();

		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(id);
		ParameterValueList pvl = parameters.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse(_fileExists(srcFolder, id), "file should not exist anymore in original folder [" + srcFolder + "]");
	}


	@Test
	public void fileSystemActorMoveActionTestRootToFolder() throws Exception {
		fileSystemActorMoveActionTest(null, "folder", true, false);
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderCreateFolder() throws Exception {
		fileSystemActorMoveActionTest(null, "folder", false, true);
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
		FileSystemException e = assertThrows(FileSystemException.class, () -> fileSystemActorMoveActionTest(null, "folder", false, false));
		assertThat(e.getMessage(), containsString("unable to process [MOVE] action for File ["));
		assertThat(e.getMessage(), containsString("]: destination folder [folder] does not exist"));
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderExistsAndAllowToCreate() throws Exception {
		fileSystemActorMoveActionTest(null, "folder", true, true);
	}

	@Test
	public void fileSystemActorMoveActionWithDeleteEmptyFolderTest() throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";
		String destinationFolder = "deleteEmptyFolder";

		_createFolder("innerFolder");
		_createFolder(destinationFolder);

		String id = createFile("innerFolder", filename, contents);

		waitForActionToFinish();

		actor.setDeleteEmptyFolder(true);
		actor.setAction(FileSystemAction.MOVE);
		parameters.add(new Parameter("destination", destinationFolder));
		parameters.configure();

		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message("innerFolder/" + id);
		ParameterValueList pvl = parameters.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		assertNotNull(result);
		assertFalse(_fileExists(id), "file should not exist anymore in original folder");
		assertTrue(_fileExists(destinationFolder, result.asString()), "file should exist in target folder");

		assertFalse(_folderExists("innerFolder"), "Expected parent folder not to be present");
	}

	@Test
	public void fileSystemActorCopyActionTestWithWildCard() throws Exception {
		String srcFolderName = "src" + TimeProvider.nowAsMillis();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + TimeProvider.nowAsMillis();
		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();

		actor.setAction(FileSystemAction.COPY);
		actor.setWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);

		parameters.add(new Parameter("destination", destFolderName));
		parameters.configure();

		actor.setCreateFolder(true);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message m = new Message("");
		ParameterValueList pvl = parameters.getValues(m, session);
		result = actor.doAction(m, pvl, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorCopyActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + TimeProvider.nowAsMillis();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + TimeProvider.nowAsMillis();
		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();

		actor.setAction(FileSystemAction.COPY);
		actor.setExcludeWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);

		parameters.add(new Parameter("destination", destFolderName));
		parameters.configure();

		actor.setCreateFolder(true);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message m = new Message("");
		ParameterValueList pvl = parameters.getValues(m, session);
		result = actor.doAction(m, pvl, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	public void fileSystemActorCopyActionTest(String folder1, String folder2, boolean folderExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if (folder1 != null) {
			_createFolder(folder1);
		}
		if (folderExists && folder2 != null) {
			_createFolder(folder2);
		}
		String id = createFile(folder1, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.COPY);
		actor.setDestination(folder2);
		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		parameters.configure();
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(id);
		ParameterValueList pvl = parameters.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		// test
		// result should be name of the moved file
		// assertNotNull(result); from 7.8, result is allowed to be null

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertTrue(_fileExists(folder1, id), "file should still exist anymore in original folder [" + folder1 + "]");
	}

	@Test
	public void fileSystemActorCopyActionTestRootToFolder() throws Exception {
		fileSystemActorCopyActionTest(null, "folder", true, false);
	}


	@Test
	public void fileSystemActorMkdirActionTest() throws Exception {
		String folder = "mkdir" + DIR1;

		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}

		actor.setAction(FileSystemAction.MKDIR);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(folder);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		// test

		boolean actual = _folderExists(folder);
		// test
		assertEquals(folder, result.asString(), "result of actor should be name of created folder");
		assertTrue(actual, "Expected folder [" + folder + "] to be present");
	}

	@Test
	public void fileSystemActorRmdirActionTest() throws Exception {
		String folder = DIR1;

		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}

		actor.setAction(FileSystemAction.RMDIR);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(folder);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		// test
		assertEquals(folder, result.asString(), "result of actor should be name of removed folder");
		waitForActionToFinish();

		boolean actual = _folderExists(folder);
		// test
		assertFalse(actual, "Expected folder [" + folder + "] " + "not to be present");
	}

	@Test
	public void fileSystemActorRmNonEmptyDirActionTest() throws Exception {
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

		actor.setAction(FileSystemAction.RMDIR);
		actor.setRemoveNonEmptyFolder(true);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(folder);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		// test
		assertEquals(folder, result.asString(), "result of actor should be name of removed folder");
		waitForActionToFinish();

		boolean actual = _folderExists(folder);
		// test
		assertFalse(actual, "Expected folder [" + folder + "] " + "not to be present");
	}

	@Test
	public void fileSystemActorAttemptToRmNonEmptyDir() throws Exception {
		String folder = DIR1;
		String innerFolder = DIR1 + "/innerFolder";
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}
		if (!_folderExists(innerFolder)) {
			_createFolder(innerFolder);
		}
		List<String> fileIds = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			String filename = "file" + i + FILE1;
			fileIds.add(createFile(folder, filename, "is not empty"));
			fileIds.add(createFile(innerFolder, filename, "is not empty"));
		}

		assertTrue(_fileExists(innerFolder, fileIds.get(0)), "Expected file [" + innerFolder + "/file0file1.txt] to be present");

		actor.setAction(FileSystemAction.RMDIR);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(folder);
		ParameterValueList pvl = null;
		FileSystemException e = assertThrows(FileSystemException.class, () -> actor.doAction(message, pvl, session));
		assertThat(e.getMessage(), containsString("unable to process [RMDIR] action for File [testDirectory]: Cannot remove folder"));

		// Clean up
		message.close();
	}

	@Test
	public void fileSystemActorDeleteActionTest() throws Exception {
		String filename = "tobedeleted" + FILE1;

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}
		String id = createFile(null, filename, "is not empty");

		actor.setAction(FileSystemAction.DELETE);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(id);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		waitForActionToFinish();

		boolean actual = _fileExists(id);
		// test
		assertEquals(id, result.asString(), "result of sender should be name of deleted file");
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");

		message.close();
	}

	@Test
	public void fileSystemActorDeleteActionWithDeleteEmptyFolderTest() throws Exception {
		String filename = "filetobedeleted" + FILE1;
		String folder = "inner";

		_deleteFolder(folder);
		_createFolder(folder);
		String id = createFile(folder, filename, "is not empty");

		actor.setDeleteEmptyFolder(true);
		actor.setAction(FileSystemAction.DELETE);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(folder + "/" + id);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		waitForActionToFinish();

		boolean actual = _fileExists(folder, id);
		// test
		assertEquals(id, result.asString(), "result of sender should be name of deleted file");
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");
		assertFalse(_folderExists(folder), "Expected parent folder not to be present");
		message.close();
	}

	@Test
	//Should not be able to clean up directory after removing 'filename' because there are still 2 empty folders on the same root. Tests if list detects DIRECTORIES
	public void fileSystemActorDeleteActionWithDeleteEmptyFolderRootContainsEmptyFoldersTest() throws Exception {
		String filename = "filetobedeleted" + FILE1;
		final String folder = "inner";

		_createFolder(folder);
		_createFolder(folder + "/innerFolder1");
		_createFolder(folder + "/innerFolder2");
		createFile(folder, filename, "is not empty");

		actor.setDeleteEmptyFolder(true);
		actor.setAction(FileSystemAction.DELETE);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message message = new Message(folder + "/" + filename);
		ParameterValueList pvl = null;
		assertThrows(FileSystemException.class, () -> actor.doAction(message, pvl, session));

		waitForActionToFinish();

		// test
		assertFalse(_fileExists(filename), "Expected file [" + filename + "] " + "not to be present in the root");
		assertTrue(_folderExists(folder), "Expected parent folder to be present");
		assertTrue(_folderExists(folder + "/innerFolder1"), "Expected file in parent folder to be present");
		message.close();
	}

	@Test
	public void fileSystemActorDeleteActionTestWithWildCard() throws Exception {
		String srcFolderName = "src" + TimeProvider.nowAsMillis();
		if(!_folderExists(srcFolderName)) {
			_createFolder(srcFolderName);
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tobedeleted" + i + FILE1;

			if (!_fileExists(srcFolderName, filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}

			filename = "tostay" + i + FILE1;

			if (!_fileExists(srcFolderName, filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		waitForActionToFinish();

		actor.setAction(FileSystemAction.DELETE);
		actor.setWildcard("tobedeleted*");
		actor.setInputFolder(srcFolderName);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message m = new Message("");
		result = actor.doAction(m, null, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;
			assertFalse(_fileExists(srcFolderName, filename));
			filename = "tostay" + i + FILE1;
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorDeleteActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + TimeProvider.nowAsMillis();
		_createFolder(srcFolderName);

		for (int i = 0; i < 3; i++) {
			String filename = "tobedeleted" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}

			filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		waitForActionToFinish();

		actor.setAction(FileSystemAction.DELETE);
		actor.setExcludeWildcard("tostay*");
		actor.setInputFolder(srcFolderName);
		actor.configure(fileSystem, parameters, adapter);
		actor.open();

		Message m = new Message("");
		result = actor.doAction(m, null, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;
			assertFalse(_fileExists(srcFolderName, filename));
			filename = "tostay" + i + FILE1;
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}
}
