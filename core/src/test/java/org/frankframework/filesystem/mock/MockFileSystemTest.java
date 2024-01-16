package org.frankframework.filesystem.mock;

import org.frankframework.filesystem.FileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;

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
