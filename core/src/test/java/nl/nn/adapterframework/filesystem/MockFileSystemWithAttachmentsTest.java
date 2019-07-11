package nl.nn.adapterframework.filesystem;

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
