package nl.nn.adapterframework.filesystem;

public class MockFileSystemPipeTest extends FileSystemPipeTest<FileSystemPipe<MockFile, MockFileSystem<MockFile>>, MockFile, MockFileSystem<MockFile>> {

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	public FileSystemPipe<MockFile, MockFileSystem<MockFile>> createFileSystemPipe() {
		FileSystemPipe<MockFile,MockFileSystem<MockFile>> result = new FileSystemPipe<MockFile,MockFileSystem<MockFile>>() {};
		result.setFileSystem(((MockFileSystemTestHelper<MockFile>)helper).getFileSystem());
		return result;
	}
}
