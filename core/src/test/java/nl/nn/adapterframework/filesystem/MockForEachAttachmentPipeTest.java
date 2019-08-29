package nl.nn.adapterframework.filesystem;

public class MockForEachAttachmentPipeTest extends ForEachAttachmentPipeTest <ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>, MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>{

	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemWithAttachmentsTestHelper();
	}

	@Override
	public ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments> createForEachAttachmentPipe() {
		ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments> pipe= new ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>();
		pipe.setFileSystem(((MockFileSystemWithAttachmentsTestHelper)helper).getFileSystem());
		return pipe;
	}

}
