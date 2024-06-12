package org.frankframework.filesystem.mock;

import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.FileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;

public class MockFileSystemTest extends FileSystemTest<MockFile, MockFileSystem<MockFile>> {

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<>();
	}

	@Override
	protected MockFileSystem<MockFile> createFileSystem() {
		return ((MockFileSystemTestHelper<MockFile>) helper).getFileSystem();
	}

	@Override
	@Test
	public void basicFileSystemTestListDirsAndOrFolders() {
		// Folder structure not correctly implemented in MockFileSystem. Mock tests are deleted in the near future, because they're not needed anymore.
	}
}
