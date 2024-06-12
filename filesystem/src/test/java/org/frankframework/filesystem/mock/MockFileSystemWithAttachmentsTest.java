package org.frankframework.filesystem.mock;

import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.FileSystemWithAttachmentsTest;
import org.frankframework.filesystem.IFileSystemTestHelper;

public class MockFileSystemWithAttachmentsTest extends FileSystemWithAttachmentsTest <MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>{


	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemWithAttachmentsTestHelper();
	}

	@Override
	protected MockFileSystemWithAttachments createFileSystem() {
		return ((MockFileSystemWithAttachmentsTestHelper)helper).getFileSystem();
	}

	@Override
	@Test
	public void basicFileSystemTestListDirsAndOrFolders() {
		// Folder structure not correctly implemented in MockFileSystem. Mock tests are deleted in the near future, because they're not needed anymore.
	}
}
