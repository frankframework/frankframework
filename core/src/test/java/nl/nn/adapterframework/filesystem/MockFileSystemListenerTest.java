package nl.nn.adapterframework.filesystem;

public class MockFileSystemListenerTest extends FileSystemListenerTest <MockFile,MockFileSystem>{

	
	@Override
	protected MockFileSystem getFileSystem() {
		return new MockFileSystem();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper(fileSystem);
	}

}
