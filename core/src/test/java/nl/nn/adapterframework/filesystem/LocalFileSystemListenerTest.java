package nl.nn.adapterframework.filesystem;

import java.nio.file.Path;

import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.receivers.DirectoryListener;

public class LocalFileSystemListenerTest extends FileSystemListenerTest<Path, LocalFileSystem>{

	public TemporaryFolder folder;

	@Override
	public FileSystemListener<Path, LocalFileSystem> createFileSystemListener() {
		DirectoryListener result=new DirectoryListener();
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



}
