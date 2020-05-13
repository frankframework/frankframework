package nl.nn.adapterframework.filesystem;

public class MockFileSystemUtilTest extends FileSystemUtilsTest <MockFile,MockFileSystem<MockFile>>{

	
	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	protected MockFileSystem<MockFile> createFileSystem() {
		return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
	}

}
