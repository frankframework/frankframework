package nl.nn.adapterframework.filesystem;

public class MockFileSystemSenderTest extends FileSystemSenderTest <MockFile,MockFileSystem>{

	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper();
	}

	@Override
	public FileSystemSender<MockFile, MockFileSystem> createFileSystemSender() {
		FileSystemSender<MockFile,MockFileSystem> result=new FileSystemSender<MockFile,MockFileSystem>();
		result.setFileSystem(((MockFileSystemTestHelper)helper).getFileSystem());
		return result;
	}


}
