package nl.nn.adapterframework.filesystem;


public class MockFileSystemActorTest extends FileSystemActorTest <MockFile,MockFileSystem<MockFile>>{

	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	protected MockFileSystem<MockFile> createFileSystem() {
		return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
	}

}
