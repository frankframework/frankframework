package nl.nn.adapterframework.filesystem.mock;

import nl.nn.adapterframework.filesystem.FileSystemPipe;
import nl.nn.adapterframework.filesystem.FileSystemPipeTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;

public class MockFileSystemPipeTest extends FileSystemPipeTest<FileSystemPipe<MockFile, MockFileSystem<MockFile>>, MockFile, MockFileSystem<MockFile>> {

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	public FileSystemPipe<MockFile, MockFileSystem<MockFile>> createFileSystemPipe() {
		return new MockFileSystemPipe(((MockFileSystemTestHelper<MockFile>)helper).getFileSystem());
	}

	public static class MockFileSystemPipe extends FileSystemPipe<MockFile, MockFileSystem<MockFile>> {
		public MockFileSystemPipe(MockFileSystem<MockFile> mockFileSystem) {
			setFileSystem(mockFileSystem);
		}
	}

}
