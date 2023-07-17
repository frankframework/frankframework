package nl.nn.adapterframework.filesystem;

public class MockFileSystemTest extends FileSystemTest <MockFile,MockFileSystem<MockFile>>{

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	protected MockFileSystem<MockFile> createFileSystem() {
		return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
	}

}
