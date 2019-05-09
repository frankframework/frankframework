package nl.nn.adapterframework.filesystem;

public class MockFileSystemListenerTest extends FileSystemListenerTest <MockFile,MockFileSystem>{

	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper();
	}

	@Override
	public FileSystemListener<MockFile, MockFileSystem> createFileSystemListener() {
		FileSystemListener<MockFile,MockFileSystem> result=new FileSystemListener<MockFile,MockFileSystem>();
		result.setFileSystem(((MockFileSystemTestHelper)helper).getFileSystem());
		return result;
	}

}
