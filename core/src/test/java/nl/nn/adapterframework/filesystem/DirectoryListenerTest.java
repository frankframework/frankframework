package nl.nn.adapterframework.filesystem;

import java.io.File;

import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.receivers.DirectoryListener;

public class DirectoryListenerTest extends FileSystemListenerTest<File, LocalFileSystem> {

	public TemporaryFolder folder;
	
	@Override
	public IFileSystemListener<File> createFileSystemListener() {
		DirectoryListener result=new DirectoryListener();
		result.setInputDirectory(folder.getRoot().getAbsolutePath());
		fileAndFolderPrefix=folder.getRoot().getAbsolutePath()+"/";
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

}
