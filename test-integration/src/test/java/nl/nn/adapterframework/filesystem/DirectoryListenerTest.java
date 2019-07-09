package nl.nn.adapterframework.filesystem;

import org.junit.rules.TemporaryFolder;

public class DirectoryListenerTest extends FileSystemListenerTest {

	public TemporaryFolder folder;

	@Override
	public IFileSystemListener createFileSystemListener() {
		DirectoryListenerWrapper result=new DirectoryListenerWrapper();
		result.setInputDirectory(folder.getRoot().getAbsolutePath());
		result.setWildcard("*");
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
