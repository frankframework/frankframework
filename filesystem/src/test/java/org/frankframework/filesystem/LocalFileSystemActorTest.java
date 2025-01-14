package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

public class LocalFileSystemActorTest extends FileSystemActorRolloverTest<Path, LocalFileSystem> {

	@TempDir
	public Path folder;

	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result = new LocalFileSystem();
		result.setRoot(folder.toAbsolutePath().toString());
		return result;
	}

	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}

	public void fileSystemActorMoveActionTestNoRoot(String destFolder, boolean createDestFolder, boolean setCreateFolderAttribute) throws Exception {
		LocalFileSystem localFileSystemNoRoot = new LocalFileSystem();
		String srcFolder = folder.toAbsolutePath().toString();

		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if (createDestFolder && destFolder != null) {
			_createFolder(destFolder);
		}
		createFile(null, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.MOVE);
		ParameterList params = new ParameterList();
		params.add(new Parameter("destination", srcFolder + "/" + destFolder));
		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		params.configure();
		actor.configure(localFileSystemNoRoot, params, adapter);
		actor.open();

		Message message = new Message(srcFolder + "/" + filename);
		ParameterValueList pvl = params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);

		// test
		// result should be name of the moved file
		assertNotNull(result, "name of moved file should not be null");

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(destFolder, filename), "file should exist in destination folder [" + destFolder + "]");
		assertFalse(_fileExists(null, filename), "file should not exist anymore in original folder [" + srcFolder + "]");
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderNoRoot() throws Exception {
		fileSystemActorMoveActionTestNoRoot("folder", true, false);
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderCreateFolderNoRoot() throws Exception {
		fileSystemActorMoveActionTestNoRoot("folder", false, true);
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderFailIfolderDoesNotExistNoRoot() throws Exception {
		Exception e = assertThrows(Exception.class, () -> fileSystemActorMoveActionTestNoRoot("folder", false, false));
		assertThat(e.getMessage(), containsString("unable to process [" + FileSystemAction.MOVE + "] action for File [" + folder.toAbsolutePath() + "/sendermovefile1.txt]: destination folder [" + folder.toAbsolutePath() + "/folder] does not exist"));
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderExistsAndAllowToCreateNoRoot() throws Exception {
		fileSystemActorMoveActionTestNoRoot("folder", true, true);
	}

	@Test
	@DisplayName("The folder is created with createRootFolder = true")
	public void testCreateFolderWithCreateRootFolder() throws Exception {
		String tmpDir = fileSystem.getRoot() + "/testCreateFolder";
		fileSystem.setRoot(tmpDir);
		fileSystem.setCreateRootFolder(true);
		fileSystem.open();

		String fileName = "b52cc8d5-ee39-4a8f-84b8-f91b72b1c8b7";

		ParameterList params = new ParameterList();
		params.add(new Parameter("filename", fileName));
		params.configure();

		actor.setAction(FileSystemAction.WRITE);
		actor.setCreateFolder(true);
		actor.setOverwrite(true);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(fileSystem.getRoot() + "/" + fileName);
		ParameterValueList pvl = params.getValues(message, session);

		Object result = actor.doAction(message, pvl, session);

		assertNotNull(result, "Could not create new file in " + fileSystem.getRoot());
	}

	@Test
	@DisplayName("The folder is created correctly based on absolute path in the given filename")
	public void createTestFolderForAbsolutePath() throws Exception {
		String tmpDir = "testCreateFolderByPath";
		String fileName = tmpDir + "/b52cc8d5-ee39-4a8f-84b8-f91b72b1c8b8";

		ParameterList params = new ParameterList();
		params.add(new Parameter("filename", fileName));
		params.configure();

		actor.setAction(FileSystemAction.WRITE);
		actor.setCreateFolder(true);
		actor.setOverwrite(true);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(fileName);
		ParameterValueList pvl = params.getValues(message, session);

		Object result = actor.doAction(message, pvl, session);

		assertNotNull(result, "Could not create new file in " + tmpDir);
	}

	@Test
	@DisplayName("The folder is not created with createRootFolder = false")
	public void testCreateFolderWithoutCreateRootFolder() throws Exception {
		String tmpDir = fileSystem.getRoot() + "/testCreateRootFolder";
		fileSystem.setRoot(tmpDir);
		fileSystem.open();

		String fileName = "b52cc8d5-ee39-4a8f-84b8-f91b72b1c8b7";

		ParameterList params = new ParameterList();
		params.add(new Parameter("filename", fileName));
		params.configure();

		actor.setAction(FileSystemAction.WRITE);
		actor.setCreateFolder(true);
		actor.setOverwrite(true);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(fileSystem.getRoot() + "/" + fileName);
		ParameterValueList pvl = params.getValues(message, session);

		assertThrows(FileSystemException.class, () -> actor.doAction(message, pvl, session));
	}
}
