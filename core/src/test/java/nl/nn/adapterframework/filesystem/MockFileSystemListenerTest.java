package nl.nn.adapterframework.filesystem;

public class MockFileSystemListenerTest extends FileSystemListenerExtraTest <MockFile,MockFileSystem<MockFile>>{


	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	public FileSystemListener<MockFile, MockFileSystem<MockFile>> createFileSystemListener() {
		FileSystemListener<MockFile,MockFileSystem<MockFile>> result=new FileSystemListener<MockFile,MockFileSystem<MockFile>>(){

			@Override
			protected MockFileSystem<MockFile> createFileSystem() {
				return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
			}

		};
		return result;
	}

}
