package org.frankframework.filesystem.mock;

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

}
