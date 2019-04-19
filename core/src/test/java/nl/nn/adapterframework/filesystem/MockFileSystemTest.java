package nl.nn.adapterframework.filesystem;

public class MockFileSystemTest extends FileSystemTest <MockFile,MockFileSystem>{

	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper();
	}

	@Override
	protected MockFileSystem getFileSystem() {
		return ((MockFileSystemTestHelper)helper).getFileSystem();
	}

}
