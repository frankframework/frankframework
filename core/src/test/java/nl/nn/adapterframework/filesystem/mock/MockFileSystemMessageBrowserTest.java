package nl.nn.adapterframework.filesystem.mock;

import nl.nn.adapterframework.filesystem.FileSystemMessageBrowserTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelperFullControl;

public class MockFileSystemMessageBrowserTest extends FileSystemMessageBrowserTest <MockFile,MockFileSystem<MockFile>>{

	{
		messageIdProperty="id";
	}

	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	protected MockFileSystem<MockFile> createFileSystem() {
		return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
	}

}
