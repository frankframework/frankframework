package nl.nn.adapterframework.filesystem.mock;

import nl.nn.adapterframework.filesystem.FileSystemWithAttachmentsTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;

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
