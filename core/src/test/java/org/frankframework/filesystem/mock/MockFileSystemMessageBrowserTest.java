package org.frankframework.filesystem.mock;

import org.frankframework.filesystem.FileSystemMessageBrowserTest;
import org.frankframework.filesystem.IFileSystemTestHelperFullControl;

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
