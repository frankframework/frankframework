package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

public class LocalFileSystemActorTest extends FileSystemActorTest<File, LocalFileSystem>{

	public TemporaryFolder folder;


	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(folder.getRoot().getAbsolutePath());
		return result;
	}

	@Override
	public void setUp() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}
	

	public void fileSystemActorMoveActionTestNoRoot(String destFolder, boolean createDestFolder, boolean setCreateFolderAttribute) throws Exception {
		
		LocalFileSystem localFileSystemNoRoot=new LocalFileSystem();
		String srcFolder=folder.getRoot().getAbsolutePath();
		
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";
		
		if (createDestFolder && destFolder!=null) {
			_createFolder(destFolder);
		}
		createFile(null, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction("move");
		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(srcFolder+"/"+destFolder);
		params.add(p);
		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		params.configure();
		actor.configure(localFileSystemNoRoot,params,owner);
		actor.open();
		
		Message message = new Message(srcFolder+"/"+filename);
		ParameterValueList pvl = params.getValues(message, null);
		Object result = actor.doAction(message, pvl, null);
		
		// test
		// result should be name of the moved file
		assertNotNull("name of moved file should not be null", result);
		
		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file
		
		assertTrue("file should exist in destination folder ["+destFolder+"]", _fileExists(destFolder, filename)); 
		assertFalse("file should not exist anymore in original folder ["+srcFolder+"]", _fileExists(null, filename));
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
		thrown.expectMessage("unable to process [move] action for File ["+folder.getRoot().getAbsolutePath()+"/sendermovefile1.txt]: destination folder ["+folder.getRoot().getAbsolutePath()+File.separator+"folder] does not exist");
		fileSystemActorMoveActionTestNoRoot("folder",false,false);
	}
	@Test
	public void fileSystemActorMoveActionTestRootToFolderExistsAndAllowToCreateNoRoot() throws Exception {
		fileSystemActorMoveActionTestNoRoot("folder",true,true);
	}

	@Override // to adjust expected error message
	@Test
	public void fileSystemActorMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
		thrown.expectMessage("unable to process [move] action for File [sendermovefile1.txt]: destination folder ["+folder.getRoot().getAbsolutePath()+File.separator+"folder] does not exist");
		fileSystemActorMoveActionTest(null,"folder",false,false);
	}

}
