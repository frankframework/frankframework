package nl.nn.adapterframework.filesystem;

import java.io.File;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.senders.LocalFileSystemSender;

public class LocalFileSystemSenderTest extends FileSystemSenderTest<LocalFileSystemSender, File, LocalFileSystem>{

	public TemporaryFolder folder;

	@Override
	public LocalFileSystemSender createFileSystemSender() {
		LocalFileSystemSender result=new LocalFileSystemSender();
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

	@Override // to adjust expected error message
	@Test
	public void fileSystemSenderMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
		thrown.expectMessage("unable to process [move] action for File [sendermovefile1.txt]: destination folder ["+folder.getRoot().getAbsolutePath()+File.separator+"folder] does not exist");
		fileSystemSenderMoveActionTest(null,"folder",false,false);
	}

}
