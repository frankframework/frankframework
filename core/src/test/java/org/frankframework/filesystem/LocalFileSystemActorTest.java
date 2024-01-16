package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LocalFileSystemActorTest extends FileSystemActorTest<Path, LocalFileSystem>{

	@TempDir
	public Path folder;

	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(folder.toAbsolutePath().toString());
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}


	public void fileSystemActorMoveActionTestNoRoot(String destFolder, boolean createDestFolder, boolean setCreateFolderAttribute) throws Exception {

		LocalFileSystem localFileSystemNoRoot=new LocalFileSystem();
		String srcFolder=folder.toAbsolutePath().toString();

		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if (createDestFolder && destFolder!=null) {
			_createFolder(destFolder);
		}
		createFile(null, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.MOVE);
		ParameterList params = new ParameterList();
		params.add(new Parameter("destination", srcFolder+"/"+destFolder));
		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		params.configure();
		actor.configure(localFileSystemNoRoot,params,owner);
		actor.open();

		Message message = new Message(srcFolder+"/"+filename);
		ParameterValueList pvl = params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);

		// test
		// result should be name of the moved file
		assertNotNull(result, "name of moved file should not be null");

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(destFolder, filename), "file should exist in destination folder ["+destFolder+"]");
		assertFalse(_fileExists(null, filename), "file should not exist anymore in original folder ["+srcFolder+"]");
	}


	@Test
	public void fileSystemActorMoveActionTestRootToFolderNoRoot() throws Exception {
		fileSystemActorMoveActionTestNoRoot("folder",true,false);
	}
	@Test
	public void fileSystemActorMoveActionTestRootToFolderCreateFolderNoRoot() throws Exception {
		fileSystemActorMoveActionTestNoRoot("folder",false,true);
	}
	@Test
	public void fileSystemActorMoveActionTestRootToFolderFailIfolderDoesNotExistNoRoot() throws Exception {
		Exception e = assertThrows(Exception.class, () -> fileSystemActorMoveActionTestNoRoot("folder",false,false));
		assertThat(e.getMessage(), containsString("unable to process ["+FileSystemAction.MOVE+"] action for File ["+folder.toAbsolutePath()+"/sendermovefile1.txt]: destination folder ["+folder.toAbsolutePath()+"/folder] does not exist"));
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderExistsAndAllowToCreateNoRoot() throws Exception {
		fileSystemActorMoveActionTestNoRoot("folder",true,true);
	}

}
