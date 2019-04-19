package nl.nn.adapterframework.filesystem;

public class MockFileSystemTest extends FileSystemTest <MockFile,MockFileSystem>{

	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper();
	}

	@Override
	protected MockFileSystem createFileSystem() {
		return ((MockFileSystemTestHelper)helper).getFileSystem();
	}

}
